package io.github.dsheirer.audio.broadcast.remote;

import com.google.gson.Gson;
import io.github.dsheirer.audio.broadcast.AbstractAudioBroadcaster;
import io.github.dsheirer.audio.broadcast.AudioRecording;
import io.github.dsheirer.audio.broadcast.BroadcastEvent;
import io.github.dsheirer.audio.broadcast.BroadcastState;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.util.ThreadPool;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
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
    private static final String QUEUE_DIRECTORY = "remote_api_queue";
    private static final Map<String,PendingQueue> PENDING_QUEUES = new ConcurrentHashMap<>();
    private final PendingQueue mPendingQueue;
    private final Queue<QueuedCall> mQueue;
    private final Path mQueuePath;
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
        this(configuration, SystemProperties.getInstance().getApplicationFolder("streaming"));
    }

    /** Constructor with an explicit queue root, used by restart-safety tests. */
    RemoteApiBroadcaster(RemoteApiConfiguration configuration, Path streamingDirectory)
    {
        super(configuration);
        String queueId = configuration.getQueueId();
        Path queuePath = streamingDirectory.resolve(QUEUE_DIRECTORY).resolve(queueId);
        mPendingQueue = PENDING_QUEUES.computeIfAbsent(queueId, ignored -> new PendingQueue(queuePath));
        mQueue = mPendingQueue.queue();
        mQueuePath = mPendingQueue.path();
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
        //Stopping, disabling and application shutdown preserve pending calls.  Destination deletion explicitly
        //removes the durable queue through discardPreservedQueue().
        stop(true);
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

    /** Releases calls retained while a disabled or edited destination was waiting to restart. */
    public static void discardPreservedQueue(String queueId)
    {
        PendingQueue queue = PENDING_QUEUES.remove(queueId);
        if(queue != null)
        {
            queue.discard();
        }
        else
        {
            discardDirectory(defaultQueuePath(queueId));
        }
    }

    /** Returns calls retained for a disabled destination that does not currently have a broadcaster instance. */
    public static int getPreservedQueueSize(RemoteApiConfiguration configuration)
    {
        PendingQueue queue = PENDING_QUEUES.get(configuration.getQueueId());
        return queue != null ? queue.size() : persistedQueueSize(defaultQueuePath(configuration.getQueueId()));
    }

    private void discardQueue()
    {
        if(mPendingQueue.discard())
        {
            PENDING_QUEUES.remove(getBroadcastConfiguration().getQueueId(), mPendingQueue);
        }
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
            call.setNextAttempt(now);
            persistMetadataQuietly(call);
            mPendingQueue.offer(call);
        }
        queueChanged();
    }

    private void releaseQueuedCall(QueuedCall call)
    {
        complete(call);
    }

    private void requeueImmediately(QueuedCall call)
    {
        call.setNextAttempt(System.currentTimeMillis());
        persistMetadataQuietly(call);
        mPendingQueue.offer(call);
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
        if(recording == null)
        {
            return;
        }

        try
        {
            mPendingQueue.offer(persist(recording));
            recording.removePendingReplay();
            queueChanged();
        }
        catch(Exception e)
        {
            incrementErrorAudioCount();
            recording.removePendingReplay();
            broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_ERROR_COUNT_CHANGE));
            mLog.error("Unable to save Remote API call to the durable queue", e);
        }
    }

    private void processQueue()
    {
        try
        {
            while(mRunning.get() && mUploadSlots.tryAcquire())
            {
                QueuedCall call = mQueue.peek();
                if(call == null || call.getNextAttempt() > System.currentTimeMillis())
                {
                    mUploadSlots.release();
                    return;
                }
                mQueue.poll();
                queueChanged();
                if(!getBroadcastConfiguration().isRetryIndefinitely() &&
                    System.currentTimeMillis() - call.getMetadata().getTimestamp() >
                    getBroadcastConfiguration().getMaximumRecordingAge())
                {
                    complete(call);
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
                            complete(call);
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
        Path audioPath = mQueuePath.resolve(call.getAudioFile());
        CompletedCallMetadata metadata = call.getMetadata();
        return mSpeechProcessor.process(audioPath).exceptionally(error ->
        {
            mLog.warn("Speech processing failed; uploading call without transcript: {}", error.getMessage());
            return CallTranscription.none();
        }).thenCompose(transcription ->
        {
            try
            {
                metadata.setTranscription(transcription);
                byte[] audio = Files.readAllBytes(audioPath);
                MultipartBody body = new MultipartBody()
                    .text("metadata", GSON.toJson(metadata))
                    .file("audio", call.getAudioName(), "audio/mpeg", audio);
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
        int nextAttempt = call.getAttempt() + 1;
        if(mRunning.get() && (getBroadcastConfiguration().isRetryIndefinitely() ||
            nextAttempt <= getBroadcastConfiguration().getMaximumRetries()))
        {
            long delay = getBroadcastConfiguration().isRetryIndefinitely() &&
                nextAttempt > getBroadcastConfiguration().getMaximumRetries() ? 300_000L :
                Math.min(300_000L, 1_000L << Math.min(nextAttempt - 1, 18));
            long jitter = (long)(Math.random() * Math.max(1, delay / 4));
            call.setAttempt(nextAttempt);
            call.setNextAttempt(System.currentTimeMillis() + delay + jitter);
            persistMetadataQuietly(call);
            mPendingQueue.offer(call);
            String retryLimit = getBroadcastConfiguration().isRetryIndefinitely() ? "unlimited" :
                Integer.toString(getBroadcastConfiguration().getMaximumRetries());
            mLog.warn("Remote call upload failed; retry {}/{} scheduled: {}", nextAttempt, retryLimit,
                rootMessage(error));
        }
        else
        {
            incrementErrorAudioCount();
            broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_ERROR_COUNT_CHANGE));
            complete(call);
            mLog.error("Remote call upload permanently failed after {} attempts", nextAttempt, error);
        }
    }

    private void queueChanged()
    {
        broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_QUEUE_CHANGE));
    }

    private QueuedCall persist(AudioRecording recording) throws IOException
    {
        Files.createDirectories(mQueuePath);
        String id = UUID.randomUUID().toString();
        String extension = extension(recording.getPath());
        String audioFile = id + extension;
        Path temporaryAudio = mQueuePath.resolve(audioFile + ".tmp");
        Path durableAudio = mQueuePath.resolve(audioFile);
        Files.copy(recording.getPath(), temporaryAudio, StandardCopyOption.REPLACE_EXISTING);
        moveAtomically(temporaryAudio, durableAudio);
        QueuedCall call = new QueuedCall(id, audioFile, recording.getPath().getFileName().toString(),
            CompletedCallMetadata.from(recording), 0, System.currentTimeMillis());
        persistMetadata(call);
        return call;
    }

    private void complete(QueuedCall call)
    {
        try
        {
            Files.deleteIfExists(mQueuePath.resolve(call.getAudioFile()));
            Files.deleteIfExists(metadataPath(call));
        }
        catch(IOException e)
        {
            //The stable call id makes retrying a leftover item safe for receivers that honor idempotency keys.
            mLog.warn("Unable to remove completed Remote API queue item [{}]", call.getId(), e);
        }
    }

    private void persistMetadata(QueuedCall call) throws IOException
    {
        Files.createDirectories(mQueuePath);
        Path temporary = mQueuePath.resolve(call.getId() + ".json.tmp");
        Files.writeString(temporary, GSON.toJson(call), StandardCharsets.UTF_8);
        moveAtomically(temporary, metadataPath(call));
    }

    private void persistMetadataQuietly(QueuedCall call)
    {
        try
        {
            persistMetadata(call);
        }
        catch(IOException e)
        {
            mLog.error("Unable to update Remote API durable queue item [{}]", call.getId(), e);
        }
    }

    private Path metadataPath(QueuedCall call)
    {
        return mQueuePath.resolve(call.getId() + ".json");
    }

    private static void moveAtomically(Path source, Path destination) throws IOException
    {
        try
        {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch(AtomicMoveNotSupportedException e)
        {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String extension(Path path)
    {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : ".mp3";
    }

    private static Path defaultQueuePath(String queueId)
    {
        return SystemProperties.getInstance().getApplicationFolder("streaming")
            .resolve(QUEUE_DIRECTORY).resolve(queueId);
    }

    private static int persistedQueueSize(Path queuePath)
    {
        if(!Files.isDirectory(queuePath))
        {
            return 0;
        }
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(queuePath, "*.json"))
        {
            int count = 0;
            for(Path ignored: stream) { count++; }
            return count;
        }
        catch(IOException e)
        {
            return 0;
        }
    }

    private static void discardDirectory(Path queuePath)
    {
        if(!Files.isDirectory(queuePath))
        {
            return;
        }
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(queuePath))
        {
            for(Path path: stream) { Files.deleteIfExists(path); }
            Files.deleteIfExists(queuePath);
        }
        catch(IOException e)
        {
            mLog.warn("Unable to discard Remote API durable queue [{}]", queuePath, e);
        }
    }

    private static class QueuedCall
    {
        private String mId;
        private String mAudioFile;
        private String mAudioName;
        private CompletedCallMetadata mMetadata;
        private int mAttempt;
        private long mNextAttempt;

        @SuppressWarnings("unused")
        private QueuedCall() {}

        private QueuedCall(String id, String audioFile, String audioName, CompletedCallMetadata metadata,
                           int attempt, long nextAttempt)
        {
            mId = id;
            mAudioFile = audioFile;
            mAudioName = audioName;
            mMetadata = metadata;
            mAttempt = attempt;
            mNextAttempt = nextAttempt;
        }

        private String getId() { return mId; }
        private String getAudioFile() { return mAudioFile; }
        private String getAudioName() { return mAudioName; }
        private CompletedCallMetadata getMetadata() { return mMetadata; }
        private int getAttempt() { return mAttempt; }
        private void setAttempt(int value) { mAttempt = value; }
        private long getNextAttempt() { return mNextAttempt; }
        private void setNextAttempt(long value) { mNextAttempt = value; }
    }

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
            Comparator.comparingLong(QueuedCall::getNextAttempt));
        private final AtomicBoolean mDiscarded = new AtomicBoolean();
        private final Path mPath;

        private PendingQueue(Path path)
        {
            mPath = path;
            load();
        }

        private Queue<QueuedCall> queue() { return mQueue; }
        private Path path() { return mPath; }
        private int size() { return mQueue.size(); }

        private synchronized boolean offer(QueuedCall call)
        {
            if(mDiscarded.get())
            {
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
                try
                {
                    Files.deleteIfExists(mPath.resolve(call.getAudioFile()));
                    Files.deleteIfExists(mPath.resolve(call.getId() + ".json"));
                }
                catch(IOException e)
                {
                    mLog.warn("Unable to discard Remote API queue item [{}]", call.getId(), e);
                }
            }
            discardDirectory(mPath);
            return true;
        }

        private void load()
        {
            try
            {
                Files.createDirectories(mPath);
                try(DirectoryStream<Path> stream = Files.newDirectoryStream(mPath, "*.json"))
                {
                    for(Path metadataPath: stream)
                    {
                        try
                        {
                            QueuedCall call = GSON.fromJson(
                                Files.readString(metadataPath, StandardCharsets.UTF_8), QueuedCall.class);
                            if(call != null && call.getId() != null && call.getAudioFile() != null &&
                                call.getMetadata() != null && Files.isRegularFile(mPath.resolve(call.getAudioFile())))
                            {
                                mQueue.offer(call);
                            }
                            else
                            {
                                Files.deleteIfExists(metadataPath);
                            }
                        }
                        catch(Exception e)
                        {
                            mLog.error("Unable to load Remote API queue metadata [{}]", metadataPath, e);
                        }
                    }
                }
                if(!mQueue.isEmpty())
                {
                    mLog.info("Restored [{}] pending Remote API calls from [{}]", mQueue.size(), mPath);
                }
            }
            catch(IOException e)
            {
                mLog.error("Unable to initialize Remote API durable queue [{}]", mPath, e);
            }
        }
    }
}
