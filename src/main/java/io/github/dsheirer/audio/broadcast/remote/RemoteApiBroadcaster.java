package io.github.dsheirer.audio.broadcast.remote;

import com.google.gson.Gson;
import io.github.dsheirer.audio.broadcast.AbstractAudioBroadcaster;
import io.github.dsheirer.audio.broadcast.AudioRecording;
import io.github.dsheirer.audio.broadcast.BroadcastEvent;
import io.github.dsheirer.audio.broadcast.BroadcastState;
import io.github.dsheirer.util.ThreadPool;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reliable completed-call uploader for user-configured HTTP API destinations. */
public class RemoteApiBroadcaster extends AbstractAudioBroadcaster<RemoteApiConfiguration>
{
    private static final Logger mLog = LoggerFactory.getLogger(RemoteApiBroadcaster.class);
    private static final Gson GSON = new Gson();
    private static final Map<Integer,PendingQueue> PENDING_QUEUES = new ConcurrentHashMap<>();
    private final PendingQueue mPendingQueue;
    private final Queue<QueuedCall> mQueue;
    private final Duration mRequestTimeout;
    private final Object mConnectionLock = new Object();
    private volatile HttpClient mHttpClient;
    private final Semaphore mUploadSlots;
    private final AtomicBoolean mRunning = new AtomicBoolean();
    private final AtomicBoolean mPreserveQueueOnStop = new AtomicBoolean();
    private final AtomicBoolean mHeartbeatInFlight = new AtomicBoolean();
    private final AtomicInteger mConsecutiveConnectionFailures = new AtomicInteger();
    private final AtomicInteger mReconnectCount = new AtomicInteger();
    private final AtomicLong mHttpClientGeneration = new AtomicLong();
    private final Set<ActiveUpload> mActiveUploads = ConcurrentHashMap.newKeySet();
    private ScheduledFuture<?> mProcessor;
    private ScheduledFuture<?> mHeartbeatProcessor;
    private volatile CompletableFuture<HttpResponse<String>> mHeartbeatRequest;
    private final SpeechProcessor mSpeechProcessor;
    private volatile long mStarted;
    private volatile long mNextHeartbeatAttempt;
    private volatile long mLastHeartbeatAttempt;
    private volatile long mLastHeartbeatSuccess;
    private volatile String mLastHeartbeatError = "";
    private volatile long mLastCallAttempt;
    private volatile long mLastCallSuccess;
    private volatile String mLastCallError = "";
    private volatile long mLastContactAttempt;
    private volatile long mLastContactSuccess;
    private volatile long mLastReconnect;

    public RemoteApiBroadcaster(RemoteApiConfiguration configuration)
    {
        super(configuration);
        mPendingQueue = PENDING_QUEUES.computeIfAbsent(configuration.getId(), ignored -> new PendingQueue());
        mQueue = mPendingQueue.queue();
        mRequestTimeout = Duration.ofSeconds(configuration.getRequestTimeoutSeconds());
        mHttpClient = createHttpClient();
        mUploadSlots = new Semaphore(configuration.getMaximumConcurrentUploads());
        if(configuration.isOpenAiEnabled())
        {
            mSpeechProcessor = new OpenAiWhisperProcessor(configuration.getOpenAiKeyEnvironmentVariable(),
                configuration.isTranslateToEnglish(), mRequestTimeout);
        }
        else if(configuration.getLocalWhisperExecutable() != null &&
            !configuration.getLocalWhisperExecutable().isBlank() && configuration.getLocalWhisperModel() != null &&
            !configuration.getLocalWhisperModel().isBlank())
        {
            mSpeechProcessor = new LocalWhisperProcessor(configuration.getLocalWhisperExecutable(),
                configuration.getLocalWhisperModel(), configuration.isTranslateToEnglish(), mRequestTimeout);
        }
        else
        {
            mSpeechProcessor = path -> CompletableFuture.completedFuture(CallTranscription.none());
        }
    }

    @Override
    public void start()
    {
        if(mRunning.compareAndSet(false, true))
        {
            mPreserveQueueOnStop.set(false);
            mStarted = System.currentTimeMillis();
            mNextHeartbeatAttempt = mStarted;
            setBroadcastState(getBroadcastConfiguration().isHeartbeatEnabled() ? BroadcastState.CONNECTING :
                BroadcastState.CONNECTED);
            mProcessor = ThreadPool.SCHEDULED.scheduleWithFixedDelay(this::processQueue, 0, 250,
                TimeUnit.MILLISECONDS);
            if(getBroadcastConfiguration().isHeartbeatEnabled())
            {
                //Run a short watchdog interval so that failed or stale requests retry sooner than the normal heartbeat
                //interval.  The due timestamp still controls the configured cadence when the connection is healthy.
                mHeartbeatProcessor = ThreadPool.SCHEDULED.scheduleWithFixedDelay(this::monitorHeartbeat, 0, 1,
                    TimeUnit.SECONDS);
            }
        }
    }

    @Override
    public void stop()
    {
        stop(false);
    }

    /** Stops this instance while retaining queued and in-flight calls for its replacement instance. */
    public void stopPreservingQueue()
    {
        stop(true);
    }

    private void stop(boolean preserveQueue)
    {
        mPreserveQueueOnStop.set(preserveQueue);
        mRunning.set(false);
        if(mProcessor != null)
        {
            mProcessor.cancel(true);
            mProcessor = null;
        }
        if(mHeartbeatProcessor != null)
        {
            mHeartbeatProcessor.cancel(true);
            mHeartbeatProcessor = null;
        }
        CompletableFuture<HttpResponse<String>> heartbeatRequest = mHeartbeatRequest;
        if(heartbeatRequest != null)
        {
            heartbeatRequest.cancel(true);
            mHeartbeatRequest = null;
        }
        mHeartbeatInFlight.set(false);
        cancelActiveUploads(preserveQueue);
        if(!preserveQueue)
        {
            discardQueue();
        }
        setBroadcastState(BroadcastState.DISCONNECTED);
    }

    @Override
    public void dispose()
    {
        if(!mPreserveQueueOnStop.get())
        {
            discardQueue();
        }
    }

    private void discardQueue()
    {
        if(mPendingQueue.discard())
        {
            PENDING_QUEUES.remove(getBroadcastConfiguration().getId(), mPendingQueue);
        }
    }

    /** Releases calls retained while a disabled or edited destination was waiting to restart. */
    public static void discardPreservedQueue(int configurationId)
    {
        PendingQueue queue = PENDING_QUEUES.remove(configurationId);
        if(queue != null)
        {
            queue.discard();
        }
    }

    /** Returns calls retained for a disabled destination that does not currently have a broadcaster instance. */
    public static int getPreservedQueueSize(int configurationId)
    {
        PendingQueue queue = PENDING_QUEUES.get(configurationId);
        return queue != null ? queue.size() : 0;
    }

    private void cancelActiveUploads(boolean retryImmediately)
    {
        for(ActiveUpload activeUpload: mActiveUploads)
        {
            activeUpload.setRetryImmediately(retryImmediately);
            activeUpload.future().cancel(true);
        }
    }

    private void expediteQueuedCalls()
    {
        QueuedCall call;
        long now = System.currentTimeMillis();
        int queued = mQueue.size();
        for(int x = 0; x < queued && (call = mQueue.poll()) != null; x++)
        {
            mPendingQueue.offer(new QueuedCall(call.recording(), call.attempt(), now));
        }
        queueChanged();
    }

    private void releaseQueuedCall(QueuedCall call)
    {
        call.recording().removePendingReplay();
    }

    private void requeueImmediately(QueuedCall call)
    {
        mPendingQueue.offer(new QueuedCall(call.recording(), call.attempt(), System.currentTimeMillis()));
    }

    @Override
    public int getAudioQueueSize()
    {
        return mQueue.size() + mActiveUploads.size();
    }

    public int getActiveUploadCount() { return mActiveUploads.size(); }

    @Override
    public void receive(AudioRecording recording)
    {
        mPendingQueue.offer(new QueuedCall(recording, 0, System.currentTimeMillis()));
        queueChanged();
    }

    private void processQueue()
    {
        try
        {
            while(mRunning.get() && mUploadSlots.tryAcquire())
            {
                QueuedCall call = mQueue.peek();
                if(call == null || call.nextAttempt() > System.currentTimeMillis())
                {
                    mUploadSlots.release();
                    return;
                }
                mQueue.poll();
                queueChanged();
                if(!getBroadcastConfiguration().isRetryIndefinitely() &&
                    System.currentTimeMillis() - call.recording().getStartTime() >
                    getBroadcastConfiguration().getMaximumRecordingAge())
                {
                    call.recording().removePendingReplay();
                    incrementAgedOffAudioCount();
                    broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_AGED_OFF_COUNT_CHANGE));
                    mUploadSlots.release();
                    continue;
                }
                CompletableFuture<Void> upload;
                try
                {
                    upload = upload(call);
                }
                catch(Exception e)
                {
                    //A synchronous preparation failure must not permanently consume a semaphore permit.
                    try
                    {
                        retryOrFail(call, e);
                    }
                    finally
                    {
                        mUploadSlots.release();
                        queueChanged();
                    }
                    continue;
                }
                upload = upload.orTimeout(uploadWatchdogMillis(), TimeUnit.MILLISECONDS);
                ActiveUpload activeUpload = new ActiveUpload(upload);
                mActiveUploads.add(activeUpload);
                upload.whenComplete((ignored, error) ->
                {
                    try
                    {
                        mActiveUploads.remove(activeUpload);
                        if(activeUpload.retryImmediately() || (!mRunning.get() && mPreserveQueueOnStop.get()))
                        {
                            requeueImmediately(call);
                        }
                        else if(error == null)
                        {
                            connectionSucceeded();
                            incrementStreamedAudioCount();
                            broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_STREAMED_COUNT_CHANGE));
                            call.recording().removePendingReplay();
                        }
                        else
                        {
                            if(mRunning.get())
                            {
                                retryOrFail(call, error);
                            }
                            else
                            {
                                releaseQueuedCall(call);
                            }
                        }
                    }
                    finally
                    {
                        mUploadSlots.release();
                        queueChanged();
                    }
                });
            }
        }
        catch(Exception e)
        {
            mLog.error("Error processing remote call upload queue", e);
        }
    }

    private CompletableFuture<Void> upload(QueuedCall call)
    {
        AudioRecording recording = call.recording();
        CompletedCallMetadata metadata = CompletedCallMetadata.from(recording);
        return mSpeechProcessor.process(recording.getPath()).exceptionally(error ->
        {
            mLog.warn("Speech processing failed; uploading call without transcript: {}", error.getMessage());
            return CallTranscription.none();
        }).thenCompose(transcription ->
        {
            try
            {
                metadata.setTranscription(transcription);
                byte[] audio = Files.readAllBytes(recording.getPath());
                MultipartBody body = new MultipartBody()
                    .text("metadata", GSON.toJson(metadata))
                    .file("audio", recording.getPath().getFileName().toString(), "audio/mpeg", audio);
                HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(getBroadcastConfiguration().getHost()))
                    .timeout(mRequestTimeout)
                    .header("Content-Type", "multipart/form-data; boundary=" + body.boundary())
                    .header("User-Agent", "sdrtrunk")
                    .header("Idempotency-Key", metadata.getCallId())
                    .POST(body.publisher());
                addAuthentication(request);
                mLastCallAttempt = System.currentTimeMillis();
                mLastContactAttempt = mLastCallAttempt;
                HttpClient client = mHttpClient;
                return client.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString()).thenAccept(response ->
                {
                    if(response.statusCode() < 200 || response.statusCode() >= 300)
                    {
                        throw new IllegalStateException(responseError(response));
                    }
                    mLastCallSuccess = System.currentTimeMillis();
                    mLastCallError = "";
                });
            }
            catch(Exception e)
            {
                return CompletableFuture.failedFuture(e);
            }
        });
    }

    private void monitorHeartbeat()
    {
        try
        {
            if(!mRunning.get())
            {
                return;
            }

            long now = System.currentTimeMillis();
            if(isConnectionStale() && !mHeartbeatInFlight.get() && now - mLastReconnect > heartbeatIntervalMillis())
            {
                mLastHeartbeatError = "No successful contact within the heartbeat stale window";
                recoverConnection(mLastHeartbeatError);
                mNextHeartbeatAttempt = now;
            }

            if(now >= mNextHeartbeatAttempt)
            {
                sendHeartbeat();
            }
        }
        catch(Exception e)
        {
            mLastHeartbeatError = rootMessage(e);
            recoverConnection(mLastHeartbeatError);
            mNextHeartbeatAttempt = System.currentTimeMillis() + retryDelayMillis();
            mHeartbeatInFlight.set(false);
            mLog.error("Error monitoring Remote API heartbeat for [{}]",
                getBroadcastConfiguration().getName(), e);
        }
    }

    private void sendHeartbeat()
    {
        long clientGeneration;
        CompletableFuture<HttpResponse<String>> heartbeatRequest;
        Exception preparationError = null;

        synchronized(mConnectionLock)
        {
            if(!mRunning.get() || !mHeartbeatInFlight.compareAndSet(false, true))
            {
                return;
            }

            mLastHeartbeatAttempt = System.currentTimeMillis();
            mLastContactAttempt = mLastHeartbeatAttempt;
            clientGeneration = mHttpClientGeneration.get();
            try
            {
                Map<String,Object> heartbeat = new LinkedHashMap<>();
                heartbeat.put("event", "heartbeat");
                heartbeat.put("status", "online");
                heartbeat.put("connected", true);
                heartbeat.put("timestamp", mLastHeartbeatAttempt);
                heartbeat.put("timestampIso", Instant.ofEpochMilli(mLastHeartbeatAttempt).toString());
                heartbeat.put("application", "sdrtrunk");
                heartbeat.put("version", applicationVersion());
                heartbeat.put("destination", getBroadcastConfiguration().getName());
                heartbeat.put("hostname", hostname());
                heartbeat.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
                heartbeat.put("queuedCalls", getAudioQueueSize());

                HttpRequest.Builder request =
                    HttpRequest.newBuilder(URI.create(getBroadcastConfiguration().getHost()))
                        .timeout(mRequestTimeout)
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .header("User-Agent", "sdrtrunk")
                        .header("X-SDRTrunk-Event", "heartbeat")
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(heartbeat)));
                addAuthentication(request);
                heartbeatRequest = mHttpClient.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString())
                    .orTimeout(mRequestTimeout.toMillis() + 1_000L, TimeUnit.MILLISECONDS);
                mHeartbeatRequest = heartbeatRequest;
            }
            catch(Exception e)
            {
                heartbeatRequest = null;
                preparationError = e;
            }
        }

        if(preparationError != null)
        {
            completeHeartbeat(null, preparationError, clientGeneration);
        }
        else
        {
            heartbeatRequest.whenComplete((response, error) ->
                completeHeartbeat(response, error, clientGeneration));
        }
    }

    private void completeHeartbeat(HttpResponse<String> response, Throwable error, long clientGeneration)
    {
        if(!mRunning.get() || clientGeneration != mHttpClientGeneration.get())
        {
            return;
        }

        try
        {
            if(error == null && response != null && response.statusCode() >= 200 && response.statusCode() < 300)
            {
                mLastHeartbeatSuccess = System.currentTimeMillis();
                mLastHeartbeatError = "";
                connectionSucceeded();
                mNextHeartbeatAttempt = System.currentTimeMillis() + heartbeatIntervalMillis();
            }
            else
            {
                String message = error != null ? rootMessage(error) :
                    (response != null ? responseError(response) : "Remote API returned an unknown response");
                mLastHeartbeatError = message;
                recoverConnection(message);
                mNextHeartbeatAttempt = System.currentTimeMillis() + retryDelayMillis();
                mLog.warn("Remote API heartbeat failed for [{}]: {}", getBroadcastConfiguration().getName(), message);
            }
        }
        finally
        {
            if(clientGeneration == mHttpClientGeneration.get())
            {
                mHeartbeatRequest = null;
                mHeartbeatInFlight.set(false);
            }
        }
    }

    /** Discards pooled HTTP connections and schedules an immediate health check without dropping queued calls. */
    public void reconnect()
    {
        if(!mRunning.get())
        {
            return;
        }

        cancelActiveUploads(true);
        expediteQueuedCalls();
        rebuildHttpClient();
        mConsecutiveConnectionFailures.set(0);
        mLastHeartbeatError = "";
        mLastCallError = "";
        if(getBroadcastConfiguration().isHeartbeatEnabled())
        {
            setBroadcastState(BroadcastState.CONNECTING);
            mNextHeartbeatAttempt = 0;
        }
        else
        {
            setBroadcastState(BroadcastState.CONNECTED);
        }
    }

    private HttpClient createHttpClient()
    {
        return HttpClient.newBuilder().connectTimeout(mRequestTimeout)
            .followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    private void rebuildHttpClient()
    {
        synchronized(mConnectionLock)
        {
            mHttpClientGeneration.incrementAndGet();
            CompletableFuture<HttpResponse<String>> heartbeatRequest = mHeartbeatRequest;
            mHeartbeatRequest = null;
            mHeartbeatInFlight.set(false);
            if(heartbeatRequest != null && !heartbeatRequest.isDone())
            {
                heartbeatRequest.cancel(true);
            }
            mHttpClient = createHttpClient();
            mLastReconnect = System.currentTimeMillis();
            mReconnectCount.incrementAndGet();
        }
    }

    private void recoverConnection(String message)
    {
        mConsecutiveConnectionFailures.incrementAndGet();
        cancelActiveUploads(true);
        expediteQueuedCalls();
        rebuildHttpClient();
        mNextHeartbeatAttempt = 0;
        setBroadcastState(BroadcastState.CONNECTING);
        mLog.info("Remote API connection [{}] is reconnecting after: {}",
            getBroadcastConfiguration().getName(), message);
    }

    private void connectionSucceeded()
    {
        mLastContactSuccess = System.currentTimeMillis();
        mConsecutiveConnectionFailures.set(0);
        setBroadcastState(BroadcastState.CONNECTED);
    }

    private long heartbeatIntervalMillis()
    {
        return TimeUnit.SECONDS.toMillis(getBroadcastConfiguration().getHeartbeatIntervalSeconds());
    }

    private long staleWindowMillis()
    {
        return Math.max(heartbeatIntervalMillis() * 3L, mRequestTimeout.toMillis() * 2L + 2_000L);
    }

    private long retryDelayMillis()
    {
        int failures = Math.min(mConsecutiveConnectionFailures.get(), 6);
        long backoff = 1_000L << Math.max(0, failures - 1);
        return Math.min(heartbeatIntervalMillis(), Math.min(60_000L, backoff));
    }

    private long uploadWatchdogMillis()
    {
        //Covers speech processing plus the HTTP request and guarantees that a wedged future cannot consume an
        //upload slot forever.
        return Math.max(30_000L, mRequestTimeout.toMillis() * 2L + 5_000L);
    }

    public boolean isConnectionStale()
    {
        if(!mRunning.get() || !getBroadcastConfiguration().isHeartbeatEnabled())
        {
            return false;
        }
        long lastSuccess = Math.max(mLastHeartbeatSuccess, mLastContactSuccess);
        long reference = lastSuccess > 0 ? lastSuccess : mStarted;
        return reference > 0 && System.currentTimeMillis() - reference > staleWindowMillis();
    }

    private static String responseError(HttpResponse<String> response)
    {
        String body = response.body();
        if(body == null || body.isBlank())
        {
            return "Remote API returned HTTP " + response.statusCode();
        }
        String compact = body.replaceAll("[\\r\\n]+", " ").trim();
        if(compact.length() > 300)
        {
            compact = compact.substring(0, 300) + "...";
        }
        return "Remote API returned HTTP " + response.statusCode() + ": " + compact;
    }

    private void addAuthentication(HttpRequest.Builder request)
    {
        String key = getBroadcastConfiguration().resolveApiKey();
        String header = getBroadcastConfiguration().getAuthenticationHeader();
        if(key != null && !key.isBlank() && header != null && !header.isBlank())
        {
            String prefix = getBroadcastConfiguration().getAuthenticationPrefix();
            request.header(header, (prefix != null ? prefix : "") + key);
        }
    }

    private static String hostname()
    {
        try { return InetAddress.getLocalHost().getHostName(); }
        catch(Exception e) { return "unknown"; }
    }

    private static String applicationVersion()
    {
        String version = RemoteApiBroadcaster.class.getPackage().getImplementationVersion();
        return version != null && !version.isBlank() ? version : "development";
    }

    private static String rootMessage(Throwable error)
    {
        Throwable cause = error;
        while(cause.getCause() != null) { cause = cause.getCause(); }
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }

    public long getLastHeartbeatAttempt() { return mLastHeartbeatAttempt; }
    public long getLastHeartbeatSuccess() { return mLastHeartbeatSuccess; }
    public String getLastHeartbeatError() { return mLastHeartbeatError; }
    public long getLastCallAttempt() { return mLastCallAttempt; }
    public long getLastCallSuccess() { return mLastCallSuccess; }
    public String getLastCallError() { return mLastCallError; }
    public long getLastContactAttempt() { return mLastContactAttempt; }
    public long getLastContactSuccess() { return mLastContactSuccess; }
    public long getLastReconnect() { return mLastReconnect; }
    public int getReconnectCount() { return mReconnectCount.get(); }
    public int getConsecutiveConnectionFailures() { return mConsecutiveConnectionFailures.get(); }

    private void retryOrFail(QueuedCall call, Throwable error)
    {
        mLastCallError = rootMessage(error);
        recoverConnection(mLastCallError);
        int nextAttempt = call.attempt() + 1;
        if(mRunning.get() && (getBroadcastConfiguration().isRetryIndefinitely() ||
            nextAttempt <= getBroadcastConfiguration().getMaximumRetries()))
        {
            long delay = getBroadcastConfiguration().isRetryIndefinitely() &&
                nextAttempt > getBroadcastConfiguration().getMaximumRetries() ? 300_000L :
                Math.min(300_000L, 1_000L << Math.min(nextAttempt - 1, 18));
            long jitter = (long)(Math.random() * Math.max(1, delay / 4));
            mPendingQueue.offer(new QueuedCall(call.recording(), nextAttempt,
                System.currentTimeMillis() + delay + jitter));
            String retryLimit = getBroadcastConfiguration().isRetryIndefinitely() ? "unlimited" :
                Integer.toString(getBroadcastConfiguration().getMaximumRetries());
            mLog.warn("Remote call upload failed; retry {}/{} scheduled: {}", nextAttempt, retryLimit,
                rootMessage(error));
        }
        else
        {
            incrementErrorAudioCount();
            broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_ERROR_COUNT_CHANGE));
            call.recording().removePendingReplay();
            mLog.error("Remote call upload permanently failed after {} attempts", nextAttempt, error);
        }
    }

    private void queueChanged()
    {
        broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_QUEUE_CHANGE));
    }

    private record QueuedCall(AudioRecording recording, int attempt, long nextAttempt) {}

    private static class ActiveUpload
    {
        private final CompletableFuture<Void> mFuture;
        private final AtomicBoolean mRetryImmediately = new AtomicBoolean();

        private ActiveUpload(CompletableFuture<Void> future)
        {
            mFuture = future;
        }

        private CompletableFuture<Void> future() { return mFuture; }
        private boolean retryImmediately() { return mRetryImmediately.get(); }
        private void setRetryImmediately(boolean value) { mRetryImmediately.set(value); }
    }

    private static class PendingQueue
    {
        private final Queue<QueuedCall> mQueue = new PriorityBlockingQueue<>(32,
            Comparator.comparingLong(QueuedCall::nextAttempt));
        private final AtomicBoolean mDiscarded = new AtomicBoolean();

        private Queue<QueuedCall> queue() { return mQueue; }
        private int size() { return mQueue.size(); }

        private synchronized boolean offer(QueuedCall call)
        {
            if(mDiscarded.get())
            {
                call.recording().removePendingReplay();
                return false;
            }
            mQueue.offer(call);
            return true;
        }

        private synchronized boolean discard()
        {
            if(!mDiscarded.compareAndSet(false, true))
            {
                return false;
            }
            QueuedCall call;
            while((call = mQueue.poll()) != null)
            {
                call.recording().removePendingReplay();
            }
            return true;
        }
    }
}
