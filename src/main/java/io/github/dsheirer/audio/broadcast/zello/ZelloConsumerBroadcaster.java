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
public class ZelloConsumerBroadcaster extends AbstractAudioBroadcaster<ZelloConsumerConfiguration>
    implements IRealTimeAudioBroadcaster
{
    private static final Logger mLog = LoggerFactory.getLogger(ZelloConsumerBroadcaster.class);

    private static final int ZELLO_SAMPLE_RATE = 16000;
    private static final int ZELLO_CHANNELS = 1;
    private static final int ZELLO_FRAME_SIZE_MS = 60;
    private static final int ZELLO_FRAME_SIZE_SAMPLES = ZELLO_SAMPLE_RATE * ZELLO_FRAME_SIZE_MS / 1000; // 960
    private static final int OPUS_BITRATE = 16000;

    // codec_header: {sample_rate_hz(16LE), frames_per_packet(8), frame_size_ms(8)}
    private static final byte[] CODEC_HEADER = {(byte)0x80, (byte)0x3E, 0x01, 0x3C};
    private static final String CODEC_HEADER_B64 = Base64.getEncoder().encodeToString(CODEC_HEADER);

    private static final long RECONNECT_INTERVAL_MS = 15000;

    private final HttpClient mHttpClient;
    private final Gson mGson = new Gson();
    private final AliasModel mAliasModel;

    private WebSocket mWebSocket;
    private final AtomicBoolean mConnected = new AtomicBoolean(false);
    private final AtomicBoolean mChannelOnline = new AtomicBoolean(false);
    private final AtomicInteger mSequence = new AtomicInteger(1);
    private ScheduledFuture<?> mReconnectFuture;

    private final AtomicBoolean mStreamActive = new AtomicBoolean(false);
    private final AtomicLong mCurrentStreamId = new AtomicLong(-1);
    private final LinkedTransferQueue<float[]> mAudioQueue = new LinkedTransferQueue<>();
    private ScheduledFuture<?> mEncoderFuture;

    private OpusEncoder mOpusEncoder;
    private short[] mResampleBuffer = new short[ZELLO_FRAME_SIZE_SAMPLES];
    private int mResampleBufferPos = 0;
    private byte[] mOpusOutputBuffer = new byte[1275];
    private int mStreamedCount = 0;

    public ZelloConsumerBroadcaster(ZelloConsumerConfiguration configuration, InputAudioFormat inputAudioFormat,
                            MP3Setting mp3Setting, AliasModel aliasModel)
    {
        super(configuration);
        mAliasModel = aliasModel;
        mHttpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(15))
            .build();
    }

    @Override
    public void start()
    {
        setBroadcastState(BroadcastState.CONNECTING);
        try
        {
            initOpusEncoder();
            connectWebSocket();
        }
        catch(Exception e)
        {
            mLog.error("Error starting Zello broadcaster", e);
            setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
            scheduleReconnect();
        }
    }

    @Override
    public void stop()
    {
        if(mStreamActive.get()) stopRealTimeStream();
        if(mReconnectFuture != null) { mReconnectFuture.cancel(true); mReconnectFuture = null; }
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
    public void startRealTimeStream(IdentifierCollection identifiers)
    {
        if(!mConnected.get() || !mChannelOnline.get())
        {
            mLog.warn("Cannot start Zello stream - not connected");
            return;
        }
        if(mStreamActive.get())
        {
            stopRealTimeStream();
        }

        mStreamActive.set(true);
        mCurrentStreamId.set(-1);
        mResampleBufferPos = 0;
        mAudioQueue.clear();

        sendStartStream();

        if(mEncoderFuture == null || mEncoderFuture.isDone())
        {
            mEncoderFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(
                this::processAudioQueue, 10, 10, TimeUnit.MILLISECONDS);
        }

        mLog.debug("Real-time Zello stream started");
    }

    @Override
    public void receiveRealTimeAudio(float[] audioBuffer)
    {
        if(mStreamActive.get()) mAudioQueue.offer(audioBuffer);
    }

    @Override
    public void stopRealTimeStream()
    {
        if(!mStreamActive.get()) return;

        mStreamActive.set(false);
        processAudioQueue();
        if(mResampleBufferPos > 0) flushResampleBuffer();

        if(mEncoderFuture != null) { mEncoderFuture.cancel(false); mEncoderFuture = null; }

        long streamId = mCurrentStreamId.get();
        if(streamId > 0)
        {
            sendStopStream(streamId);
            mStreamedCount++;
            broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_STREAMED_COUNT_CHANGE));
        }

        mCurrentStreamId.set(-1);
        mAudioQueue.clear();
        mLog.debug("Real-time Zello stream stopped");
    }

    // ========================================================================
    // Audio Processing
    // ========================================================================

    private void processAudioQueue()
    {
        try
        {
            float[] buffer;
            while((buffer = mAudioQueue.poll()) != null)
            {
                processAudioBuffer(buffer);
            }
        }
        catch(Exception e)
        {
            mLog.error("Error processing audio queue", e);
        }
    }

    /** Convert float 8kHz -> short 16kHz (2x upsample), accumulate, encode when frame full */
    private void processAudioBuffer(float[] audio8k)
    {
        for(int i = 0; i < audio8k.length; i++)
        {
            short sample = (short)(audio8k[i] * 32767.0f);

            // 2x upsample: duplicate each sample
            mResampleBuffer[mResampleBufferPos++] = sample;
            if(mResampleBufferPos >= ZELLO_FRAME_SIZE_SAMPLES)
            {
                encodeAndSendFrame();
                mResampleBufferPos = 0;
            }

            mResampleBuffer[mResampleBufferPos++] = sample;
            if(mResampleBufferPos >= ZELLO_FRAME_SIZE_SAMPLES)
            {
                encodeAndSendFrame();
                mResampleBufferPos = 0;
            }
        }
    }

    private void encodeAndSendFrame()
    {
        long streamId = mCurrentStreamId.get();
        if(streamId <= 0) return;

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
        catch(Exception e)
        {
            mLog.error("Opus encoding error", e);
        }
    }

    private void flushResampleBuffer()
    {
        for(int i = mResampleBufferPos; i < ZELLO_FRAME_SIZE_SAMPLES; i++)
            mResampleBuffer[i] = 0;
        encodeAndSendFrame();
        mResampleBufferPos = 0;
    }

    private void initOpusEncoder() throws Exception
    {
        mOpusEncoder = new OpusEncoder(ZELLO_SAMPLE_RATE, ZELLO_CHANNELS, OpusApplication.OPUS_APPLICATION_VOIP);
        mOpusEncoder.setBitrate(OPUS_BITRATE);
        mOpusEncoder.setSignalType(OpusSignal.OPUS_SIGNAL_VOICE);
        mOpusEncoder.setComplexity(5);
        mLog.info("Opus encoder initialized: {}Hz, {}ch, {}kbps, {}ms frames",
            ZELLO_SAMPLE_RATE, ZELLO_CHANNELS, OPUS_BITRATE / 1000, ZELLO_FRAME_SIZE_MS);
    }

    // ========================================================================
    // WebSocket
    // ========================================================================

    private void connectWebSocket()
    {
        String wsUrl = getBroadcastConfiguration().getWebSocketUrl();
        if(wsUrl == null)
        {
            mLog.error("Zello WebSocket URL is null");
            setBroadcastState(BroadcastState.CONFIGURATION_ERROR);
            return;
        }

        mLog.info("Connecting to Zello Work: {}", wsUrl);
        try
        {
            mHttpClient.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new ZelloWebSocketListener())
                .thenAccept(ws -> { mWebSocket = ws; mLog.info("WebSocket connected"); sendLogon(); })
                .exceptionally(ex -> {
                    mLog.error("WebSocket connection failed: {}", ex.getMessage());
                    setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
                    scheduleReconnect();
                    return null;
                });
        }
        catch(Exception e)
        {
            mLog.error("Error creating WebSocket connection", e);
            setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
            scheduleReconnect();
        }
    }

    private void disconnectWebSocket()
    {
        mConnected.set(false);
        mChannelOnline.set(false);
        if(mWebSocket != null)
        {
            try { mWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Shutting down"); }
            catch(Exception e) { /* ignore */ }
            mWebSocket = null;
        }
    }

    private void scheduleReconnect()
    {
        if(mReconnectFuture == null || mReconnectFuture.isDone())
        {
            mReconnectFuture = ThreadPool.SCHEDULED.schedule(() -> {
                if(!mConnected.get()) { mLog.info("Zello reconnecting..."); connectWebSocket(); }
            }, RECONNECT_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }
    }

    // ========================================================================
    // Zello Protocol
    // ========================================================================

    private void sendLogon()
    {
        ZelloConsumerConfiguration config = getBroadcastConfiguration();
        JsonObject logon = new JsonObject();
        logon.addProperty("command", "logon");
        logon.addProperty("seq", mSequence.getAndIncrement());
        com.google.gson.JsonArray channels = new com.google.gson.JsonArray();
        channels.add(config.getChannel());
        logon.add("channels", channels);
        logon.addProperty("username", config.getUsername());
        logon.addProperty("password", config.getPassword());
        String authToken = config.getAuthToken();
        if(authToken != null && !authToken.isEmpty()) logon.addProperty("auth_token", authToken);
        logon.addProperty("listen_only", false);
        mWebSocket.sendText(mGson.toJson(logon), true);
    }

    private void sendStartStream()
    {
        JsonObject cmd = new JsonObject();
        cmd.addProperty("command", "start_stream");
        cmd.addProperty("seq", mSequence.getAndIncrement());
        cmd.addProperty("type", "audio");
        cmd.addProperty("codec", "opus");
        cmd.addProperty("codec_header", CODEC_HEADER_B64);
        cmd.addProperty("packet_duration", ZELLO_FRAME_SIZE_MS);
        mWebSocket.sendText(mGson.toJson(cmd), true);
    }

    private void sendStopStream(long streamId)
    {
        JsonObject cmd = new JsonObject();
        cmd.addProperty("command", "stop_stream");
        cmd.addProperty("seq", mSequence.getAndIncrement());
        cmd.addProperty("stream_id", streamId);
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

    // ========================================================================
    // WebSocket Listener
    // ========================================================================

    private class ZelloWebSocketListener implements WebSocket.Listener
    {
        private StringBuilder mTextBuffer = new StringBuilder();

        @Override public void onOpen(WebSocket ws) { mLog.info("Zello WebSocket opened"); ws.request(1); }

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
            mLog.info("Zello WebSocket closed: {} {}", code, reason);
            mConnected.set(false);
            mChannelOnline.set(false);
            if(mStreamActive.get()) { mStreamActive.set(false); mCurrentStreamId.set(-1); }
            setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
            scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error)
        {
            mLog.error("Zello WebSocket error: {}", error.getMessage());
            mConnected.set(false);
            mChannelOnline.set(false);
            setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
            scheduleReconnect();
        }

        private void handleTextMessage(String message)
        {
            try
            {
                JsonObject json = JsonParser.parseString(message).getAsJsonObject();

                if(json.has("refresh_token") ||
                    (json.has("success") && json.get("success").getAsBoolean() && !json.has("stream_id")))
                {
                    mLog.info("Zello logon successful");
                    mConnected.set(true);
                }
                else if(json.has("error") && !json.has("command"))
                {
                    mLog.error("Zello logon failed: {}", message);
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
                            mChannelOnline.set(true);
                            setBroadcastState(BroadcastState.CONNECTED);
                            mLog.info("Zello channel online: {}",
                                json.has("channel") ? json.get("channel").getAsString() : "");
                        }
                        else
                        {
                            mChannelOnline.set(false);
                        }
                    }
                    else if("on_error".equals(command))
                    {
                        mLog.error("Zello error: {}", message);
                    }
                }

                if(json.has("stream_id") && json.has("success"))
                {
                    if(json.get("success").getAsBoolean())
                    {
                        long streamId = json.get("stream_id").getAsLong();
                        mCurrentStreamId.set(streamId);
                        mLog.debug("Zello stream_id={}", streamId);
                    }
                    else
                    {
                        mLog.error("Zello start_stream failed: {}", message);
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
