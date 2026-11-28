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

package io.github.dsheirer.web.live;

import java.util.function.Consumer;

/**
 * Where a connection's messages go: the WebSocket session, or a test double.
 */
interface MessageSink
{
    /**
     * Sends a text message asynchronously.  Exactly one of the callbacks runs when the send completes.
     */
    void send(String text, Runnable succeeded, Consumer<Throwable> failed);

    /**
     * Sends a binary message asynchronously.  Exactly one of the callbacks runs when the send completes.
     */
    void sendBinary(byte[] data, Runnable succeeded, Consumer<Throwable> failed);

    /**
     * Closes the connection with a WebSocket close code.
     */
    void close(int code, String reason);
}
