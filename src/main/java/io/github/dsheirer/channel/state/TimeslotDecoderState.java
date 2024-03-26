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
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.configuration.ChannelDescriptorConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.sample.Listener;

/**
 * Base decoder state implementation for a multi-timeslot protocol that tracks the state for a single timeslot.
 */
public abstract class TimeslotDecoderState extends DecoderState
{
    private int mTimeslot;

    /**
     * Constructs an instance
     * @param timeslot to monitor/maintain state.
     */
    public TimeslotDecoderState(int timeslot)
    {
        super(new MutableIdentifierCollection(timeslot));
        mTimeslot = timeslot;
        mConfigurationIdentifierListener = new TimeslotConfigurationIdentifierListener();
    }

    /**
     * Monitored timeslot for this decoder state instance.
     */
    protected int getTimeslot()
    {
        return mTimeslot;
    }

    /**
     * Listener for configuration type identifier updates sent from the channel state.  Adds configuration
     * identifiers to this decoder state so that decode events will contain configuration details in the
     * event's identifier collection.
     */
    public class TimeslotConfigurationIdentifierListener implements Listener<IdentifierUpdateNotification>
    {
        @Override
        public void receive(IdentifierUpdateNotification identifierUpdateNotification)
        {
            if(identifierUpdateNotification.getTimeslot() == getTimeslot())
            {
                if(identifierUpdateNotification.getIdentifier().getIdentifierClass() == IdentifierClass.CONFIGURATION &&
                    identifierUpdateNotification.getIdentifier().getForm() != Form.DECODER_TYPE &&
                    identifierUpdateNotification.getIdentifier().getForm() != Form.CHANNEL_DESCRIPTOR)
                {
                    if(identifierUpdateNotification.isAdd())
                    {
                        getIdentifierCollection().update(identifierUpdateNotification.getIdentifier());
                    }
                    else if(identifierUpdateNotification.isSilentAdd())
                    {
                        getIdentifierCollection().silentUpdate(identifierUpdateNotification.getIdentifier());
                    }
                }

                if(identifierUpdateNotification.getOperation() == IdentifierUpdateNotification.Operation.ADD)
                {
                    Identifier identifier = identifierUpdateNotification.getIdentifier();

                    if(identifier instanceof ChannelDescriptorConfigurationIdentifier)
                    {
                        setCurrentChannel(((ChannelDescriptorConfigurationIdentifier)identifier).getValue());
                    }
                    else if(identifier instanceof IChannelDescriptor)
                    {
                        setCurrentChannel((IChannelDescriptor)identifier);
                    }
                    else if(identifier instanceof FrequencyConfigurationIdentifier fci)
                    {
                        setCurrentFrequency(fci.getValue());
                    }
                }
            }
        }
    }
}
