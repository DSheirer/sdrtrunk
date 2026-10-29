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

package io.github.dsheirer.audio.broadcast.thinline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Consumer;

/**
 * One participant on the shared site-key websocket.
 */
class ThinlineRadioEndpoint
{
    private final Consumer<JsonNode> mCommandListener;
    private final Runnable mReadyListener;
    private final Runnable mClosedListener;
    private ThinlineRadioSocket mSocket;

    ThinlineRadioEndpoint(Consumer<JsonNode> commandListener, Runnable readyListener)
    {
        this(commandListener, readyListener, null);
    }

    ThinlineRadioEndpoint(Consumer<JsonNode> commandListener, Runnable readyListener, Runnable closedListener)
    {
        mCommandListener = commandListener;
        mReadyListener = readyListener;
        mClosedListener = closedListener;
    }

    void ensure(String url, String apiKey)
    {
        if(url == null || url.isBlank() || apiKey == null || apiKey.isBlank())
        {
            close();
            return;
        }

        ThinlineRadioSocket next = ThinlineRadioSocket.share(url, apiKey);
        if(next == mSocket)
        {
            return;
        }

        close();
        mSocket = next;
        if(mCommandListener != null)
        {
            mSocket.addCommandListener(mCommandListener);
        }
        if(mReadyListener != null)
        {
            mSocket.addReadyListener(mReadyListener);
        }
        if(mClosedListener != null)
        {
            mSocket.addClosedListener(mClosedListener);
        }
    }

    void close()
    {
        ThinlineRadioSocket socket = mSocket;
        mSocket = null;
        if(socket == null)
        {
            return;
        }
        if(mCommandListener != null)
        {
            socket.removeCommandListener(mCommandListener);
        }
        if(mReadyListener != null)
        {
            socket.removeReadyListener(mReadyListener);
        }
        if(mClosedListener != null)
        {
            socket.removeClosedListener(mClosedListener);
        }
    }

    boolean isReady()
    {
        return mSocket != null && mSocket.isReady();
    }

    boolean send(ObjectNode body)
    {
        return mSocket != null && mSocket.send(body);
    }

    boolean trySend(ObjectNode body)
    {
        return mSocket != null && mSocket.trySend(body);
    }
}
