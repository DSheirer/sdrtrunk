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
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton TCP server that broadcasts raw IMBE voice frames as NDJSON to connected clients.
 *
 * Three message types are emitted:
 *   call_start — once when a new transmission begins (squelch opens)
 *   frame      — once per IMBE frame (~every 20 ms per active voice channel)
 *   call_end   — once when a transmission ends (squelch closes)
 *
 * Multiple clients can connect simultaneously; each receives a full copy of the stream.
 * Consumers should demultiplex by the "callId" field, which is unique per transmission.
 */
public class ImbeStreamManager
{
    private static final Logger mLog = LoggerFactory.getLogger(ImbeStreamManager.class);

    private static ImbeStreamManager sInstance;

    private final CopyOnWriteArrayList<ClientWriter> mClients = new CopyOnWriteArrayList<>();

    private ImbeStreamManager() {}

    /**
     * Returns the singleton instance, creating and starting it if necessary.
     * @param port TCP port to listen on (default 9502)
     */
    public static synchronized ImbeStreamManager getInstance(int port)
    {
        if (sInstance == null)
        {
            ImbeStreamManager mgr = new ImbeStreamManager();
            mgr.startAcceptLoop(port);
            sInstance = mgr;
        }
        return sInstance;
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

    private void startAcceptLoop(int port)
    {
        Thread.ofVirtual().start(() ->
        {
            try (ServerSocket serverSocket = new ServerSocket(port))
            {
                mLog.info("ImbeStreamManager listening on port {}", port);
                while (true)
                {
                    try
                    {
                        Socket socket = serverSocket.accept();
                        ClientWriter writer = new ClientWriter(socket);
                        mClients.add(writer);
                        mLog.debug("IMBE stream client connected on port {}: {}",
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
                        mLog.warn("Error accepting IMBE stream client on port {}: {}", port, e.getMessage());
                    }
                }
            }
            catch (IOException e)
            {
                mLog.error("Failed to start IMBE stream TCP server on port {}: {}", port, e.getMessage());
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
