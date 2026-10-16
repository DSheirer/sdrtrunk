/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.audio.broadcast.rdioscanner;

import com.google.gson.Gson;
import com.google.common.net.HttpHeaders;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.AbstractAudioBroadcaster;
import io.github.dsheirer.audio.broadcast.AudioRecording;
import io.github.dsheirer.audio.broadcast.BroadcastEvent;
import io.github.dsheirer.audio.broadcast.BroadcastState;
import io.github.dsheirer.audio.convert.InputAudioFormat;
import io.github.dsheirer.audio.convert.MP3Setting;
import io.github.dsheirer.gui.playlist.radioreference.RadioReferenceDecoder;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.alias.TalkerAliasIdentifier;
import io.github.dsheirer.identifier.configuration.ConfigurationLongIdentifier;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.util.ThreadPool;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Audio broadcaster to push completed audio recordings to the Rdio Scanner call upload API.
 *
 */
public class RdioScannerBroadcaster extends AbstractAudioBroadcaster<RdioScannerConfiguration>
{
    private final static Logger mLog = LoggerFactory.getLogger(RdioScannerBroadcaster.class);
    private static final Gson GSON = new Gson();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private static final String ENCODING_TYPE_MP3 = "mp3";
    private static final String MULTIPART_TYPE = "multipart";
    private static final String DEFAULT_SUBTYPE = "form-data";
    private static final String MULTIPART_FORM_DATA = MULTIPART_TYPE + "/" + DEFAULT_SUBTYPE;
    private static final String QUEUE_DIRECTORY = "rdioscanner_queue";
    private static final int MAXIMUM_CONCURRENT_UPLOADS = 2;
    private final Queue<QueuedCall> mAudioRecordingQueue = new PriorityBlockingQueue<>(32,
        Comparator.comparingLong(QueuedCall::getNextAttempt));
    private final Set<ActiveUpload> mActiveUploads = ConcurrentHashMap.newKeySet();
    private final Path mQueuePath;
    private ScheduledFuture<?> mAudioRecordingProcessorFuture;
    private ScheduledFuture<?> mHeartbeatProcessorFuture;
    private volatile HttpClient mHttpClient = createHttpClient();
    private volatile CompletableFuture<HttpResponse<String>> mHeartbeatRequest;
    private final AtomicBoolean mRunning = new AtomicBoolean();
    private final AtomicBoolean mConnectionInFlight = new AtomicBoolean();
    private final AtomicBoolean mHeartbeatInFlight = new AtomicBoolean();
    private final AtomicInteger mConnectionFailures = new AtomicInteger();
    private final AtomicInteger mHeartbeatFailures = new AtomicInteger();
    private volatile long mNextConnectionAttempt;
    private volatile long mLastConnectionAttempt;
    private volatile long mLastConnectionSuccess;
    private volatile String mLastConnectionError = "";
    private volatile long mNextHeartbeatAttempt;
    private volatile long mLastHeartbeatAttempt;
    private volatile long mLastHeartbeatSuccess;
    private volatile String mLastHeartbeatError = "";
    private AliasModel mAliasModel;

    /**
     * Constructs an instance of the broadcaster
     * @param config to use
     * @param aliasModel for access to aliases
     */
    public RdioScannerBroadcaster(RdioScannerConfiguration config, InputAudioFormat inputAudioFormat,
                                       MP3Setting mp3Setting, AliasModel aliasModel)
    {
        this(config, inputAudioFormat, mp3Setting, aliasModel,
            SystemProperties.getInstance().getApplicationFolder("streaming"));
    }

    /**
     * Constructs an instance with an explicit streaming directory.  The overload is also used by tests to verify
     * restart-safe queue behavior without writing to the user's application directory.
     */
    public RdioScannerBroadcaster(RdioScannerConfiguration config, InputAudioFormat inputAudioFormat,
                                  MP3Setting mp3Setting, AliasModel aliasModel, Path streamingDirectory)
    {
        super(config);
        mAliasModel = aliasModel;
        mQueuePath = streamingDirectory.resolve(QUEUE_DIRECTORY).resolve(config.getQueueId());
        loadPersistedQueue();
    }

    /**
     * Starts the audio recording processor thread
     */
    @Override
    public void start()
    {
        if(mRunning.compareAndSet(false, true))
        {
            mNextConnectionAttempt = 0;
            setBroadcastState(BroadcastState.CONNECTING);

            if(mAudioRecordingProcessorFuture == null)
            {
                //A fixed-delay guarded task cannot permanently stop after one unexpected exception.
                mAudioRecordingProcessorFuture = ThreadPool.SCHEDULED.scheduleWithFixedDelay(
                    new AudioRecordingProcessor(), 0, 500, TimeUnit.MILLISECONDS);
            }

            if(getBroadcastConfiguration().isHeartbeatEnabled() && mHeartbeatProcessorFuture == null)
            {
                mNextHeartbeatAttempt = 0;
                mHeartbeatProcessorFuture = ThreadPool.SCHEDULED.scheduleWithFixedDelay(this::monitorHeartbeat,
                    0, 1, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Stops the audio recording processor thread
     */
    @Override
    public void stop()
    {
        mRunning.set(false);
        if(mAudioRecordingProcessorFuture != null)
        {
            mAudioRecordingProcessorFuture.cancel(true);
            mAudioRecordingProcessorFuture = null;
        }
        if(mHeartbeatProcessorFuture != null)
        {
            mHeartbeatProcessorFuture.cancel(true);
            mHeartbeatProcessorFuture = null;
        }
        CompletableFuture<HttpResponse<String>> heartbeatRequest = mHeartbeatRequest;
        if(heartbeatRequest != null)
        {
            heartbeatRequest.cancel(true);
            mHeartbeatRequest = null;
        }
        mHeartbeatInFlight.set(false);
        mConnectionInFlight.set(false);
        for(ActiveUpload activeUpload: mActiveUploads)
        {
            activeUpload.getFuture().cancel(true);
        }
        setBroadcastState(BroadcastState.DISCONNECTED);
    }

    /**
     * Prepares for disposal
     */
    @Override
    public void dispose()
    {
        //Pending calls are intentionally retained on disk.  A replacement broadcaster or the next application run
        //loads and resumes this queue.
    }

    /**
     * Indicates if this broadcaster continues to have successful connections to and transactions with the remote
     * server.  If there is a connectivity or other issue, the broadcast state is set to temporary error and
     * the audio processor thread will persistently invoke this method to attempt a reconnect.
     *
     * Rdio Scanner does not have a test API endpoint, so we look for the incomplete call response.
     */
    private boolean connected()
    {
        return getBroadcastState() == BroadcastState.CONNECTED;
    }

    /** Starts one asynchronous connection check when its backoff delay has elapsed. */
    private void ensureConnected()
    {
        long now = System.currentTimeMillis();
        if(!mRunning.get() || connected() || now < mNextConnectionAttempt ||
            !mConnectionInFlight.compareAndSet(false, true))
        {
            return;
        }

        mLastConnectionAttempt = now;
        setBroadcastState(BroadcastState.CONNECTING);
        ThreadPool.CACHED.submit(() ->
        {
            try
            {
                String response = testConnection(getBroadcastConfiguration());
                if(!mRunning.get())
                {
                    return;
                }

                if(isSuccessfulConnectionTest(response))
                {
                    mConnectionFailures.set(0);
                    mLastConnectionSuccess = System.currentTimeMillis();
                    mLastConnectionError = "";
                    mNextConnectionAttempt = 0;
                    mHttpClient = createHttpClient();
                    expediteQueuedCalls();
                    setBroadcastState(BroadcastState.CONNECTED);
                    mLog.info("Rdio Scanner connection restored for [{}]", getBroadcastConfiguration().getName());
                }
                else
                {
                    scheduleReconnect(response != null ? response : "No response");
                }
            }
            catch(Throwable t)
            {
                scheduleReconnect(rootMessage(t));
            }
            finally
            {
                mConnectionInFlight.set(false);
            }
        });
    }

    private static boolean isSuccessfulConnectionTest(String response)
    {
        if(response == null)
        {
            return false;
        }

        String normalized = response.toLowerCase();
        return normalized.startsWith("incomplete call data: no talkgroup") ||
            normalized.contains("call imported successfully") || normalized.contains("duplicate call rejected");
    }

    private void scheduleReconnect(String error)
    {
        if(!mRunning.get())
        {
            return;
        }

        mLastConnectionError = error;
        int failures = Math.min(mConnectionFailures.incrementAndGet(), 7);
        long delay = Math.min(60_000L, 1_000L << (failures - 1));
        mNextConnectionAttempt = System.currentTimeMillis() + delay;
        mHttpClient = createHttpClient();
        setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
        mLog.warn("Rdio Scanner connection failed for [{}]; retrying in {} seconds: {}",
            getBroadcastConfiguration().getName(), Math.max(1, delay / 1000), error);
    }

    private static HttpClient createHttpClient()
    {
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(REQUEST_TIMEOUT).build();
    }

    private void monitorHeartbeat()
    {
        if(mRunning.get() && System.currentTimeMillis() >= mNextHeartbeatAttempt)
        {
            sendHeartbeat();
        }
    }

    private void sendHeartbeat()
    {
        if(!mRunning.get() || !mHeartbeatInFlight.compareAndSet(false, true))
        {
            return;
        }

        mLastHeartbeatAttempt = System.currentTimeMillis();
        try
        {
            Map<String,Object> heartbeat = new LinkedHashMap<>();
            heartbeat.put("event", "heartbeat");
            heartbeat.put("status", "online");
            heartbeat.put("connected", getBroadcastState() == BroadcastState.CONNECTED);
            heartbeat.put("timestamp", mLastHeartbeatAttempt);
            heartbeat.put("timestampIso", Instant.ofEpochMilli(mLastHeartbeatAttempt).toString());
            heartbeat.put("application", "sdrtrunk");
            heartbeat.put("version", applicationVersion());
            heartbeat.put("destination", getBroadcastConfiguration().getName());
            heartbeat.put("hostname", hostname());
            heartbeat.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
            heartbeat.put("systemId", getBroadcastConfiguration().getSystemID());
            heartbeat.put("queuedCalls", getAudioQueueSize());

            HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(getBroadcastConfiguration().resolveHeartbeatUrl()))
                .timeout(REQUEST_TIMEOUT)
                .header(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8")
                .header(HttpHeaders.USER_AGENT, "sdrtrunk")
                .header("X-SDRTrunk-Event", "heartbeat")
                .header("X-RdioScanner-System-Id",
                    Integer.toString(getBroadcastConfiguration().getSystemID()))
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(heartbeat)));
            String apiKey = getBroadcastConfiguration().getApiKey();
            if(apiKey != null && !apiKey.isBlank())
            {
                request.header("X-API-Key", apiKey);
            }

            CompletableFuture<HttpResponse<String>> heartbeatRequest = mHttpClient
                .sendAsync(request.build(), HttpResponse.BodyHandlers.ofString())
                .orTimeout(REQUEST_TIMEOUT.toMillis() + 1_000L, TimeUnit.MILLISECONDS);
            mHeartbeatRequest = heartbeatRequest;
            heartbeatRequest.whenComplete(this::completeHeartbeat);
        }
        catch(Exception e)
        {
            completeHeartbeat(null, e);
        }
    }

    private void completeHeartbeat(HttpResponse<String> response, Throwable error)
    {
        if(!mRunning.get())
        {
            mHeartbeatInFlight.set(false);
            return;
        }

        if(error == null && response != null && response.statusCode() >= 200 && response.statusCode() < 300)
        {
            mLastHeartbeatSuccess = System.currentTimeMillis();
            mLastHeartbeatError = "";
            mHeartbeatFailures.set(0);
            mNextHeartbeatAttempt = System.currentTimeMillis() +
                TimeUnit.SECONDS.toMillis(getBroadcastConfiguration().getHeartbeatIntervalSeconds());
        }
        else
        {
            mLastHeartbeatError = error != null ? rootMessage(error) :
                "HTTP " + (response != null ? response.statusCode() : "unknown");
            int failures = Math.min(mHeartbeatFailures.incrementAndGet(), 7);
            long retryDelay = Math.min(TimeUnit.SECONDS.toMillis(
                getBroadcastConfiguration().getHeartbeatIntervalSeconds()),
                Math.min(60_000L, 1_000L << (failures - 1)));
            mNextHeartbeatAttempt = System.currentTimeMillis() + retryDelay;
            mHttpClient = createHttpClient();
            mLog.warn("Rdio Scanner heartbeat failed for [{}]: {}",
                getBroadcastConfiguration().getName(), mLastHeartbeatError);
        }
        mHeartbeatRequest = null;
        mHeartbeatInFlight.set(false);
    }

    private static String hostname()
    {
        try { return InetAddress.getLocalHost().getHostName(); }
        catch(Exception e) { return "unknown"; }
    }

    private static String applicationVersion()
    {
        String version = RdioScannerBroadcaster.class.getPackage().getImplementationVersion();
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
    public long getLastConnectionAttempt() { return mLastConnectionAttempt; }
    public long getLastConnectionSuccess() { return mLastConnectionSuccess; }
    public String getLastConnectionError() { return mLastConnectionError; }

    @Override
    public int getAudioQueueSize()
    {
        return mAudioRecordingQueue.size() + mActiveUploads.size();
    }

    @Override
    public void receive(AudioRecording audioRecording)
    {
        if(audioRecording == null)
        {
            return;
        }

        try
        {
            QueuedCall call = persist(audioRecording);
            mAudioRecordingQueue.offer(call);
            //The durable copy now owns the pending transaction, so the shared temporary streaming file can be cleaned.
            audioRecording.removePendingReplay();
            queueChanged();
        }
        catch(Exception e)
        {
            incrementErrorAudioCount();
            broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_ERROR_COUNT_CHANGE));
            audioRecording.removePendingReplay();
            mLog.error("Unable to save Rdio Scanner call to the durable queue", e);
        }
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

        QueuedCall call = new QueuedCall(id, audioFile, rdioAudioName(recording.getPath()),
            recording.getStartTime(), recording.getRecordingLength(), getTo(recording), getFrom(recording),
            getTalkerAlias(recording), getFrequency(recording), getPatches(recording), getTalkgroupLabel(recording),
            getTalkgroupGroup(recording), getSystemLabel(recording), 0, System.currentTimeMillis());
        persistMetadata(call);
        return call;
    }

    private void processRecordingQueue()
    {
        ensureConnected();
        if(!mRunning.get() || !connected())
        {
            return;
        }

        while(mActiveUploads.size() < MAXIMUM_CONCURRENT_UPLOADS)
        {
            QueuedCall call = mAudioRecordingQueue.peek();
            if(call == null || call.getNextAttempt() > System.currentTimeMillis())
            {
                return;
            }

            mAudioRecordingQueue.poll();
            queueChanged();
            try
            {
                upload(call);
            }
            catch(NoSuchFileException e)
            {
                //Another broadcaster instance may have completed this call while an edited destination restarted.
                complete(call);
            }
            catch(Exception e)
            {
                retryRecording(call, rootMessage(e));
            }
        }
    }

    private void upload(QueuedCall call) throws IOException
    {
        byte[] audioBytes = Files.readAllBytes(mQueuePath.resolve(call.getAudioFile()));
        RdioScannerBuilder bodyBuilder = new RdioScannerBuilder();
        bodyBuilder.addPart(FormField.KEY, getBroadcastConfiguration().getApiKey())
            .addPart(FormField.SYSTEM, getBroadcastConfiguration().getSystemID())
            .addAudioName(call.getAudioName())
            .addFile(audioBytes)
            .addPart(FormField.DATE_TIME, call.getStartTime() / 1000)
            .addPart(FormField.TALKGROUP_ID, call.getTalkgroup())
            .addPart(FormField.SOURCE, call.getRadioId())
            .addPart(FormField.FREQUENCY, call.getFrequency())
            .addPart(FormField.TALKER_ALIAS, call.getTalkerAlias())
            .addPart(FormField.TALKGROUP_LABEL, call.getTalkgroupLabel())
            .addPart(FormField.TALKGROUP_GROUP, call.getTalkgroupGroup())
            .addPart(FormField.SYSTEM_LABEL, call.getSystemLabel())
            .addPart(FormField.PATCHES, call.getPatches());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(getBroadcastConfiguration().getHost()))
            .timeout(REQUEST_TIMEOUT)
            .header(HttpHeaders.CONTENT_TYPE, MULTIPART_FORM_DATA + "; boundary=" + bodyBuilder.getBoundary())
            .header(HttpHeaders.USER_AGENT, "sdrtrunk")
            .POST(bodyBuilder.build()).build();
        CompletableFuture<HttpResponse<String>> future = mHttpClient
            .sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .orTimeout(REQUEST_TIMEOUT.toMillis() + 1_000L, TimeUnit.MILLISECONDS);
        ActiveUpload activeUpload = new ActiveUpload(call, future);
        mActiveUploads.add(activeUpload);
        queueChanged();
        future.whenComplete((response, error) -> completeUpload(activeUpload, response, error));
    }

    private void completeUpload(ActiveUpload upload, HttpResponse<String> response, Throwable error)
    {
        mActiveUploads.remove(upload);
        QueuedCall call = upload.getCall();
        if(!mRunning.get() && error != null)
        {
            //The durable files already represent this call.  A replacement instance will load them without the old
            //instance rewriting retry state after it has been stopped.
            queueChanged();
            return;
        }
        if(error != null || response == null || response.statusCode() != 200)
        {
            String message = error != null ? rootMessage(error) :
                "HTTP " + (response != null ? response.statusCode() + ": " + response.body() : "no response");
            retryRecording(call, message);
        }
        else
        {
            String body = response.body() != null ? response.body() : "";
            if(body.contains("Call imported successfully."))
            {
                complete(call);
                incrementStreamedAudioCount();
                broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_STREAMED_COUNT_CHANGE));
                connectionSucceeded();
            }
            else if(body.contains("duplicate call rejected"))
            {
                complete(call);
                incrementDuplicateRejectedAudioCount();
                broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_DUPLICATE_REJECTED_COUNT_CHANGE));
                connectionSucceeded();
            }
            else
            {
                retryRecording(call, "Unexpected response: " + body);
            }
        }
        queueChanged();
    }

    private void retryRecording(QueuedCall call, String message)
    {
        incrementErrorAudioCount();
        broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_ERROR_COUNT_CHANGE));
        int attempt = call.getAttempt() + 1;
        long delay = Math.min(300_000L, 1_000L << Math.min(attempt - 1, 18));
        call.setAttempt(attempt);
        call.setNextAttempt(System.currentTimeMillis() + delay);
        try
        {
            persistMetadata(call);
        }
        catch(IOException e)
        {
            mLog.error("Unable to update durable Rdio Scanner queue metadata", e);
        }
        if(mRunning.get())
        {
            mAudioRecordingQueue.offer(call);
        }
        scheduleReconnect(message);
        mLog.warn("Rdio Scanner upload failed; retry {} scheduled in {} seconds: {}", attempt,
            Math.max(1, delay / 1000), message);
        queueChanged();
    }

    private void connectionSucceeded()
    {
        mConnectionFailures.set(0);
        mLastConnectionSuccess = System.currentTimeMillis();
        mLastConnectionError = "";
        mNextConnectionAttempt = 0;
        if(mRunning.get())
        {
            setBroadcastState(BroadcastState.CONNECTED);
        }
    }

    private void expediteQueuedCalls()
    {
        QueuedCall call;
        int queued = mAudioRecordingQueue.size();
        long now = System.currentTimeMillis();
        for(int x = 0; x < queued && (call = mAudioRecordingQueue.poll()) != null; x++)
        {
            call.setNextAttempt(now);
            try
            {
                persistMetadata(call);
            }
            catch(IOException e)
            {
                mLog.warn("Unable to expedite queued Rdio Scanner call [{}]", call.getId(), e);
            }
            mAudioRecordingQueue.offer(call);
        }
        queueChanged();
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
            //Leaving metadata behind is safe: Rdio Scanner rejects the duplicate if it is retried after restart.
            mLog.warn("Unable to remove completed Rdio Scanner queue item [{}]", call.getId(), e);
        }
    }

    private void queueChanged()
    {
        broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_QUEUE_CHANGE));
    }

    private void loadPersistedQueue()
    {
        try
        {
            Files.createDirectories(mQueuePath);
            try(DirectoryStream<Path> stream = Files.newDirectoryStream(mQueuePath, "*.json"))
            {
                for(Path metadata: stream)
                {
                    try
                    {
                        QueuedCall call = GSON.fromJson(Files.readString(metadata, StandardCharsets.UTF_8),
                            QueuedCall.class);
                        if(call != null && call.getId() != null && call.getAudioFile() != null &&
                            Files.isRegularFile(mQueuePath.resolve(call.getAudioFile())))
                        {
                            mAudioRecordingQueue.offer(call);
                        }
                        else
                        {
                            Files.deleteIfExists(metadata);
                        }
                    }
                    catch(Exception e)
                    {
                        mLog.error("Unable to load Rdio Scanner queued call metadata [{}]", metadata, e);
                    }
                }
            }
            if(!mAudioRecordingQueue.isEmpty())
            {
                mLog.info("Restored [{}] pending Rdio Scanner calls for [{}]", mAudioRecordingQueue.size(),
                    getBroadcastConfiguration().getName());
            }
        }
        catch(IOException e)
        {
            mLog.error("Unable to initialize Rdio Scanner durable queue [{}]", mQueuePath, e);
        }
    }

    private void persistMetadata(QueuedCall call) throws IOException
    {
        Files.createDirectories(mQueuePath);
        Path metadata = metadataPath(call);
        Path temporary = mQueuePath.resolve(call.getId() + ".json.tmp");
        Files.writeString(temporary, GSON.toJson(call), StandardCharsets.UTF_8);
        moveAtomically(temporary, metadata);
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

    private static String rdioAudioName(Path path)
    {
        String name = path.getFileName().toString();
        int lastUnderscore = name.lastIndexOf('_');
        int previousUnderscore = lastUnderscore > 0 ? name.lastIndexOf('_', lastUnderscore - 1) : -1;
        return previousUnderscore >= 0 ? name.substring(previousUnderscore + 1) : name;
    }

    /** Returns the number of restart-safe calls waiting for a disabled destination. */
    public static int getPersistedQueueSize(RdioScannerConfiguration configuration, Path streamingDirectory)
    {
        Path path = streamingDirectory.resolve(QUEUE_DIRECTORY).resolve(configuration.getQueueId());
        if(!Files.isDirectory(path))
        {
            return 0;
        }
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.json"))
        {
            int count = 0;
            for(Path ignored: stream)
            {
                count++;
            }
            return count;
        }
        catch(IOException e)
        {
            return 0;
        }
    }

    /**
     * Creates a frequency value from the audio recording identifier collection.
     */
    private static Long getFrequency(AudioRecording audioRecording)
    {
        Identifier identifier = audioRecording.getIdentifierCollection().getIdentifier(IdentifierClass.CONFIGURATION,
            Form.CHANNEL_FREQUENCY, Role.ANY);

        if(identifier instanceof ConfigurationLongIdentifier)
        {
            Long value = ((ConfigurationLongIdentifier)identifier).getValue();

            if(value != null)
            {
                return value;
            }
        }

        return Long.valueOf(0);
    }

    /**
     * Creates a formatted string with the FROM identifier or uses a default of zero(0)
     */
    private static String getFrom(AudioRecording audioRecording)
    {
        for(Identifier identifier: audioRecording.getIdentifierCollection().getIdentifiers(Role.FROM))
        {
            if(identifier instanceof RadioIdentifier)
            {
                return ((RadioIdentifier)identifier).getValue().toString();
            }
        }

        return "0";
    }

    private static String getTalkerAlias(AudioRecording audioRecording)
    {
        for(Identifier identifier: audioRecording.getIdentifierCollection().getIdentifiers(Role.FROM))
        {
            if(identifier instanceof TalkerAliasIdentifier)
            {
                TalkerAliasIdentifier talkerID = (TalkerAliasIdentifier)identifier;

                if(talkerID.isValid())
                {
                    return talkerID.getValue();
                }
            }
        }

        return "";
    }

    /**
     * Creates a formatted string with the TO identifiers or uses a default of zero (0)
     * 
     */
    private String getTo(AudioRecording audioRecording)
    {
        Identifier identifier = audioRecording.getIdentifierCollection().getToIdentifier();

        if(identifier != null)
        {
            AliasList aliasList = mAliasModel != null ?
                mAliasModel.getAliasList(audioRecording.getIdentifierCollection()) : null;

            if(aliasList != null)
            {
                List<Alias> aliases = aliasList.getAliases(identifier);

                //Check for 'Stream As Talkgroup' alias and use this instead of the decoded TO value.
                Optional<Alias> streamAs = aliases.stream().filter(alias -> alias.getStreamTalkgroupAlias() != null).findFirst();

                if(streamAs.isPresent())
                {
                    return String.valueOf(streamAs.get().getStreamTalkgroupAlias().getValue());
                }
            }

            if(identifier instanceof PatchGroupIdentifier patchGroupIdentifier)
            {
                return patchGroupIdentifier.getValue().getPatchGroup().getValue().toString();
            }
            else if(identifier instanceof TalkgroupIdentifier talkgroupIdentifier)
            {
                return String.valueOf(RadioReferenceDecoder.convertToRadioReferenceTalkgroup(talkgroupIdentifier.getValue(),
                        talkgroupIdentifier.getProtocol()));
            }
            else if(identifier instanceof RadioIdentifier radioIdentifier)
            {
                return radioIdentifier.getValue().toString();
            }
        }

        return "0";
    }

    /**
     * Creates a formatted string with the Talkgroup Label from the Audio Recording Alias
     * If this is a PatchGroup we return only the first label as the primary talkgroup label.
     * 
     */
    private String getTalkgroupLabel(AudioRecording audioRecording)
    {
        AliasList aliasList = mAliasModel != null ?
            mAliasModel.getAliasList(audioRecording.getIdentifierCollection()) : null;
        Identifier identifier = audioRecording.getIdentifierCollection().getToIdentifier();

        if(aliasList != null && identifier != null)
        {
            List<Alias> aliases = aliasList.getAliases(identifier);
            for(Alias alias: aliases)
            {
                if(alias != null && alias.getName() != null && !alias.getName().isBlank())
                {
                    return alias.getName();
                }
            }
        }

        //Rdio Scanner calls this field a talkgroup label.  For an analog channel or an alias with no usable name,
        //send the configured playlist channel name instead of leaving the label blank/Unknown.
        Identifier channel = audioRecording.getIdentifierCollection().getIdentifier(
            IdentifierClass.CONFIGURATION, Form.CHANNEL, Role.ANY);
        if(channel == null)
        {
            channel = audioRecording.getIdentifierCollection().getIdentifier(
                IdentifierClass.DECODER, Form.CHANNEL_NAME, Role.BROADCAST);
        }

        return channel != null ? channel.toString() : "";
    }

    /**
     * Creates a formatted string with the Talkgroup Group from the Audio Recording Alias
     * If this is a PatchGroup we return only the first group as the primary talkgroup group.
     * 
     */
    private String getTalkgroupGroup(AudioRecording audioRecording)
    {

        AliasList aliasList = mAliasModel != null ?
            mAliasModel.getAliasList(audioRecording.getIdentifierCollection()) : null;
        Identifier identifier = audioRecording.getIdentifierCollection().getToIdentifier();

        StringBuilder sb = new StringBuilder();
        if(identifier != null && aliasList != null)
        {
            List<Alias> aliases = aliasList.getAliases(identifier);
            if(!aliases.isEmpty())
            {
                sb.append(aliases.get(0).getGroup());
            }
        }

        return sb.toString();
    }

    /**
     * Creates a formatted string with the System Label from the Audio Recording Alias
     * If this is a PatchGroup we return only the first sytem as the primary talkgroup system.
     * 
     */
    private String getSystemLabel(AudioRecording audioRecording)
    {
        List<Identifier> systems = audioRecording.getIdentifierCollection().getIdentifiers(Form.SYSTEM);

        StringBuilder sb = new StringBuilder();
        if(!systems.isEmpty())
        {
           sb.append(systems.get(0));
        }

        return sb.toString();
    }

    /**
     * Creates a formatted string with the patched talkgroups
     */
    public static String getPatches(AudioRecording audioRecording)
    {
        Identifier identifier = audioRecording.getIdentifierCollection().getToIdentifier();

        if(identifier instanceof PatchGroupIdentifier patchGroupIdentifier)
        {
            PatchGroup patchGroup = patchGroupIdentifier.getValue();

            StringBuilder sb = new StringBuilder();
            sb.append("[");
            
            sb.append(patchGroup.getPatchGroup().getValue().toString());
            
            for(TalkgroupIdentifier patched: patchGroup.getPatchedTalkgroupIdentifiers())
            {
                sb.append(",").append(patched.getValue());
            }

            for(RadioIdentifier patched: patchGroup.getPatchedRadioIdentifiers())
            {
                sb.append(",").append(patched.getValue());
            }
            
            sb.append("]");
            return sb.toString();
        }

        return "[]";
    }

    /**
     * Tests both the connection and configuration against the RdioScanner Call API service
     * @param configuration containing API key and system id
     * @return error string or null if test is successful
     */
    public static String testConnection(RdioScannerConfiguration configuration)
    {
        HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

        RdioScannerBuilder bodyBuilder = new RdioScannerBuilder();
        bodyBuilder.addPart(FormField.KEY, configuration.getApiKey())
            .addPart(FormField.SYSTEM, configuration.getSystemID())
            .addPart(FormField.TEST, 1);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(configuration.getHost()))
            .header(HttpHeaders.CONTENT_TYPE, MULTIPART_FORM_DATA + "; boundary=" + bodyBuilder.getBoundary())
            .header(HttpHeaders.USER_AGENT, "sdrtrunk")
            .header(HttpHeaders.ACCEPT, "*/*")
            .POST(bodyBuilder.build())
            .build();

        HttpResponse.BodyHandler<String> responseHandler = HttpResponse.BodyHandlers.ofString();

        try
        {
            HttpResponse<String> response = httpClient.send(request, responseHandler);
            String responseBody = response.body();
            return (responseBody != null ? responseBody : "(no response)") + " Status Code:" + response.statusCode();
        }
        catch(Exception e)
        {
            return e.getLocalizedMessage();
        }
    }

    private static class ActiveUpload
    {
        private final QueuedCall mCall;
        private final CompletableFuture<HttpResponse<String>> mFuture;

        private ActiveUpload(QueuedCall call, CompletableFuture<HttpResponse<String>> future)
        {
            mCall = call;
            mFuture = future;
        }

        private QueuedCall getCall() { return mCall; }
        private CompletableFuture<HttpResponse<String>> getFuture() { return mFuture; }
    }

    /** JSON metadata stored beside each durable queued audio file. */
    private static class QueuedCall
    {
        private String mId;
        private String mAudioFile;
        private String mAudioName;
        private long mStartTime;
        private long mRecordingLength;
        private String mTalkgroup;
        private String mRadioId;
        private String mTalkerAlias;
        private Long mFrequency;
        private String mPatches;
        private String mTalkgroupLabel;
        private String mTalkgroupGroup;
        private String mSystemLabel;
        private int mAttempt;
        private long mNextAttempt;

        private QueuedCall()
        {
            //Gson
        }

        private QueuedCall(String id, String audioFile, String audioName, long startTime, long recordingLength,
                           String talkgroup, String radioId, String talkerAlias, Long frequency, String patches,
                           String talkgroupLabel, String talkgroupGroup, String systemLabel, int attempt,
                           long nextAttempt)
        {
            mId = id;
            mAudioFile = audioFile;
            mAudioName = audioName;
            mStartTime = startTime;
            mRecordingLength = recordingLength;
            mTalkgroup = talkgroup;
            mRadioId = radioId;
            mTalkerAlias = talkerAlias;
            mFrequency = frequency;
            mPatches = patches;
            mTalkgroupLabel = talkgroupLabel;
            mTalkgroupGroup = talkgroupGroup;
            mSystemLabel = systemLabel;
            mAttempt = attempt;
            mNextAttempt = nextAttempt;
        }

        private String getId() { return mId; }
        private String getAudioFile() { return mAudioFile; }
        private String getAudioName() { return mAudioName; }
        private long getStartTime() { return mStartTime; }
        private String getTalkgroup() { return mTalkgroup; }
        private String getRadioId() { return mRadioId; }
        private String getTalkerAlias() { return mTalkerAlias; }
        private Long getFrequency() { return mFrequency; }
        private String getPatches() { return mPatches; }
        private String getTalkgroupLabel() { return mTalkgroupLabel; }
        private String getTalkgroupGroup() { return mTalkgroupGroup; }
        private String getSystemLabel() { return mSystemLabel; }
        private int getAttempt() { return mAttempt; }
        private void setAttempt(int attempt) { mAttempt = attempt; }
        private long getNextAttempt() { return mNextAttempt; }
        private void setNextAttempt(long nextAttempt) { mNextAttempt = nextAttempt; }
    }

    public class AudioRecordingProcessor implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                processRecordingQueue();
            }
            catch(Throwable t)
            {
                mLog.error("Unexpected error processing Rdio Scanner queue; processing will continue", t);
                scheduleReconnect(rootMessage(t));
            }
        }
    }

    public static void main(String[] args)
    {
        mLog.debug("Starting ...");

        RdioScannerConfiguration config = new RdioScannerConfiguration();
        config.setHost("https://api.RdioScanner.com/call-upload-dev");
        config.setApiKey("c33aae37-8572-11ea-bd8b-0ecc8ab9ccec");
        config.setSystemID(11);

        String response = testConnection(config);

        if(response == null)
        {
            mLog.debug("Test Successful!");
        }
        else
        {
            if(response.contains("1 Invalid-API-Key"))
            {
                mLog.error("Invalid API Key");
            }
            else if(response.contains("1 API-Key-Access-Denied"))
            {
                mLog.error("System ID not valid for API Key");
            }
            else
            {
                mLog.debug("Response: " + response);
            }
        }

        mLog.debug("Finished!");
    }
}
