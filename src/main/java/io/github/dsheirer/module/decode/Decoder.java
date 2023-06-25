/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.module.decode;

import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.IMessageProvider;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.Listener;

/**
 * Base decoder class.
 */
public abstract class Decoder extends Module implements IMessageProvider
{
    private Listener<IMessage> mMessageDistributor = new MessageDistributor();
    protected Listener<IMessage> mMessageListener;
    private boolean mRunning;

    /**
     * Decoder - parent class for all decoders, demodulators and components.
     */
    public Decoder()
    {
    }

    /**
     * Starts this decoder and sets the running flag to true.
     */
    public void start()
    {
        mRunning = true;
    }

    /**
     * Stops this decoder and sets the running flag to false.
     */
    public void stop()
    {
        mRunning = false;
    }

    /**
     * Indicates if this decoder is currently started and in a running state.
     * @return true if running or false if not.
     */
    public boolean isRunning()
    {
        return mRunning;
    }

    public void reset()
    {
        //no-op
    }

    /**
     * Identifies the decoder type (ie protocol)
     */
    public abstract DecoderType getDecoderType();


    /**
     * Adds a listener for receiving decoded messages from this decoder
     */
    @Override
    public void setMessageListener(Listener<IMessage> listener)
    {
        mMessageListener = listener;
    }

    /**
     * Removes the listener from receiving decoded messages from all attached
     * decoders
     */
    @Override
    public void removeMessageListener()
    {
        mMessageListener = null;
    }

    /**
     * Message listener for receiving the output from this decoder
     */
    protected Listener<IMessage> getMessageListener()
    {
        return mMessageDistributor;
    }

    /**
     * Distributes/forwards messages from sub-class decoder implementations to the registered message listener.
     */
    public class MessageDistributor implements Listener<IMessage>
    {
        @Override
        public void receive(IMessage message)
        {
            if(mMessageListener != null)
            {
                mMessageListener.receive(message);
            }
        }
    }
}
