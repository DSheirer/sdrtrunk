/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.channel.metadata;

import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MutableMetadata extends Metadata implements Listener<AttributeChangeRequest>
{
    private final static Logger mLog = LoggerFactory.getLogger(MutableMetadata.class);

    private Broadcaster<MutableMetadataChangeEvent> mMetadataChangeEventBroadcaster = new Broadcaster<>();

    /**
     * Mutable channel metadata.  Contains all attributes that reflect the state and current attribute values for a
     * channel that is currently decoding.  This metadata is intended to support any decoding channel gui components to
     * graphically convey the current state of a decoding channel and to provide audio metadata
     */
    public MutableMetadata()
    {
        super();
    }

    /**
     * Sets the selected state for this metadata.  Selected indicates that the user has selected this metadata for
     * immediate monitor in the audio panel.  When true, this overrides the audio priority for any audio produced
     * by the channel and sets that audio to the highest priority.
     *
     * Any component that toggles the selected state for this metadata is also responsible for clearing the flag.
     */
    public void setSelected(boolean selected)
    {
        mSelected = selected;
        mUpdated = true;
    }

    /**
     * Sets the recordable flag to the argument value and overrides any recordable settings in the enclosed aliases.
     */
    public void setRecordable(boolean recordable)
    {
        mRecordable = recordable;
    }

    /**
     * Sets the primary decoder type
     */
    public void setPrimaryDecoderType(DecoderType decoderType)
    {
        mPrimaryDecoderType = decoderType;
        mUpdated = true;
    }

    /**
     * Request to change an attribute value for this metadata.  This is the primary method for updating all attributes
     * in this metadata set.
     */
    @Override
    public void receive(AttributeChangeRequest request)
    {
        switch(request.getAttribute())
        {
            case BUFFER_OVERFLOW:
                mBufferOverflow = request.getBooleanValue();
                broadcast(Attribute.BUFFER_OVERFLOW);
                break;
            case CHANNEL_CONFIGURATION_SYSTEM:
                mChannelConfigurationSystem = request.getStringValue();
                broadcast(Attribute.CHANNEL_CONFIGURATION_SYSTEM);
                break;
            case CHANNEL_CONFIGURATION_SITE:
                mChannelConfigurationSite = request.getStringValue();
                broadcast(Attribute.CHANNEL_CONFIGURATION_SITE);
                break;
            case CHANNEL_CONFIGURATION_NAME:
                mChannelConfigurationName = request.getStringValue();
                broadcast(Attribute.CHANNEL_CONFIGURATION_NAME);
                break;
            case CHANNEL_FREQUENCY:
                if(request.hasValue())
                {
                    mChannelFrequency = request.getLongValue();
                }
                broadcast(Attribute.CHANNEL_FREQUENCY);
                break;
            case CHANNEL_FREQUENCY_LABEL:
                mChannelFrequencyLabel = request.getStringValue();
                broadcast(Attribute.CHANNEL_FREQUENCY_LABEL);
                break;
            case MESSAGE:
                mMessage = request.getStringValue();
                broadcast(Attribute.MESSAGE);
                break;
            case MESSAGE_TYPE:
                mMessageType = request.getStringValue();
                broadcast(Attribute.MESSAGE_TYPE);
                break;
            case NETWORK_ID_1:
                mNetworkID1.setIdentifier(request.getStringValue());
                mNetworkID1.setAlias(request.getAlias());
                broadcast(Attribute.NETWORK_ID_1);
                break;
            case NETWORK_ID_2:
                mNetworkID2.setIdentifier(request.getStringValue());
                mNetworkID2.setAlias(request.getAlias());
                broadcast(Attribute.NETWORK_ID_2);
                break;
            case PRIMARY_ADDRESS_FROM:
                mPrimaryAddressFrom.setIdentifier(request.getStringValue());
                mPrimaryAddressFrom.setAlias(request.getAlias());
                broadcast(Attribute.PRIMARY_ADDRESS_FROM);
                break;
            case PRIMARY_ADDRESS_TO:
                mPrimaryAddressTo.setIdentifier(request.getStringValue());
                mPrimaryAddressTo.setAlias(request.getAlias());
                broadcast(Attribute.PRIMARY_ADDRESS_TO);
                break;
            case PRIMARY_DECODER_TYPE:
                mPrimaryDecoderType = request.getDecoderTypeValue();
                broadcast(Attribute.PRIMARY_DECODER_TYPE);
                break;
            case SECONDARY_ADDRESS_FROM:
                mSecondaryAddressFrom.setIdentifier(request.getStringValue());
                mSecondaryAddressFrom.setAlias(request.getAlias());
                broadcast(Attribute.SECONDARY_ADDRESS_FROM);
                break;
            case SECONDARY_ADDRESS_TO:
                mSecondaryAddressTo.setIdentifier(request.getStringValue());
                mSecondaryAddressTo.setAlias(request.getAlias());
                broadcast(Attribute.SECONDARY_ADDRESS_TO);
                break;
            case CHANNEL_STATE:
                mState = (State) request.getValue();
                broadcast(Attribute.CHANNEL_STATE);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized Metadata Attribute: " +
                    request.getAttribute().name());
        }

        mUpdated = true;
    }

    /**
     * Resets temporal attributes to null value.  This only resets attributes that have a non-ull value in order to
     * limit the number of change events that are produced.
     */
    public void resetTemporalAttributes()
    {
        mDoNotRecord = null;

        if(hasMessage())
        {
            mMessage = null;
            broadcast(Attribute.MESSAGE);
        }

        if(hasMessageType())
        {
            mMessageType = null;
            broadcast(Attribute.MESSAGE_TYPE);
        }

        if(mPrimaryAddressFrom.hasIdentifier())
        {
            mPrimaryAddressFrom.reset();
            broadcast(Attribute.PRIMARY_ADDRESS_FROM);
        }

        if(mPrimaryAddressTo.hasIdentifier())
        {
            mPrimaryAddressTo.reset();
            broadcast(Attribute.PRIMARY_ADDRESS_TO);
        }

        if(mSecondaryAddressFrom.hasIdentifier())
        {
            mSecondaryAddressFrom.reset();
            broadcast(Attribute.SECONDARY_ADDRESS_FROM);
        }

        if(mSecondaryAddressTo.hasIdentifier())
        {
            mSecondaryAddressTo.reset();
            broadcast(Attribute.SECONDARY_ADDRESS_TO);
        }

        mUpdated = true;
    }

    /**
     * Resets all attributes to empty values
     */
    public void resetAllAttributes()
    {
        resetTemporalAttributes();

        if(hasChannelConfigurationSystem())
        {
            mChannelConfigurationSystem = null;
            broadcast(Attribute.CHANNEL_CONFIGURATION_SYSTEM);
        }

        if(hasChannelConfigurationSite())
        {
            mChannelConfigurationSite = null;
            broadcast(Attribute.CHANNEL_CONFIGURATION_SITE);
        }

        if(hasChannelConfigurationName())
        {
            mChannelConfigurationSite = null;
            broadcast(Attribute.CHANNEL_CONFIGURATION_NAME);
        }

        if(hasChannelFrequencyLabel())
        {
            mChannelFrequencyLabel = null;
            broadcast(Attribute.CHANNEL_FREQUENCY_LABEL);
        }

        if(hasChannelFrequency())
        {
            mChannelFrequency = 0;
            broadcast(Attribute.CHANNEL_FREQUENCY);
        }

        if(hasPrimaryDecoderType())
        {
            mPrimaryDecoderType = null;
            broadcast(Attribute.PRIMARY_DECODER_TYPE);
        }

        if(mNetworkID1.hasIdentifier())
        {
            mNetworkID1.reset();
            broadcast(Attribute.NETWORK_ID_1);
        }

        if(mNetworkID2.hasIdentifier())
        {
            mNetworkID2.reset();
            broadcast(Attribute.NETWORK_ID_2);
        }

        mUpdated = true;
    }


    /**
     * Broadcasts to registered listeners that an attribute has changed for this metadata
     */
    private void broadcast(Attribute attribute)
    {
        mMetadataChangeEventBroadcaster.broadcast(new MutableMetadataChangeEvent(this, attribute));
    }

    /**
     * Adds the listener to receive metadata change events for this channel metadata
     */
    public void addListener(Listener<MutableMetadataChangeEvent> listener)
    {
        mMetadataChangeEventBroadcaster.addListener(listener);
    }

    /**
     * Removes the listener from receiving metadata change events for this channel metadata
     */
    public void removeListener(Listener<MutableMetadataChangeEvent> listener)
    {
        mMetadataChangeEventBroadcaster.removeListener(listener);
    }
}
