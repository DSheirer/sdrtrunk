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
package io.github.dsheirer.module.decode.nxdn;

import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NXDN Message Processor.
 *
 * Performs post-message creation processing and enrichment before the message is sent downstream.
 */
public class NXDNMessageProcessor implements Listener<IMessage>
{
    private final static Logger LOGGER = LoggerFactory.getLogger(NXDNMessageProcessor.class);

    /**
     * Downstream message listener
     */
    private Listener<IMessage> mMessageListener;

    /**
     * Constructs an instance
     */
    public NXDNMessageProcessor()
    {
    }

    /**
     * Processes the message for enrichment or reassembly of fragments and sends the enriched message and any additional
     * messages that were created during the enrichment to the registered message listener.
     * @param message to process
     */
    @Override
    public void receive(IMessage message)
    {
        if(message.isValid())
        {

        }
        else
        {
            dispatch(message);
        }
    }

    /**
     * Post-process the message for frequency band details.
     * @param message to post process and dispatch
     */
    private void dispatch(IMessage message)
    {
        if(message == null)
        {
            return;
        }

        if(mMessageListener != null)
        {
            mMessageListener.receive(message);
        }
    }

    /**
     * Prepares for disposal of this instance.
     */
    public void dispose()
    {
        mMessageListener = null;
    }

    /**
     * Sets the message listener to receive the output from this processor.
     * @param listener to receive output messages
     */
    public void setMessageListener(Listener<IMessage> listener)
    {
        mMessageListener = listener;
    }

    /**
     * Clears a registered message listener.
     */
    public void removeMessageListener()
    {
        mMessageListener = null;
    }
}
