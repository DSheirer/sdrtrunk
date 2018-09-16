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

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.alias.id.priority.Priority;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.module.decode.DecoderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.TreeSet;

public class Metadata
{
    private final static Logger mLog = LoggerFactory.getLogger(Metadata.class);

    // Static unique metadata identifier tracking
    private static int UNIQUE_METADATA_ID_GENERATOR = 0;

    private int mMetadataID;

    protected boolean mUpdated;
    protected DecoderType mPrimaryDecoderType;
    protected boolean mSelected;
    protected boolean mBufferOverflow;
    protected State mState = State.IDLE;
    protected String mChannelConfigurationSystem;
    protected String mChannelConfigurationSite;
    protected String mChannelConfigurationName;
    protected String mChannelFrequencyLabel;
    protected long mChannelFrequency;
    protected AliasedIdentifier mNetworkID1 = new AliasedIdentifier();
    protected AliasedIdentifier mNetworkID2 = new AliasedIdentifier();

    protected String mMessage;
    protected String mMessageType;
    protected AliasedIdentifier mPrimaryAddressFrom = new AliasedIdentifier();
    protected AliasedIdentifier mPrimaryAddressTo = new AliasedIdentifier();
    protected AliasedIdentifier mSecondaryAddressFrom = new AliasedIdentifier();
    protected AliasedIdentifier mSecondaryAddressTo = new AliasedIdentifier();

    //Lazily constructed member variables.
    private Integer mAudioPriority;
    protected Boolean mRecordable = false;
    protected Boolean mDoNotRecord;
    private Set<BroadcastChannel> mBroadcastChannels;

    private long mTimestamp = System.currentTimeMillis();


    /**
     * Channel metadata.  Contains all attributes that reflect the state and current attribute values for a channel
     * that is currently decoding.  This metadata is intended to support any decoding channel gui components to
     * graphically convey the current state of a decoding channel and to provide audio metadata
     */
    public Metadata()
    {
        mMetadataID = UNIQUE_METADATA_ID_GENERATOR++;
    }

    /**
     * Protected constructor.  Primarily used by the copyOf() method to create a metadata copy with the same ID
     */
    protected Metadata(int metadataID)
    {
        mMetadataID = metadataID;
    }

    /**
     * Timestamp when this metadata was created
     * @return milliseconds since epoch
     */
    public long getTimestamp()
    {
        return mTimestamp;
    }
    /**
     * Unique string identifier for this metadata that is comprised of the channel ID and the primary TO address.
     *
     * This identifier can be used to uniquely identify a channel audio stream and the primary communicant.
     */
    public String getUniqueIdentifier()
    {
        return "SRC:" + mMetadataID +
            " ID:" + (mPrimaryAddressTo.hasIdentifier() ? mPrimaryAddressTo.getIdentifier() : "UNKNOWN");
    }

    /**
     * Indicates if any of the fields of this metadata have been updated since the last time a copy was made from this
     * metadata.  This method is primarily used by downstream audio playback and audio recording to signal when changes
     * are made to the metadata that requires the downstream component to reinspect the metadata.
     *
     * This flag is reset to false immediately after a copy is made of this metadata via the copyOf() method.
     */
    public boolean isUpdated()
    {
        return mUpdated;
    }

    /**
     * Audio Priority as the highest audio priority value from across the primary and secondary identifier aliases.
     *
     * A default priority will be used if there are no aliased values.  If there is a conflict between audio priority
     * and 'Do Not Monitor' among the aliases, the highest audio priority will be used (prefer to monitor).
     *
     * -1   Do Not Monitor
     *  0   Channel Selected for immediate monitor
     *  1   Highest Priority
     *  100 Lowest Priority (default)
     *
     * @return audio priority
     */
    public int getAudioPriority()
    {
        if(isSelected())
        {
            return Priority.SELECTED_PRIORITY;
        }

        if(mAudioPriority == null)
        {
            determineAudioPriority();
        }

        return mAudioPriority;
    }

    /**
     * Indicates if the audio priority is set to 'Do Not Monitor'
     */
    public boolean isDoNotMonitor()
    {
        return getAudioPriority() == Priority.DO_NOT_MONITOR;
    }

    /**
     * Indicates if any of the primary or secondary TO/FROM aliases are identified as recordable
     */
    public boolean isRecordable()
    {
        if(mDoNotRecord == null)
        {
            determineRecordable();
        }

        return mRecordable && !mDoNotRecord;
    }

    /**
     * Indicates that this metadata has been selected by the user and any audio produced by this channel will be set
     * to the highest playback priority (immediate monitor).
     */
    public boolean isSelected()
    {
        return mSelected;
    }

    /**
     * Indicates if the channel for this metadata is in a buffer overflow state.
     */
    public boolean isBufferOverflow()
    {
        return mBufferOverflow;
    }

    /**
     * List of de-duplicated broadcast channels aggregated from the primary and secondary TO/FROM aliases.
     */
    public Set<BroadcastChannel> getBroadcastChannels()
    {
        if(mBroadcastChannels == null)
        {
            determineBroadcastChannels();
        }

        return mBroadcastChannels;
    }

    /**
     * Indicates if this metadata is streamable, meaning that it contains one ore more broadcast channels
     */
    public boolean isStreamable()
    {
        return !getBroadcastChannels().isEmpty();
    }

    /**
     * Unique identifier for the source channel that produces this metadata
     */
    public int getMetadataID()
    {
        return mMetadataID;
    }

    /**
     * Decoder type (ie protocol) for the primary decoder
     */
    public DecoderType getPrimaryDecoderType()
    {
        return mPrimaryDecoderType;
    }

    /**
     * Indicates if this metadata has a primary decoder type defined
     */
    public boolean hasPrimaryDecoderType()
    {
        return mPrimaryDecoderType != null;
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
            case CHANNEL_CONFIGURATION_SYSTEM:
                return mChannelConfigurationSystem;
            case CHANNEL_CONFIGURATION_SITE:
                return mChannelConfigurationSite;
            case CHANNEL_FREQUENCY_LABEL:
                return mChannelFrequencyLabel;
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
                return mSecondaryAddressTo.getIdentifier();
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
    public String getChannelConfigurationSystem()
    {
        return mChannelConfigurationSystem;
    }

    /**
     * Indicates if there is a non-null, non-empty channel configuration system value
     */
    public boolean hasChannelConfigurationSystem()
    {
        return mChannelConfigurationSystem != null && !mChannelConfigurationSystem.isEmpty();
    }

    /**
     * Site name from the channel configuration
     */
    public String getChannelConfigurationSite()
    {
        return mChannelConfigurationSite;
    }

    /**
     * Indicates if there is a non-null, non-empty channel configuration site value
     */
    public boolean hasChannelConfigurationSite()
    {
        return mChannelConfigurationSite != null && !mChannelConfigurationSite.isEmpty();
    }

    /**
     * Site name from the channel configuration
     */
    public String getChannelConfigurationName()
    {
        return mChannelConfigurationName;
    }

    /**
     * Indicates if there is a non-null, non-empty channel configuration name value
     */
    public boolean hasChannelConfigurationName()
    {
        return mChannelConfigurationSite != null && !mChannelConfigurationSite.isEmpty();
    }

    /**
     * Channel ID or Channel Number
     */
    public String getChannelFrequencyLabel()
    {
        return mChannelFrequencyLabel;
    }

    /**
     * Indicates if there is a non-null, non-empty channel identifier value
     */
    public boolean hasChannelFrequencyLabel()
    {
        return mChannelFrequencyLabel != null && !mChannelFrequencyLabel.isEmpty();
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

    /**
     * Determines the audio priority from across the primary and secondary TO/FROM aliases when they exist
     */
    private void determineAudioPriority()
    {
        int priority = Priority.DEFAULT_PRIORITY;

        boolean doNotMonitorFound = false;
        boolean audioPriorityFound = false;

        if(getPrimaryAddressTo().hasAlias())
        {
            Alias primaryTo = getPrimaryAddressTo().getAlias();

            if(primaryTo.hasCallPriority())
            {
                if(primaryTo.getCallPriority() == Priority.DO_NOT_MONITOR)
                {
                    doNotMonitorFound = true;
                }
                else
                {
                    audioPriorityFound = true;

                    if(primaryTo.getCallPriority() < priority)
                    {
                        priority = primaryTo.getCallPriority();
                    }
                }
            }
        }

        if(getPrimaryAddressFrom().hasAlias())
        {
            Alias primaryFrom = getPrimaryAddressFrom().getAlias();

            if(primaryFrom.hasCallPriority())
            {
                if(primaryFrom.getCallPriority() == Priority.DO_NOT_MONITOR)
                {
                    doNotMonitorFound = true;
                }
                else
                {
                    audioPriorityFound = true;

                    if(primaryFrom.getCallPriority() < priority)
                    {
                        priority = primaryFrom.getCallPriority();
                    }
                }
            }
        }

        if(getSecondaryAddressTo().hasAlias())
        {
            Alias secondaryTo = getSecondaryAddressTo().getAlias();

            if(secondaryTo.hasCallPriority())
            {
                if(secondaryTo.getCallPriority() == Priority.DO_NOT_MONITOR)
                {
                    doNotMonitorFound = true;
                }
                else
                {
                    audioPriorityFound = true;

                    if(secondaryTo.getCallPriority() < priority)
                    {
                        priority = secondaryTo.getCallPriority();
                    }
                }
            }
        }

        if(getSecondaryAddressFrom().hasAlias())
        {
            Alias secondaryFrom = getSecondaryAddressFrom().getAlias();

            if(secondaryFrom.hasCallPriority())
            {
                if(secondaryFrom.getCallPriority() == Priority.DO_NOT_MONITOR)
                {
                    doNotMonitorFound = true;
                }
                else
                {
                    audioPriorityFound = true;

                    if(secondaryFrom.getCallPriority() < priority)
                    {
                        priority = secondaryFrom.getCallPriority();
                    }
                }
            }
        }

        //If we found a 'Do Not Monitor' and no other audio priority, then set to do not monitor
        if(doNotMonitorFound && !audioPriorityFound)
        {
            mAudioPriority = Priority.DO_NOT_MONITOR;
        }
        else
        {
            mAudioPriority = priority;
        }
    }

    /**
     * Determines if any of the primary or secondary to/from aliases are recordable.
     */
    private void determineRecordable()
    {
        mDoNotRecord = false;

        if(mPrimaryAddressTo.hasAlias() && !mPrimaryAddressTo.getAlias().isRecordable())
        {
            mDoNotRecord = true;
            return;
        }

        if(mPrimaryAddressFrom.hasAlias() && !mPrimaryAddressFrom.getAlias().isRecordable())
        {
            mDoNotRecord = true;
            return;
        }

        if(mSecondaryAddressTo.hasAlias() && !mSecondaryAddressTo.getAlias().isRecordable())
        {
            mDoNotRecord = true;
            return;
        }

        if(mSecondaryAddressFrom.hasAlias() && !mSecondaryAddressFrom.getAlias().isRecordable())
        {
            mDoNotRecord = true;
            return;
        }
    }

    /**
     * Aggregates a de-duplicated list of broadcast channels from among the primary/secondary to/from aliases
     */
    private void determineBroadcastChannels()
    {
        mBroadcastChannels = new TreeSet<>();

        if(mPrimaryAddressTo.hasAlias() && mPrimaryAddressTo.getAlias().isStreamable())
        {
            mBroadcastChannels.addAll(mPrimaryAddressTo.getAlias().getBroadcastChannels());
        }

        if(mPrimaryAddressFrom.hasAlias() && mPrimaryAddressFrom.getAlias().isStreamable())
        {
            mBroadcastChannels.addAll(mPrimaryAddressFrom.getAlias().getBroadcastChannels());
        }

        if(mSecondaryAddressTo.hasAlias() && mSecondaryAddressTo.getAlias().isStreamable())
        {
            mBroadcastChannels.addAll(mSecondaryAddressTo.getAlias().getBroadcastChannels());
        }

        if(mSecondaryAddressFrom.hasAlias() && mSecondaryAddressFrom.getAlias().isStreamable())
        {
            mBroadcastChannels.addAll(mSecondaryAddressFrom.getAlias().getBroadcastChannels());
        }
    }

    /**
     * Creates a deep/full snapshot copy of this metadata set.
     *
     * The streamable, recordable, audio priority and broadcast channels are not copied.  These are lazily determined
     * when initially requested.
     */
    public Metadata copyOf()
    {
        Metadata copy = new Metadata(mMetadataID);

        copy.mDoNotRecord = mDoNotRecord;
        copy.mRecordable = mRecordable;
        copy.mState = mState;
        copy.mPrimaryDecoderType = mPrimaryDecoderType;
        copy.mChannelFrequency = mChannelFrequency;

        copy.mChannelConfigurationSystem = hasChannelConfigurationSystem() ? new String(mChannelConfigurationSystem) : null;
        copy.mChannelConfigurationSite = hasChannelConfigurationSite() ? new String(mChannelConfigurationSite) : null;
        copy.mChannelConfigurationName = hasChannelConfigurationName() ? new String(mChannelConfigurationName) : null;
        copy.mChannelFrequencyLabel = hasChannelFrequencyLabel() ? new String(mChannelFrequencyLabel) : null;
        copy.mMessage = hasMessage() ? new String(mMessage) : null;
        copy.mMessageType = hasMessageType() ? new String(mMessageType) : null;

        copy.mNetworkID1 = mNetworkID1.copyOf();
        copy.mNetworkID2 = mNetworkID2.copyOf();
        copy.mPrimaryAddressFrom = mPrimaryAddressFrom.copyOf();
        copy.mPrimaryAddressTo = mPrimaryAddressTo.copyOf();
        copy.mSecondaryAddressFrom = mSecondaryAddressFrom.copyOf();
        copy.mSecondaryAddressTo = mSecondaryAddressTo.copyOf();

        copy.mUpdated = mUpdated;

        //Reset the updated flag
        mUpdated = false;

        return copy;
    }
}
