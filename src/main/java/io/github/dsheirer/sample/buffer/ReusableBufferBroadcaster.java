/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.sample.buffer;

import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReusableBufferBroadcaster<T extends ReusableBuffer> extends Broadcaster<T>
{
    private final static Logger mLog = LoggerFactory.getLogger(ReusableBufferBroadcaster.class);

    /**
     * Increments the user count for the reusable complex buffer and then broadcasts the buffer to all registered
     * listeners.
     *
     * The total user count is established and applied to the buffer prior to dispatching.  If we were to simply
     * increment the user count prior to sending to each consumer, there is a possibility that the consumer could
     * immediately decrement the user count and prematurely signal that the buffer is ready for disposal before we
     * send the buffer to all consumers.
     */
    @Override
    public void broadcast(T reusableBuffer)
    {
        for(Listener<T> listener : mListeners)
        {
            reusableBuffer.incrementUserCount();
            listener.receive(reusableBuffer);
        }

        //Decrement user counter for this broadcaster
        reusableBuffer.decrementUserCount();
    }
}
