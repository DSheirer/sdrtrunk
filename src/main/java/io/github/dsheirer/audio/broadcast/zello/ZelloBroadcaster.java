/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.audio.broadcast.zello;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.AbstractAudioBroadcaster;
import io.github.dsheirer.audio.broadcast.AudioRecording;
import io.github.dsheirer.audio.broadcast.BroadcastEvent;
import io.github.dsheirer.audio.broadcast.BroadcastState;
import io.github.dsheirer.audio.broadcast.IRealTimeAudioBroadcaster;
import io.github.dsheirer.audio.convert.InputAudioFormat;
import io.github.dsheirer.audio.convert.MP3Setting;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.util.ThreadPool;
import io.github.jaredmdobson.concentus.OpusApplication;
import io.github.jaredmdobson.concentus.OpusEncoder;
import io.github.jaredmdobson.concentus.OpusSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real-time audio broadcaster for Zello Work channels via WebSocket.
 *
 * Implements IRealTimeAudioBroadcaster to receive 8kHz mono float audio buffers
 * in real-time. Audio is resampled to 16kHz, Opus-encoded, and streamed via
 * the Zello Channel API WebSocket protocol.
 *
 * Each audio segment maps to one Zello push-to-talk voice message:
 * - startRealTimeStream() -> start_stream command
 * - receiveRealTimeAudio() -> accumulate, resample, Opus encode, send packets
 * - stopRealTimeStream() -> stop_stream command
 */
public class ZelloBroadcaster extends AbstractAudioBroadcaster<ZelloConfiguration>
    implements IRealTimeAudioBroadcaster
{
    private static final Logger mLog = LoggerFactory.getLogger(ZelloBroadcaster.class);

    private static final int ZELLO_SAMPLE_RATE = 16000;
    private static final int ZELLO_CHANNELS = 1;
    private static final int ZELLO_FRAME_SIZE_MS = 60;
    private static final int ZELLO_FRAME_SIZE_SAMPLES = ZELLO_SAMPLE_RATE * ZELLO_FRAME_SIZE_MS / 1000; // 960
    private static final int OPUS_BITRATE = 28000;

    // codec_header: {sample_rate_hz(16LE), frames_per_packet(8), frame_size_ms(8)}
    private static final byte[] CODEC_HEADER = {(byte)0x80, (byte)0x3E, 0x01, 0x3C};
    private static final String CODEC_HEADER_B64 = Base64.getEncoder().encodeToString(CODEC_HEADER);

    private static final long RECONNECT_INTERVAL_MS = 15000;
    private static final long KICKED_BACKOFF_MS = 60000;
    private static final int MAX_KICKED_RETRIES = 5;

    /** Client-side keepalive interval — sends a keepalive command to detect dead connections */
    private static final long KEEPALIVE_INTERVAL_MS = 30000;
    /** Consecutive missed keepalive acks before declaring the connection dead */
    private static final int KEEPALIVE_MISSED_ACK_THRESHOLD = 3;

    /** Default minimum gap (ms) between stop_stream and next start_stream. */
    private static final long DEFAULT_STREAM_GUARD_MS = 500;

    /**
     * Number of consecutive "ghost" streams (where start_stream was sent but the server
     * never responded with a stream_id) before forcing a WebSocket reconnect. This detects
     * sessions that appear connected but are silently ignoring stream requests.
     */
    private static final int MAX_GHOST_STREAMS_BEFORE_RECONNECT = 3;

    /** Maximum time (ms) to wait for on_channel_status after WebSocket connects before retrying */
    private static final long CONNECTION_TIMEOUT_MS = 45000;

    private final HttpClient mHttpClient;
    private final Gson mGson = new Gson();
    private final AliasModel mAliasModel;

    private WebSocket mWebSocket;
    private final AtomicBoolean mConnected = new AtomicBoolean(false);
    private final AtomicBoolean mChannelOnline = new AtomicBoolean(false);
    private final AtomicBoolean mKicked = new AtomicBoolean(false);
    private final AtomicBoolean mReconnecting = new AtomicBoolean(false);
    private final AtomicBoolean mStopped = new AtomicBoolean(false);
    private final AtomicInteger mSequence = new AtomicInteger(1);
    private final AtomicInteger mKickedCount = new AtomicInteger(0);
    private ScheduledFuture<?> mReconnectFuture;
    private ScheduledFuture<?> mKeepaliveFuture;
    private ScheduledFuture<?> mConnectionTimeoutFuture;
    private volatile boolean mKeepaliveAwaitingAck = false;
    private volatile int mKeepaliveMissedAcks = 0;

    /**
     * Session epoch — increments on every WebSocket reconnect. Stream operations
     * capture the epoch at start and abort if the epoch changes (meaning the
     * underlying WebSocket was replaced). This prevents sending start_stream or
     * audio packets on a new connection using stale session state.
     */
    private final AtomicInteger mSessionEpoch = new AtomicInteger(0);

    /**
     * Maps seq numbers to the command that sent them, so error responses can be
     * correlated with the originating command (e.g. "start_stream" or "stop_stream").
     * Entries are removed once the response is processed.
     */
    private final ConcurrentHashMap<Integer, String> mPendingCommands = new ConcurrentHashMap<>();

    private final AtomicBoolean mStreamActive = new AtomicBoolean(false);
    private final AtomicLong mCurrentStreamId = new AtomicLong(-1);
    private volatile long mLastStreamStopTime = 0; // System.currentTimeMillis() when last stream ended
    private volatile int mStreamSessionEpoch = -1; // epoch captured when stream started
    private final LinkedTransferQueue<float[]> mAudioQueue = new LinkedTransferQueue<>();
    private ScheduledFuture<?> mEncoderFuture;
    private ScheduledFuture<?> mRelaxationFuture; // delayed stop for relaxation_time hold-over
    private volatile long mLastAudioReceivedTime = 0;
    /** Counts consecutive streams where the server never returned a stream_id */
    private volatile int mConsecutiveGhostStreams = 0;

    private OpusEncoder mOpusEncoder;
    private short[] mResampleBuffer = new short[ZELLO_FRAME_SIZE_SAMPLES];
    private int mResampleBufferPos = 0;
    private byte[] mOpusOutputBuffer = new byte[1275];
    private final AtomicInteger mStreamedCount = new AtomicInteger(0);
    private short mPreviousSample = 0;

    public ZelloBroadcaster(ZelloConfiguration configuration, InputAudioFormat inputAudioFormat,
                            MP3Setting mp3Setting, AliasModel aliasModel)
    {
        super(configuration);
        mAliasModel = aliasModel;
        mHttpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(15))
            .build();
    }

    /** Returns the configured channel name for log identification */
    private String ch()
    {
        ZelloConfiguration config = getBroadcastConfiguration();
        return config != null && config.getChannel() != null ? "[" + config.getChannel() + "] " : "";
    }

    @Override
    public void start()
    {
        mStopped.set(false);
        setBroadcastState(BroadcastState.CONNECTING);
        try
        {
            initOpusEncoder();
            connectWebSocket();
        }
        catch(Exception e)
        {
            mLog.error("{}Error starting Zello broadcaster", ch(), e);
            setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
            scheduleReconnect();
        }
    }

    @Override
    public void stop()
    {
        mStopped.set(true);
        stopKeepalive();
        if(mRelaxationFuture != null) { mRelaxationFuture.cancel(false); mRelaxationFuture = null; }
        if(mStreamActive.get()) doStopRealTimeStream();
        if(mReconnectFuture != null) { mReconnectFuture.cancel(true); mReconnectFuture = null; }
        mKicked.set(false);
        mKickedCount.set(0);
        mReconnecting.set(false);
        disconnectWebSocket();
        setBroadcastState(BroadcastState.DISCONNECTED);
    }

    @Override
    public void dispose() { stop(); }

    @Override
    public int getAudioQueueSize() { return mAudioQueue.size(); }

    /** Standard recording receive — discarded since we use real-time streaming */
    @Override
    public void receive(AudioRecording audioRecording)
    {
        if(audioRecording != null) audioRecording.removePendingReplay();
    }

    // ========================================================================
    // IRealTimeAudioBroadcaster
    // ========================================================================

    @Override
    public boolean isRealTimeReady()
    {
        return mConnected.get() && mChannelOnline.get() && !mStreamActive.get();
    }

    @Override
    public synchronized void startRealTimeStream(IdentifierCollection identifiers)
    {
        if(!mConnected.get() || !mChannelOnline.get())
        {
            mLog.warn("{}Cannot start Zello stream - not connected", ch());
            return;
        }
        // If a relaxation timer is pending, the stream is still active — cancel the
        // timer and continue the existing stream instead of stopping and restarting.
        if(mRelaxationFuture != null)
        {
            mRelaxationFuture.cancel(false);
            mRelaxationFuture = null;
            if(mStreamActive.get())
            {
                mLog.debug("{}Relaxation hold-over: continuing existing stream", ch());
                return;
            }
        }
        if(mStreamActive.get())
        {
            doStopRealTimeStream();
        }

        // Enforce minimum gap between streams (like Bridge's stream_guard_timeout_ms).
        // On busy channels, the server may not have fully released the previous stream.
        // A value of 0 disables the guard entirely.
        long guardMs = getBroadcastConfiguration().getStreamGuardMs();
        long elapsed = System.currentTimeMillis() - mLastStreamStopTime;
        if(guardMs > 0 && mLastStreamStopTime > 0 && elapsed < guardMs)
        {
            long waitMs = guardMs - elapsed;
            mLog.debug("{}Stream guard: waiting {}ms before starting new stream", ch(), waitMs);
            try { Thread.sleep(waitMs); }
            catch(InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }

        int epoch = mSessionEpoch.get();
        mStreamActive.set(true);
        mStreamSessionEpoch = epoch;
        mCurrentStreamId.set(-1);
        mResampleBufferPos = 0;
        mPreviousSample = 0;
        mAudioQueue.clear();

        sendStartStream();

        if(mEncoderFuture == null || mEncoderFuture.isDone())
        {
            mEncoderFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(
                this::processAudioQueue, 10, 10, TimeUnit.MILLISECONDS);
        }

        mLog.info("{}Zello stream started", ch());
    }

    @Override
    public void receiveRealTimeAudio(float[] audioBuffer)
    {
        if(mStreamActive.get())
        {
            mLastAudioReceivedTime = System.currentTimeMillis();
            mAudioQueue.offer(audioBuffer);
        }
    }

    @Override
    public synchronized void stopRealTimeStream()
    {
        if(!mStreamActive.get()) return;

        // Relaxation time: hold the stream open for a configured period after
        // the last audio, allowing back-to-back transmissions to merge into a
        // single Zello voice message (like Bridge's relaxation_time).
        int relaxMs = getBroadcastConfiguration().getRelaxationTimeMs();
        if(relaxMs > 0)
        {
            // Cancel any previous relaxation timer
            if(mRelaxationFuture != null) mRelaxationFuture.cancel(false);
            mRelaxationFuture = ThreadPool.SCHEDULED.schedule(() ->
            {
                synchronized(this) { doStopRealTimeStream(); }
            }, relaxMs, TimeUnit.MILLISECONDS);
            return;
        }

        doStopRealTimeStream();
    }

    /** Internal stop logic — called directly or after relaxation timer expires. */
    private synchronized void doStopRealTimeStream()
    {
        if(!mStreamActive.get()) return;

        mStreamActive.set(false);

        // Cancel relaxation timer if still pending
        if(mRelaxationFuture != null)
        {
            mRelaxationFuture.cancel(false);
            mRelaxationFuture = null;
        }

        // Cancel the encoder future and wait for it to finish to avoid
        // concurrent access to mResampleBuffer and the Opus encoder
        if(mEncoderFuture != null)
        {
            mEncoderFuture.cancel(false);
            try { Thread.sleep(15); } // Allow in-flight execution to complete
            catch(InterruptedException ignored) { Thread.currentThread().interrupt(); }
            mEncoderFuture = null;
        }

        // Now safe to drain remaining audio and flush
        try
        {
            processAudioQueue();
            if(mResampleBufferPos > 0) flushResampleBuffer();
        }
        catch(Exception e)
        {
            mLog.debug("{}Error flushing audio on stream stop: {}", ch(), e.getMessage());
        }

        long streamId = mCurrentStreamId.get();
        if(streamId > 0)
        {
            sendStopStream(streamId);
            mStreamedCount.incrementAndGet();
            mKickedCount.set(0); // Successful stream proves connection is healthy
            mConsecutiveGhostStreams = 0; // Server responded — session is healthy
            broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_STREAMED_COUNT_CHANGE));
        }
        else if(streamId <= 0 && mConnected.get())
        {
            // "Ghost stream": we sent start_stream but the server never responded
            // with a stream_id. No audio was actually transmitted.
            mConsecutiveGhostStreams++;
            mLog.warn("{}Zello ghost stream detected — server did not return stream_id ({}/{})",
                ch(), mConsecutiveGhostStreams, MAX_GHOST_STREAMS_BEFORE_RECONNECT);

            if(mConsecutiveGhostStreams >= MAX_GHOST_STREAMS_BEFORE_RECONNECT)
            {
                mLog.error("{}Zello session appears dead — {} consecutive ghost streams. Forcing reconnect.",
                    ch(), mConsecutiveGhostStreams);
                mConsecutiveGhostStreams = 0;
                mCurrentStreamId.set(-1);
                mResampleBufferPos = 0;
                mAudioQueue.clear();
                mLastStreamStopTime = System.currentTimeMillis();
                mLog.info("{}Zello stream stopped", ch());
                // Force reconnect — tear down the stale WebSocket and start fresh
                disconnectWebSocket();
                setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
                scheduleReconnect();
                return;
            }
        }

        mCurrentStreamId.set(-1);
        mResampleBufferPos = 0;
        mAudioQueue.clear();

        // Pause time: additional delay after stop_stream before the broadcaster
        // is ready for a new stream (like Bridge's pause_time).
        int pauseMs = getBroadcastConfiguration().getPauseTimeMs();
        if(pauseMs > 0)
        {
            try { Thread.sleep(pauseMs); }
            catch(InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        mLastStreamStopTime = System.currentTimeMillis();
        mLog.info("{}Zello stream stopped", ch());
    }

    // ========================================================================
    // Audio Processing
    // ========================================================================

    private synchronized void processAudioQueue()
    {
        try
        {
            float[] buffer;
            while((buffer = mAudioQueue.poll()) != null)
            {
                processAudioBuffer(buffer);
            }
        }
        catch(Exception | AssertionError e)
        {
            mLog.debug("{}Error processing audio queue (non-fatal): {}", ch(), e.getMessage());
        }
    }

    /** Convert float 8kHz -> short 16kHz (2x upsample with linear interpolation), accumulate, encode when frame full */
    private void processAudioBuffer(float[] audio8k)
    {
        for(int i = 0; i < audio8k.length; i++)
        {
            short currentSample = (short)(audio8k[i] * 32767.0f);

            // 2x upsample with linear interpolation:
            // Insert midpoint between previous and current sample, then current sample
            short midpoint = (short)((mPreviousSample + currentSample) / 2);

            if(mResampleBufferPos < ZELLO_FRAME_SIZE_SAMPLES)
            {
                mResampleBuffer[mResampleBufferPos++] = midpoint;
            }
            if(mResampleBufferPos >= ZELLO_FRAME_SIZE_SAMPLES)
            {
                encodeAndSendFrame();
                mResampleBufferPos = 0;
            }

            if(mResampleBufferPos < ZELLO_FRAME_SIZE_SAMPLES)
            {
                mResampleBuffer[mResampleBufferPos++] = currentSample;
            }
            if(mResampleBufferPos >= ZELLO_FRAME_SIZE_SAMPLES)
            {
                encodeAndSendFrame();
                mResampleBufferPos = 0;
            }

            mPreviousSample = currentSample;
        }
    }

    private void encodeAndSendFrame()
    {
        long streamId = mCurrentStreamId.get();
        if(streamId <= 0 || mOpusEncoder == null) return;

        // Reject sends if the WebSocket session has changed since this stream started
        if(mStreamSessionEpoch != mSessionEpoch.get())
        {
            mLog.debug("{}Dropping audio frame — session epoch changed (stream={}, current={})",
                ch(), mStreamSessionEpoch, mSessionEpoch.get());
            mStreamActive.set(false);
            return;
        }

        try
        {
            int encoded = mOpusEncoder.encode(mResampleBuffer, 0, ZELLO_FRAME_SIZE_SAMPLES,
                mOpusOutputBuffer, 0, mOpusOutputBuffer.length);

            if(encoded > 0)
            {
                byte[] opusFrame = new byte[encoded];
                System.arraycopy(mOpusOutputBuffer, 0, opusFrame, 0, encoded);
                sendAudioPacket(streamId, opusFrame);
            }
        }
        catch(Exception | AssertionError e)
        {
            mLog.debug("{}Opus encoding error (non-fatal): {}", ch(), e.getMessage());

            // Re-initialize the encoder to prevent cascading failures on subsequent frames
            try
            {
                initOpusEncoder();
                mLog.debug("{}Opus encoder re-initialized after error", ch());
            }
            catch(Exception reinitEx)
            {
                mLog.warn("{}Failed to re-initialize Opus encoder: {}", ch(), reinitEx.getMessage());
                mOpusEncoder = null;
            }
        }
    }

    private void flushResampleBuffer()
    {
        try
        {
            if(mResampleBufferPos <= 0 || mResampleBufferPos > ZELLO_FRAME_SIZE_SAMPLES) {
                mResampleBufferPos = 0;
                return;
            }
            for(int i = mResampleBufferPos; i < ZELLO_FRAME_SIZE_SAMPLES; i++)
                mResampleBuffer[i] = 0;
            encodeAndSendFrame();
        }
        catch(Exception | AssertionError e)
        {
            mLog.debug("{}Opus flush error (non-fatal): {}", ch(), e.getMessage());
        }
        finally
        {
            mResampleBufferPos = 0;
        }
    }

    private void initOpusEncoder() throws Exception
    {
        mOpusEncoder = new OpusEncoder(ZELLO_SAMPLE_RATE, ZELLO_CHANNELS, OpusApplication.OPUS_APPLICATION_VOIP);
        mOpusEncoder.setBitrate(OPUS_BITRATE);
        mOpusEncoder.setSignalType(OpusSignal.OPUS_SIGNAL_VOICE);
        mOpusEncoder.setComplexity(8);
        mLog.debug("{}Opus encoder initialized: {}Hz, {}ch, {}kbps, {}ms frames",
            ch(), ZELLO_SAMPLE_RATE, ZELLO_CHANNELS, OPUS_BITRATE / 1000, ZELLO_FRAME_SIZE_MS);
    }

    // ========================================================================
    // WebSocket
    // ========================================================================

    private void connectWebSocket()
    {
        if(!mReconnecting.compareAndSet(false, true))
        {
            return; // Another reconnect is already in progress
        }

        // Don't reconnect if we've been stopped
        if(mStopped.get())
        {
            mReconnecting.set(false);
            return;
        }

        // Clean up any existing connection — send a proper close frame so the
        // server releases session state (abort() skips the close handshake which
        // can leave a stale session on Zello's side, causing auth failures on the
        // next logon attempt with the same credentials).
        if(mWebSocket != null)
        {
            try { mWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "reconnecting"); }
            catch(Exception e) { /* ignore — connection may already be dead */ }
            mWebSocket = null;
        }
        mConnected.set(false);
        mChannelOnline.set(false);
        mPendingCommands.clear();
        mConsecutiveGhostStreams = 0;

        String wsUrl = getBroadcastConfiguration().getWebSocketUrl();
        if(wsUrl == null)
        {
            mLog.error("Zello WebSocket URL is null");
            setBroadcastState(BroadcastState.CONFIGURATION_ERROR);
            mReconnecting.set(false);
            return;
        }

        mLog.debug("{}Connecting to Zello Work: {}", ch(), wsUrl);
        try
        {
            mHttpClient.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new ZelloWebSocketListener())
                .thenAccept(ws -> {
                    mWebSocket = ws;
                    mSessionEpoch.incrementAndGet();
                    mReconnecting.set(false);
                    setLastErrorDetail(null);
                    sendLogon();
                    startConnectionTimeout();
                })
                .exceptionally(ex -> {
                    mLog.error("{}WebSocket connection failed: {}", ch(), ex.getMessage());
                    setLastErrorDetail("WebSocket handshake failed");
                    setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
                    mReconnecting.set(false);
                    scheduleReconnect();
                    return null;
                });
        }
        catch(Exception e)
        {
            mLog.error("Error creating WebSocket connection", e);
            setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
            mReconnecting.set(false);
            scheduleReconnect();
        }
    }

    private void disconnectWebSocket()
    {
        mConnected.set(false);
        mChannelOnline.set(false);
        cancelConnectionTimeout();
        if(mWebSocket != null)
        {
            try { mWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Shutting down"); }
            catch(Exception e) { /* ignore */ }
            mWebSocket = null;
        }
    }

    /**
     * Starts a timer that fires if the server hasn't sent on_channel_status: online
     * within CONNECTION_TIMEOUT_MS after logon. If it fires, we close the dead
     * connection and retry.
     */
    private void startConnectionTimeout()
    {
        cancelConnectionTimeout();
        final int epoch = mSessionEpoch.get();
        mConnectionTimeoutFuture = ThreadPool.SCHEDULED.schedule(() -> {
            if(epoch == mSessionEpoch.get() && !mChannelOnline.get() && !mStopped.get())
            {
                mLog.warn("{}Zello connection timeout — no channel status after {}s. Forcing reconnect.",
                    ch(), CONNECTION_TIMEOUT_MS / 1000);
                setLastErrorDetail("Connection timeout (" + CONNECTION_TIMEOUT_MS / 1000 + "s)");
                disconnectWebSocket();
                setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
                scheduleReconnect();
            }
        }, CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    private void cancelConnectionTimeout()
    {
        if(mConnectionTimeoutFuture != null && !mConnectionTimeoutFuture.isDone())
        {
            mConnectionTimeoutFuture.cancel(false);
        }
        mConnectionTimeoutFuture = null;
    }

    private void scheduleReconnect()
    {
        // Don't reconnect if we've been stopped
        if(mStopped.get())
        {
            return;
        }

        if(mKicked.get())
        {
            int kickCount = mKickedCount.get();
            if(kickCount >= MAX_KICKED_RETRIES)
            {
                mLog.error("{}Zello kicked {} times - stopping reconnect attempts. Check channel permissions.", ch(), kickCount);
                setBroadcastState(BroadcastState.CONFIGURATION_ERROR);
                return;
            }
            long backoff = KICKED_BACKOFF_MS * (1L << Math.min(kickCount, 4)); // exponential: 60s, 120s, 240s...
            mLog.warn("{}Zello kicked - backing off {}s ({}/{})", ch(), backoff / 1000, kickCount + 1, MAX_KICKED_RETRIES);
            scheduleReconnectWithDelay(backoff);
        }
        else
        {
            scheduleReconnectWithDelay(RECONNECT_INTERVAL_MS);
        }
    }

    private void scheduleReconnectWithDelay(long delayMs)
    {
        // Cancel any existing reconnect to prevent overlapping timers
        if(mReconnectFuture != null && !mReconnectFuture.isDone())
        {
            return; // A reconnect is already pending
        }

        mReconnectFuture = ThreadPool.SCHEDULED.schedule(() -> {
            if(!mConnected.get() && !mStopped.get())
            {
                mLog.debug("{}Zello reconnecting...", ch());
                connectWebSocket();
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    // ========================================================================
    // Client-Side Keepalive
    // ========================================================================

    /**
     * Starts the client-side keepalive timer. Sends a keepalive command every
     * {@link #KEEPALIVE_INTERVAL_MS} to proactively detect dead connections
     * (e.g. silent NAT timeout, network change without TCP RST). If the server
     * fails to ack {@link #KEEPALIVE_MISSED_ACK_THRESHOLD} consecutive keepalives,
     * the connection is declared dead and reconnection is triggered.
     *
     * This mirrors the approach used by the official Zello JS SDK.
     */
    private void startKeepalive()
    {
        stopKeepalive();
        mKeepaliveAwaitingAck = false;
        mKeepaliveMissedAcks = 0;
        mKeepaliveFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(
            this::keepaliveTick, KEEPALIVE_INTERVAL_MS, KEEPALIVE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void stopKeepalive()
    {
        if(mKeepaliveFuture != null)
        {
            mKeepaliveFuture.cancel(false);
            mKeepaliveFuture = null;
        }
    }

    private void keepaliveTick()
    {
        // CRITICAL: wrap entire body in try-catch. ScheduledExecutorService.scheduleAtFixedRate
        // silently kills the recurring task if the Runnable throws ANY uncaught exception —
        // no more ticks, no error logged, the keepalive just stops. If that happens, the
        // connection can sit dead forever with no detection and no reconnect.
        try
        {
            if(mWebSocket == null || !mConnected.get())
            {
                return;
            }

            if(mKeepaliveAwaitingAck)
            {
                mKeepaliveMissedAcks++;
                mLog.debug("{}Keepalive ack missed ({}/{})", ch(), mKeepaliveMissedAcks, KEEPALIVE_MISSED_ACK_THRESHOLD);
            }

            if(mKeepaliveMissedAcks >= KEEPALIVE_MISSED_ACK_THRESHOLD)
            {
                mLog.warn("{}Keepalive timeout — {} consecutive missed acks, reconnecting", ch(), mKeepaliveMissedAcks);
                stopKeepalive();
                mConnected.set(false);
                mChannelOnline.set(false);
                mStreamActive.set(false);
                mCurrentStreamId.set(-1);
                // Abort the dead WebSocket so connectWebSocket() starts fresh
                if(mWebSocket != null)
                {
                    try { mWebSocket.abort(); } catch(Exception e) { /* ignore */ }
                    mWebSocket = null;
                }
                setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
                setLastErrorDetail("Keepalive timeout — connection dead");
                scheduleReconnect();
                return;
            }

            // Send keepalive command
            mKeepaliveAwaitingAck = true;
            JsonObject cmd = new JsonObject();
            cmd.addProperty("command", "keepalive");
            int seq = mSequence.getAndIncrement();
            cmd.addProperty("seq", seq);
            mPendingCommands.put(seq, "keepalive");
            mWebSocket.sendText(mGson.toJson(cmd), true);
        }
        catch(Exception e)
        {
            mLog.warn("{}Keepalive tick failed (non-fatal): {}", ch(), e.getMessage());
            mKeepaliveMissedAcks++;
        }
    }

    /**
     * Called when a keepalive ack is received from the server. Resets the
     * missed-ack counter so the connection is considered healthy.
     */
    private void handleKeepaliveAck()
    {
        mKeepaliveAwaitingAck = false;
        mKeepaliveMissedAcks = 0;
    }

    // ========================================================================
    // Zello Protocol
    // ========================================================================

    private void sendLogon()
    {
        if(mWebSocket == null) return;
        ZelloConfiguration config = getBroadcastConfiguration();
        JsonObject logon = new JsonObject();
        logon.addProperty("command", "logon");
        int seq = mSequence.getAndIncrement();
        logon.addProperty("seq", seq);
        mPendingCommands.put(seq, "logon");
        com.google.gson.JsonArray channels = new com.google.gson.JsonArray();
        channels.add(config.getChannel());
        logon.add("channels", channels);
        logon.addProperty("username", config.getUsername());
        logon.addProperty("password", config.getPassword());
        logon.addProperty("platform_name", "Gateway");
        mWebSocket.sendText(mGson.toJson(logon), true);
    }

    private void sendStartStream()
    {
        if(mWebSocket == null) return;
        // Don't send start_stream if the session has already changed
        if(mStreamSessionEpoch != mSessionEpoch.get())
        {
            mLog.warn("{}Aborting start_stream — session epoch changed during setup", ch());
            mStreamActive.set(false);
            return;
        }
        ZelloConfiguration config = getBroadcastConfiguration();
        JsonObject cmd = new JsonObject();
        cmd.addProperty("command", "start_stream");
        int seq = mSequence.getAndIncrement();
        cmd.addProperty("seq", seq);
        mPendingCommands.put(seq, "start_stream");
        cmd.addProperty("channel", config.getChannel());
        cmd.addProperty("type", "audio");
        cmd.addProperty("codec", "opus");
        cmd.addProperty("codec_header", CODEC_HEADER_B64);
        cmd.addProperty("packet_duration", ZELLO_FRAME_SIZE_MS);
        mWebSocket.sendText(mGson.toJson(cmd), true);
    }

    private void sendStopStream(long streamId)
    {
        if(mWebSocket == null) return;
        ZelloConfiguration config = getBroadcastConfiguration();
        JsonObject cmd = new JsonObject();
        cmd.addProperty("command", "stop_stream");
        int seq = mSequence.getAndIncrement();
        cmd.addProperty("seq", seq);
        mPendingCommands.put(seq, "stop_stream(id=" + streamId + ")");
        cmd.addProperty("stream_id", streamId);
        cmd.addProperty("channel", config.getChannel());
        mWebSocket.sendText(mGson.toJson(cmd), true);
    }

    private void sendAudioPacket(long streamId, byte[] opusData)
    {
        if(mWebSocket == null) return;
        ByteBuffer packet = ByteBuffer.allocate(1 + 4 + 4 + opusData.length);
        packet.order(ByteOrder.BIG_ENDIAN);
        packet.put((byte)0x01);
        packet.putInt((int)streamId);
        packet.putInt(0);
        packet.put(opusData);
        packet.flip();
        mWebSocket.sendBinary(packet, true);
    }

    /**
     * Maps Zello Channel API error strings to Zello Bridge error codes (3001-3009)
     * for consistent diagnostics. See Zello Bridge documentation.
     */
    private static int mapBridgeErrorCode(String error)
    {
        if(error == null) return 3008;
        switch(error)
        {
            case "not connected":           return 3001;
            case "invalid credentials":     return 3002;
            case "not authorized":          return 3002;
            case "channel is not ready":    return 3003;
            case "failed to start stream":  return 3006;
            case "invalid stream id":       return 3007;
            case "failed to stop stream":   return 3007;
            case "kicked":                  return 3009;
            default:                        return 3008; // generic "error received"
        }
    }

    // ========================================================================
    // WebSocket Listener
    // ========================================================================

    private class ZelloWebSocketListener implements WebSocket.Listener
    {
        private StringBuilder mTextBuffer = new StringBuilder();

        @Override public void onOpen(WebSocket ws) { mLog.debug("{}WebSocket opened", ch()); ws.request(1); }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last)
        {
            mTextBuffer.append(data);
            if(last) { handleTextMessage(mTextBuffer.toString()); mTextBuffer.setLength(0); }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last)
        {
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onPing(WebSocket ws, ByteBuffer msg)
        {
            ws.sendPong(msg);
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int code, String reason)
        {
            mLog.info("{}Zello disconnected (code={} {})", ch(), code, reason);
            stopKeepalive();
            mConnected.set(false);
            mChannelOnline.set(false);
            // Always reset stream state on disconnect — prevents stale stream IDs
            // from surviving into the next session regardless of mStreamActive state
            mStreamActive.set(false);
            mCurrentStreamId.set(-1);

            // If kicked error already handled the reconnect, don't double-schedule
            if(mKicked.get())
            {
                return null;
            }

            // If an auth error already set CONFIGURATION_ERROR, don't override it
            // and don't schedule a reconnect — the credentials won't change by retrying
            if(getBroadcastState() == BroadcastState.CONFIGURATION_ERROR)
            {
                return null;
            }

            setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
            scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error)
        {
            mLog.error("{}Zello WebSocket error: {}", ch(), error.getMessage());
            stopKeepalive();
            mConnected.set(false);
            mChannelOnline.set(false);
            // Reset stream state on error — same as onClose
            mStreamActive.set(false);
            mCurrentStreamId.set(-1);

            // Don't override CONFIGURATION_ERROR or double-schedule after kicked
            if(!mKicked.get() && getBroadcastState() != BroadcastState.CONFIGURATION_ERROR)
            {
                setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
                scheduleReconnect();
            }
        }

        private void handleTextMessage(String message)
        {
            try
            {
                JsonObject json = JsonParser.parseString(message).getAsJsonObject();

                if(json.has("refresh_token") ||
                    (json.has("success") && json.get("success").getAsBoolean() && !json.has("stream_id")))
                {
                    if(!mConnected.get())
                    {
                        mLog.debug("{}Zello logon accepted", ch());
                        mConnected.set(true);
                        // Reset kicked flag so next kick can be detected, but keep the
                        // kickedCount so exponential backoff continues to escalate.
                        // Count only resets when a stream succeeds or on manual stop/restart.
                        mKicked.set(false);
                    }
                    // else: refresh_token while already connected — ignore silently
                }
                else if(json.has("error") && !json.has("command"))
                {
                    String errorMsg = json.get("error").getAsString();
                    int seq = json.has("seq") ? json.get("seq").getAsInt() : -1;
                    String originCmd = seq > 0 ? mPendingCommands.remove(seq) : null;
                    int bridgeCode = mapBridgeErrorCode(errorMsg);

                    // Stream-related errors (3006/3007): the Zello server expired or
                    // closed the stream, another user interrupted our transmission, or
                    // the server refused a brand-new stream attempt. These are all
                    // transient — clean up, stay CONNECTED, and allow the next transmission.
                    if("invalid stream id".equals(errorMsg)
                        || "failed to stop stream".equals(errorMsg)
                        || "failed to start stream".equals(errorMsg)
                        || "failed to start sending message".equals(errorMsg)
                        || "failed to stop sending message".equals(errorMsg))
                    {
                        mLog.debug("{}Zello [{}]: error=\"{}\" seq={} command={}",
                            ch(), bridgeCode, errorMsg, seq, originCmd != null ? originCmd : "unknown");
                        setLastErrorDetail("[" + bridgeCode + "] " + errorMsg +
                            (originCmd != null ? " — " + originCmd : ""));
                        mStreamActive.set(false);
                        mCurrentStreamId.set(-1);
                        mLastStreamStopTime = System.currentTimeMillis();
                        // Stay in CONNECTED state — the WebSocket session is still alive
                        return;
                    }

                    // Actual logon/authentication error (e.g. "invalid credentials")
                    mLog.error("{}Zello [{}]: error=\"{}\" seq={} command={}",
                        ch(), bridgeCode, errorMsg, seq, originCmd != null ? originCmd : "unknown");
                    setLastErrorDetail("[" + bridgeCode + "] " + errorMsg);
                    setBroadcastState(BroadcastState.CONFIGURATION_ERROR);
                    return;
                }

                if(json.has("command"))
                {
                    String command = json.get("command").getAsString();
                    if("on_channel_status".equals(command))
                    {
                        String status = json.has("status") ? json.get("status").getAsString() : "";
                        if("online".equals(status))
                        {
                            // getAndSet(true) returns the old value; only log/set state on first transition
                            if(!mChannelOnline.getAndSet(true))
                            {
                                cancelConnectionTimeout();
                                setBroadcastState(BroadcastState.CONNECTED);
                                startKeepalive();
                                mLog.info("{}Zello connected", ch());
                            }
                        }
                        else
                        {
                            // Channel went offline (e.g. server-side channel disabled, network partition).
                            // If we were previously online, this is a state change that needs recovery.
                            if(mChannelOnline.getAndSet(false))
                            {
                                mLog.warn("{}Zello channel went offline (status={}), reconnecting", ch(), status);
                                stopKeepalive();
                                mConnected.set(false);
                                mStreamActive.set(false);
                                mCurrentStreamId.set(-1);
                                if(mWebSocket != null)
                                {
                                    try { mWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "channel offline"); }
                                    catch(Exception e) { /* ignore */ }
                                    mWebSocket = null;
                                }
                                setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
                                setLastErrorDetail("Channel offline (status=" + status + ")");
                                scheduleReconnect();
                            }
                        }
                    }
                    else if("on_stream_stop".equals(command))
                    {
                        // Server-initiated stream termination. This happens when the server
                        // closes our outgoing stream (e.g. server-side timeout, audio gap,
                        // or channel policy). Proactively clean up so the subsequent
                        // stopRealTimeStream() call doesn't send a stale stop_stream.
                        long stoppedId = json.has("stream_id") ? json.get("stream_id").getAsLong() : -1;
                        if(stoppedId > 0 && stoppedId == mCurrentStreamId.get())
                        {
                            mLog.info("{}Zello server stopped our stream (id={})", ch(), stoppedId);
                            setLastErrorDetail("[3007] server stopped stream (id=" + stoppedId + ")");
                            mStreamActive.set(false);
                            mCurrentStreamId.set(-1);
                            mLastStreamStopTime = System.currentTimeMillis();
                        }
                        else
                        {
                            mLog.debug("{}Zello on_stream_stop for stream_id={} (not ours: {})",
                                ch(), stoppedId, mCurrentStreamId.get());
                        }
                    }
                    else if("on_error".equals(command))
                    {
                        String error = json.has("error") ? json.get("error").getAsString() : "";
                        mLog.error("{}Zello [{}]: {}", ch(), mapBridgeErrorCode(error), message);

                        if("kicked".equals(error))
                        {
                            setLastErrorDetail("[3009] kicked");
                            mKicked.set(true);
                            mKickedCount.incrementAndGet();
                            mConnected.set(false);
                            mChannelOnline.set(false);
                            // Close the WebSocket ourselves to prevent onClose from also scheduling
                            if(mWebSocket != null)
                            {
                                try { mWebSocket.abort(); } catch(Exception e) { /* ignore */ }
                                mWebSocket = null;
                            }
                            scheduleReconnect();
                            return; // Don't process further messages on this connection
                        }
                    }
                }

                // Clean up pending command tracking on any successful response with seq
                if(json.has("seq") && json.has("success") && json.get("success").getAsBoolean())
                {
                    int ackSeq = json.get("seq").getAsInt();
                    String ackCmd = mPendingCommands.remove(ackSeq);
                    // Handle keepalive ack — reset missed-ack counter
                    if("keepalive".equals(ackCmd))
                    {
                        handleKeepaliveAck();
                    }
                }

                if(json.has("stream_id") && json.has("success"))
                {
                    if(json.get("success").getAsBoolean())
                    {
                        long streamId = json.get("stream_id").getAsLong();
                        mCurrentStreamId.set(streamId);
                        mConsecutiveGhostStreams = 0; // Server is responding — session is healthy
                        setLastErrorDetail(null);
                        mLog.debug("{}Zello stream_id={}", ch(), streamId);
                    }
                    else
                    {
                        int seq = json.has("seq") ? json.get("seq").getAsInt() : -1;
                        String originCmd = seq > 0 ? mPendingCommands.remove(seq) : null;
                        String error = json.has("error") ? json.get("error").getAsString() : "unknown";
                        mLog.error("{}Zello start_stream failed: error=\"{}\" seq={} command={}",
                            ch(), error, seq, originCmd != null ? originCmd : "start_stream");
                        setLastErrorDetail("[3006] " + error);
                        mCurrentStreamId.set(-2);
                        mStreamActive.set(false);
                    }
                }
            }
            catch(Exception e)
            {
                mLog.error("Error parsing Zello message: {}", message, e);
            }
        }
    }
}
