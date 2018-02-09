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
package io.github.dsheirer.sample.complex.reusable;

import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReusableBufferBroadcaster extends Broadcaster<ReusableComplexBuffer>
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
    public void broadcast(ReusableComplexBuffer reusableComplexBuffer)
    {
        //There is a minuscule possibility that the listener list will change from the time that we get the size until
        //the for/each iterator is created below, therefore we have to accurately track the user count.  If a listener
        //is added in the time between getting the list size and creating the iterator, that listener will NOT receive
        //a copy of the buffer, but will instead get the next buffer.  Likewise, if a listener is removed, we have to
        //decrement the user count on the buffer after iterating the listener list.
        int listenerCount = mListeners.size();

        reusableComplexBuffer.incrementUserCount(listenerCount);

        for(Listener<ReusableComplexBuffer> listener : mListeners)
        {
            if(listenerCount > 0)
            {
                listener.receive(reusableComplexBuffer);
            }

            listenerCount--;
        }

        while(listenerCount > 0)
        {
            reusableComplexBuffer.decrementUserCount();
            listenerCount--;
        }

        //Decrement user counter for this broadcaster
        reusableComplexBuffer.decrementUserCount();
    }
}
