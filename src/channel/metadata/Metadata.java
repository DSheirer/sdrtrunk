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
package channel.metadata;

import alias.Alias;
import channel.state.State;
import sample.Broadcaster;
import sample.Listener;

public class Metadata implements Listener<AttributeChangeRequest>
{
    private State mState = State.IDLE;
    private String mChannelConfigurationLabel1;
    private String mChannelConfigurationLabel2;
    private String mChannelID;
    private long mChannelFrequency;
    private AliasedIdentifier mNetworkID1 = new AliasedIdentifier();
    private AliasedIdentifier mNetworkID2 = new AliasedIdentifier();

    //Temporal attributes
    private String mMessage;
    private String mMessageType;
    private AliasedIdentifier mPrimaryAddressFrom = new AliasedIdentifier();
    private AliasedIdentifier mPrimaryAddressTo = new AliasedIdentifier();
    private AliasedIdentifier mSecondaryAddressFrom = new AliasedIdentifier();
    private AliasedIdentifier mSecondaryAddressTo = new AliasedIdentifier();

    private Broadcaster<MetadataChangeEvent> mMetadataChangeEventBroadcaster = new Broadcaster<>();


    /**
     * Channel metadata.  Contains all attributes that reflect the state and current attribute values for a channel
     * that is currently decoding.  This metadata is intended to support any decoding channel gui components to
     * graphically convey the current state of a decoding channel.
     */
    public Metadata()
    {
    }

    /**
     * Sets the state of this metadata
     */
    public void setState(State state)
    {
        mState = state;
        broadcast(Attribute.CHANNEL_STATE);
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
            case CHANNEL_CONFIGURATION_LABEL_1:
                mChannelConfigurationLabel1 = request.getStringValue();
                broadcast(Attribute.CHANNEL_CONFIGURATION_LABEL_1);
                break;
            case CHANNEL_CONFIGURATION_LABEL_2:
                mChannelConfigurationLabel2 = request.getStringValue();
                broadcast(Attribute.CHANNEL_CONFIGURATION_LABEL_2);
                break;
            case CHANNEL_FREQUENCY:
                if(request.hasValue())
                {
                    mChannelFrequency = request.getLongValue();
                }
                else
                {
                    mChannelFrequency = 0;
                }
                broadcast(Attribute.CHANNEL_FREQUENCY);
                break;
            case CHANNEL_ID:
                mChannelID = request.getStringValue();
                broadcast(Attribute.CHANNEL_ID);
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
            default:
                throw new IllegalArgumentException("Unrecognized Metadata Attribute: " +
                    request.getAttribute().name());
        }
    }

    /**
     * Broadcasts to registered listeners that an attribute has changed for this metadata
     */
    private void broadcast(Attribute attribute)
    {
        mMetadataChangeEventBroadcaster.broadcast(new MetadataChangeEvent(this, attribute));
    }

    /**
     * Adds the listener to receive metadata change events for this channel metadata
     */
    public void addListener(Listener<MetadataChangeEvent> listener)
    {
        mMetadataChangeEventBroadcaster.addListener(listener);
    }

    /**
     * Removes the listener from receiving metadata change events for this channel metadata
     */
    public void removeListener(Listener<MetadataChangeEvent> listener)
    {
        mMetadataChangeEventBroadcaster.removeListener(listener);
    }

    /**
     * Resets temporal attributes to null value.  This only resets attributes that have a non-ull value in order to
     * limit the number of change events that are produced.
     */
    public void resetTemporalAttributes()
    {
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
    }

    /**
     * Resets all attributes to empty values
     */
    public void resetAllAttributes()
    {
        resetTemporalAttributes();

        if(hasChannelConfigurationLabel1())
        {
            mChannelConfigurationLabel1 = null;
            broadcast(Attribute.CHANNEL_CONFIGURATION_LABEL_1);
        }

        if(hasChannelConfigurationLabel2())
        {
            mChannelConfigurationLabel2 = null;
            broadcast(Attribute.CHANNEL_CONFIGURATION_LABEL_2);
        }

        if(hasChannelID())
        {
            mChannelID = null;
            broadcast(Attribute.CHANNEL_ID);
        }

        if(hasChannelFrequency())
        {
            mChannelFrequency = 0;
            broadcast(Attribute.CHANNEL_FREQUENCY);
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
    }

    /**
     * Returns the alias associated with the attribute or null
     */
    public Alias getAlias(Attribute attribute)
    {
        switch(attribute)
        {
            case NETWORK_ID_1:
                return getNetworkID1().getAlias();
            case NETWORK_ID_2:
                return getNetworkID2().getAlias();
            case PRIMARY_ADDRESS_FROM:
                return getPrimaryAddressFrom().getAlias();
            case PRIMARY_ADDRESS_TO:
                return getPrimaryAddressTo().getAlias();
            case SECONDARY_ADDRESS_FROM:
                return getSecondaryAddressFrom().getAlias();
            case SECONDARY_ADDRESS_TO:
                return getSecondaryAddressTo().getAlias();
        }

        return null;
    }

    /**
     * Returns the string value for the specified attribute or null
     */
    public String getValue(Attribute attribute)
    {
        switch(attribute)
        {
            case CHANNEL_CONFIGURATION_LABEL_1:
                return mChannelConfigurationLabel1;
            case CHANNEL_CONFIGURATION_LABEL_2:
                return mChannelConfigurationLabel2;
            case CHANNEL_ID:
                return mChannelID;
            case CHANNEL_STATE:
                return mState.getDisplayValue();
            case MESSAGE:
                return mMessage;
            case MESSAGE_TYPE:
                return mMessageType;
            case NETWORK_ID_1:
                return mNetworkID1.getIdentifier();
            case NETWORK_ID_2:
                return mNetworkID2.getIdentifier();
            case PRIMARY_ADDRESS_FROM:
                return mPrimaryAddressFrom.getIdentifier();
            case PRIMARY_ADDRESS_TO:
                return mPrimaryAddressTo.getIdentifier();
            case SECONDARY_ADDRESS_FROM:
                return mSecondaryAddressFrom.getIdentifier();
            case SECONDARY_ADDRESS_TO:
                return mPrimaryAddressTo.getIdentifier();
        }

        return null;
    }

    /**
     * Current channel state
     */
    public State getState()
    {
        return mState;
    }

    /**
     * System name from the channel configuration
     */
    public String getChannelConfigurationLabel1()
    {
        return mChannelConfigurationLabel1;
    }

    /**
     * Indicates if there is a non-null, non-empty channel configuration system value
     */
    public boolean hasChannelConfigurationLabel1()
    {
        return mChannelConfigurationLabel1 != null && !mChannelConfigurationLabel1.isEmpty();
    }

    /**
     * Site name from the channel configuration
     */
    public String getChannelConfigurationLabel2()
    {
        return mChannelConfigurationLabel2;
    }

    /**
     * Indicates if there is a non-null, non-empty channel configuration site value
     */
    public boolean hasChannelConfigurationLabel2()
    {
        return mChannelConfigurationLabel2 != null && !mChannelConfigurationLabel2.isEmpty();
    }

    /**
     * Channel ID or Channel Number
     */
    public String getChannelID()
    {
        return mChannelID;
    }

    /**
     * Indicates if there is a non-null, non-empty channel identifier value
     */
    public boolean hasChannelID()
    {
        return mChannelID != null && !mChannelID.isEmpty();
    }

    /**
     * Channel frequency
     */
    public long getChannelFrequency()
    {
        return mChannelFrequency;
    }

    /**
     * Indicates if there is a non-zero channel frequency value
     */
    public boolean hasChannelFrequency()
    {
        return mChannelFrequency > 0;
    }

    /**
     * Network Identifier 1
     */
    public AliasedIdentifier getNetworkID1()
    {
        return mNetworkID1;
    }

    /**
     * Network Identifier 2
     */
    public AliasedIdentifier getNetworkID2()
    {
        return mNetworkID2;
    }

    /**
     * Message
     */
    public String getMessage()
    {
        return mMessage;
    }

    /**
     * Indicates if there is a non-null, non-empty message value
     */
    public boolean hasMessage()
    {
        return mMessage != null && !mMessage.isEmpty();
    }

    /**
     * Message and Decoder type
     */
    public String getMessageType()
    {
        return mMessageType;
    }

    /**
     * Indicates if there is a non-null, non-empty message and decoder type value
     */
    public boolean hasMessageType()
    {
        return mMessageType != null && !mMessageType.isEmpty();
    }

    /**
     * Primary FROM Address
     */
    public AliasedIdentifier getPrimaryAddressFrom()
    {
        return mPrimaryAddressFrom;
    }

    /**
     * Primary TO Address
     */
    public AliasedIdentifier getPrimaryAddressTo()
    {
        return mPrimaryAddressTo;
    }

    /**
     * Secondary FROM Address
     */
    public AliasedIdentifier getSecondaryAddressFrom()
    {
        return mSecondaryAddressFrom;
    }

    /**
     * Secondary TO Address
     */
    public AliasedIdentifier getSecondaryAddressTo()
    {
        return mSecondaryAddressTo;
    }
}
