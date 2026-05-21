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
package io.github.dsheirer.module.decode.p25.phase1;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton TCP server that broadcasts decoded PCM audio as NDJSON to connected clients.
 *
 * Three message types are emitted:
 *   call_start — once when a new transmission begins (squelch opens)
 *   pcm        — once per decoded audio chunk (float[] samples converted to 16-bit little-endian PCM)
 *   call_end   — once when a transmission ends (squelch closes)
 *
 * Audio format: 16-bit signed little-endian PCM at 8000 Hz mono, Base64-encoded.
 * No JMBE library is required on the client — the audio has already been decoded by SDRTrunk.
 *
 * Multiple clients can connect simultaneously; each receives a full copy of the stream.
 * Consumers should demultiplex by the "callId" field, which is unique per transmission.
 */
public class PcmStreamManager
{
    private static final Logger mLog = LoggerFactory.getLogger(PcmStreamManager.class);

    private static PcmStreamManager sInstance;

    private final CopyOnWriteArrayList<ClientWriter> mClients = new CopyOnWriteArrayList<>();
    private volatile boolean mRunning = false;

    private PcmStreamManager() {}

    /**
     * Returns the singleton instance, creating and starting it if necessary.
     * @param port TCP port to listen on (default 9503)
     */
    public static synchronized PcmStreamManager getInstance(int port)
    {
        if (sInstance == null)
        {
            PcmStreamManager mgr = new PcmStreamManager();
            mgr.startAcceptLoop(port);
            sInstance = mgr;
        }
        return sInstance;
    }

    /**
     * Returns the singleton instance if already created, or null if not yet initialized.
     * Used by AbstractAudioModule to null-safely check whether the stream is active.
     */
    public static PcmStreamManager getInstance()
    {
        return sInstance;
    }

    /**
     * Returns true if this manager has been started and is listening for clients.
     */
    public boolean isRunning()
    {
        return mRunning;
    }

    /**
     * Broadcasts a single NDJSON line to all connected clients.
     */
    public void broadcast(String json)
    {
        Iterator<ClientWriter> it = mClients.iterator();
        while (it.hasNext())
        {
            ClientWriter writer = it.next();
            if (!writer.isAlive())
            {
                mClients.remove(writer);
            }
            else
            {
                writer.offer(json);
            }
        }
    }

    /**
     * Broadcasts a call_start message.
     *
     * @param callId    unique identifier for this call
     * @param system    system name
     * @param talkgroup talkgroup identifier
     * @param from      source radio unit identifier
     * @param timestamp human-readable timestamp (yyyy-MM-dd HH:mm:ss)
     */
    public void broadcastCallStart(String callId, String system, String site, String talkgroup, String from, String timestamp)
    {
        String json = "{\"type\":\"call_start\"" +
                ",\"callId\":\"" + escape(callId) + "\"" +
                ",\"system\":\"" + escape(system) + "\"" +
                ",\"site\":\"" + escape(site) + "\"" +
                ",\"talkgroup\":\"" + escape(talkgroup) + "\"" +
                ",\"from\":\"" + escape(from) + "\"" +
                ",\"timestamp\":\"" + escape(timestamp) + "\"}";
        broadcast(json);
    }

    /**
     * Broadcasts a pcm chunk message.
     *
     * Converts the float[] samples to 16-bit signed little-endian PCM and Base64-encodes them.
     * Conversion: (short) Math.max(-32768, Math.min(32767, Math.round(sample * 32767f)))
     *
     * @param callId    unique identifier for this call
     * @param system    system name
     * @param talkgroup talkgroup identifier
     * @param from      source radio unit identifier
     * @param seq       zero-based chunk sequence number within this call
     * @param samples   decoded PCM float samples (8000 Hz mono, range -1.0 to 1.0)
     */
    public void broadcastPcm(String callId, String system, String site, String talkgroup, String from, int seq, float[] samples)
    {
        ByteBuffer buf = ByteBuffer.allocate(samples.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (float sample : samples)
        {
            short s = (short) Math.max(-32768, Math.min(32767, Math.round(sample * 32767f)));
            buf.putShort(s);
        }
        String samplesB64 = Base64.getEncoder().encodeToString(buf.array());

        String json = "{\"type\":\"pcm\"" +
                ",\"callId\":\"" + escape(callId) + "\"" +
                ",\"system\":\"" + escape(system) + "\"" +
                ",\"site\":\"" + escape(site) + "\"" +
                ",\"talkgroup\":\"" + escape(talkgroup) + "\"" +
                ",\"from\":\"" + escape(from) + "\"" +
                ",\"seq\":" + seq +
                ",\"samples\":\"" + samplesB64 + "\"}";
        broadcast(json);
    }

    /**
     * Broadcasts a call_end message.
     *
     * @param callId     unique identifier for this call
     * @param system     system name
     * @param talkgroup  talkgroup identifier
     * @param frameCount total number of pcm chunks sent for this call
     */
    public void broadcastCallEnd(String callId, String system, String site, String talkgroup, int frameCount)
    {
        String json = "{\"type\":\"call_end\"" +
                ",\"callId\":\"" + escape(callId) + "\"" +
                ",\"system\":\"" + escape(system) + "\"" +
                ",\"site\":\"" + escape(site) + "\"" +
                ",\"talkgroup\":\"" + escape(talkgroup) + "\"" +
                ",\"frames\":" + frameCount + "}";
        broadcast(json);
    }

    /**
     * Broadcasts a voice_id message when the first LDU1 Link Control Word is decoded (~180ms after squelch open).
     * This fires BEFORE the audio module releases audio, giving consumers a guaranteed fast SUID notification.
     *
     * @param system    system name from SDRTrunk configuration
     * @param site      site name from SDRTrunk configuration
     * @param talkgroup talkgroup identifier
     * @param from      source radio unit identifier (SUID)
     * @param timestamp human-readable timestamp (yyyy-MM-dd HH:mm:ss)
     */
    public void broadcastVoiceId(String system, String site, String talkgroup, String from, String timestamp)
    {
        String json = "{\"type\":\"voice_id\"" +
                ",\"system\":\"" + escape(system) + "\"" +
                ",\"site\":\"" + escape(site) + "\"" +
                ",\"talkgroup\":\"" + escape(talkgroup) + "\"" +
                ",\"from\":\"" + escape(from) + "\"" +
                ",\"timestamp\":\"" + escape(timestamp) + "\"}";
        broadcast(json);
    }

    private static String escape(String s)
    {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", "");
    }

    private void startAcceptLoop(int port)
    {
        Thread.ofVirtual().start(() ->
        {
            try (ServerSocket serverSocket = new ServerSocket(port))
            {
                mRunning = true;
                mLog.info("PcmStreamManager listening on port {}", port);
                while (true)
                {
                    try
                    {
                        Socket socket = serverSocket.accept();
                        ClientWriter writer = new ClientWriter(socket);
                        mClients.add(writer);
                        mLog.debug("PCM stream client connected on port {}: {}",
                                port, socket.getRemoteSocketAddress());

                        // Monitor for client disconnect
                        Thread.ofVirtual().start(() ->
                        {
                            try
                            {
                                int read = socket.getInputStream().read();
                                if (read == -1)
                                {
                                    writer.close();
                                }
                            }
                            catch (IOException e)
                            {
                                writer.close();
                            }
                        });
                    }
                    catch (IOException e)
                    {
                        mLog.warn("Error accepting PCM stream client on port {}: {}", port, e.getMessage());
                    }
                }
            }
            catch (IOException e)
            {
                mLog.error("Failed to start PCM stream TCP server on port {}: {}", port, e.getMessage());
            }
        });
    }

    /**
     * Wraps a connected client socket with a non-blocking queue-based writer.
     */
    public static class ClientWriter
    {
        private final Socket mSocket;
        private final PrintWriter mWriter;
        private final ArrayBlockingQueue<String> mQueue = new ArrayBlockingQueue<>(1024);
        private volatile boolean mAlive = true;

        public ClientWriter(Socket socket) throws IOException
        {
            mSocket = socket;
            mWriter = new PrintWriter(socket.getOutputStream(), false);

            Thread.ofVirtual().start(() ->
            {
                while (mAlive)
                {
                    try
                    {
                        String line = mQueue.take();
                        mWriter.println(line);
                        mWriter.flush();
                        if (mWriter.checkError())
                        {
                            mAlive = false;
                        }
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                        mAlive = false;
                    }
                }
            });
        }

        public boolean offer(String json)
        {
            return mQueue.offer(json);
        }

        public boolean isAlive()
        {
            return mAlive;
        }

        public void close()
        {
            mAlive = false;
            try
            {
                mSocket.close();
            }
            catch (IOException e)
            {
                // ignore
            }
        }
    }
}
