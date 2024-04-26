/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.channel.state;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.configuration.ChannelDescriptorConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.DecoderTypeConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.sample.Listener;

/**
 * Channel state monitors the stream of decoded messages produced by the
 * decoder and broadcasts call events as they occur within the decoded message activity.
 *
 * Provides access to a textual activity summary of events observed.
 */
public abstract class DecoderState extends AbstractDecoderState
{
    private MutableIdentifierCollection mIdentifierCollection;
    protected Listener<IdentifierUpdateNotification> mConfigurationIdentifierListener;
    protected IChannelDescriptor mCurrentChannel;
    private long mCurrentFrequency;

    /**
     * Constructs an instance
     * @param mutableIdentifierCollection to preload into this decoder state
     */
    public DecoderState(MutableIdentifierCollection mutableIdentifierCollection)
    {
        mIdentifierCollection = mutableIdentifierCollection;
        mIdentifierCollection.update(new DecoderTypeConfigurationIdentifier(getDecoderType()));
    }

    /**
     * Constructs an instance using an empty identifier collection.
     */
    public DecoderState()
    {
        this(new MutableIdentifierCollection());
        mConfigurationIdentifierListener = new ConfigurationIdentifierListener();
    }

    @Override
    public void start()
    {
        super.start();
        //Broadcast the existing identifiers (as add events) so that they can be received by external listeners
        mIdentifierCollection.broadcastIdentifiers();
    }

    /**
     * Current frequency for this channel.
     */
    public long getCurrentFrequency()
    {
        return mCurrentFrequency;
    }

    /**
     * Sets the current frequency for this channel.
     * @param frequency to set.
     */
    public void setCurrentFrequency(long frequency)
    {
        mCurrentFrequency = frequency;
    }

    /**
     * Registers the listener to receive identifier update notifications
     */
    @Override
    public void setIdentifierUpdateListener(Listener<IdentifierUpdateNotification> listener)
    {
        getIdentifierCollection().setIdentifierUpdateListener(listener);
    }

    /**
     * Removes the listener from receiving identifier update notifications
     */
    @Override
    public void removeIdentifierUpdateListener()
    {
        getIdentifierCollection().removeIdentifierUpdateListener();
    }

    /**
     * Current collection of identifiers managed by the decoder state.
     */
    public MutableIdentifierCollection getIdentifierCollection()
    {
        return mIdentifierCollection;
    }

    /**
     * Optional current channel descriptor
     */
    protected IChannelDescriptor getCurrentChannel()
    {
        return mCurrentChannel;
    }

    /**
     * Sets the current channel descriptor
     */
    public void setCurrentChannel(IChannelDescriptor channel)
    {
        mCurrentChannel = channel;
    }

    /**
     * Resets the current temporal state variables to end the current event and prepare for the next.
     */
    protected void resetState()
    {
        mIdentifierCollection.remove(IdentifierClass.USER);
    }

    /**
     * Reset the decoder state to prepare for processing a different sample
     * source
     */
    public void reset()
    {
        setCurrentChannel(null);
    }

    /**
     * Allow the decoder to perform any setup actions
     */
    public abstract void init();

    /**
     * Listens for configuration identifiers and adds them to this decoder state's identifier collection.
     * @return
     */
    @Override
    public Listener<IdentifierUpdateNotification> getIdentifierUpdateListener()
    {
        return mConfigurationIdentifierListener;
    }

    /**
     * Configuration identifier listener.
     */
    public Listener<IdentifierUpdateNotification> getConfigurationIdentifierListener()
    {
        return mConfigurationIdentifierListener;
    }

    /**
     * Listener for configuration type identifier updates sent from the channel state.  Adds configuration
     * identifiers to this decoder state so that decode events will contain configuration details in the
     * event's identifier collection.
     */
    public class ConfigurationIdentifierListener implements Listener<IdentifierUpdateNotification>
    {
        @Override
        public void receive(IdentifierUpdateNotification identifierUpdateNotification)
        {
            getIdentifierCollection().receive(identifierUpdateNotification);

            if(identifierUpdateNotification.getIdentifier() instanceof ChannelDescriptorConfigurationIdentifier cdci)
            {
                setCurrentChannel(cdci.getValue());
            }
            else if(identifierUpdateNotification.getIdentifier() instanceof FrequencyConfigurationIdentifier fci)
            {
                setCurrentFrequency(fci.getValue());
            }
        }
    }
}
