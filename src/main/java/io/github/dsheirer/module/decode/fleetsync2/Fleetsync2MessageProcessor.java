/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.fleetsync2;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.fleetsync2.message.AutomaticNumberIdentification;
import io.github.dsheirer.module.decode.fleetsync2.message.Fleetsync2Message;
import io.github.dsheirer.module.decode.fleetsync2.message.LocationReport;
import io.github.dsheirer.module.decode.fleetsync2.message.Status;
import io.github.dsheirer.sample.Listener;

/**
 * Fleetsync message processor converts binary messages into IMessage implementations.
 */
public class Fleetsync2MessageProcessor implements Listener<CorrectedBinaryMessage>
{
    private Listener<IMessage> mMessageListener;

    /**
     * Constructs a message processor
     */
    public Fleetsync2MessageProcessor()
    {
    }

    public void dispose()
    {
        mMessageListener = null;
    }

    /**
     * Primary receive interface for demodulated binary messages
     */
    @Override
    public void receive(CorrectedBinaryMessage message)
    {
        FleetsyncMessageType messageType = Fleetsync2Message.getMessageType(message);

        switch(messageType)
        {
            case GPS:
                broadcast(new LocationReport(message, System.currentTimeMillis()));
                break;
            case STATUS:
                broadcast(new Status(message, System.currentTimeMillis()));
                break;
            default:
                broadcast(new AutomaticNumberIdentification(message, System.currentTimeMillis()));
                break;
        }
    }

    /**
     * Broadcasts the message to an optional registered listener
     */
    private void broadcast(IMessage message)
    {
        if(mMessageListener != null)
        {
            mMessageListener.receive(message);
        }
    }

    /**
     * Registers the listener to receive fleetsync messages
     */
    public void setMessageListener(Listener<IMessage> listener)
    {
        mMessageListener = listener;
    }

    /**
     * Unregisters the current listener from receiving fleetsync messages
     */
    public void removeMessageListener()
    {
        mMessageListener = null;
    }
}
