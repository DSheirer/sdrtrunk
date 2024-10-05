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

package io.github.dsheirer.channel.metadata;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierUpdateListener;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.configuration.AliasListConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.ChannelNameConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.DecoderTypeConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.SiteConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.SystemConfigurationIdentifier;
import io.github.dsheirer.identifier.decoder.ChannelStateIdentifier;
import io.github.dsheirer.identifier.decoder.DecoderLogicalChannelNameIdentifier;
import io.github.dsheirer.sample.Listener;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Channel metadata containing details about the channel configuration, decoder state and current
 * set of decoded user identifiers (ie TO and FROM).
 */
public class ChannelMetadata implements Listener<IdentifierUpdateNotification>, IdentifierUpdateListener
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelMetadata.class);

    private SystemConfigurationIdentifier mSystemConfigurationIdentifier;
    private SiteConfigurationIdentifier mSiteConfigurationIdentifier;
    private ChannelNameConfigurationIdentifier mChannelNameConfigurationIdentifier;
    private FrequencyConfigurationIdentifier mFrequencyConfigurationIdentifier;
    private ChannelStateIdentifier mChannelStateIdentifier = ChannelStateIdentifier.IDLE;
    private DecoderLogicalChannelNameIdentifier mDecoderLogicalChannelNameIdentifier;
    private DecoderTypeConfigurationIdentifier mDecoderTypeConfigurationIdentifier;
    private Identifier mFromIdentifier;
    private List<Alias> mFromIdentifierAliases;
    private Identifier mTalkerAliasIdentifier;
    private Identifier mToIdentifier;
    private List<Alias> mToIdentifierAliases;
    private Integer mTimeslot;

    private IChannelMetadataUpdateListener mIChannelMetadataUpdateListener;
    private AliasModel mAliasModel;
    private AliasList mAliasList;

    /**
     * Constructs an instance
     * @param aliasModel for alias lookups
     * @param timeslot for this metadata
     */
    public ChannelMetadata(AliasModel aliasModel, Integer timeslot)
    {
        mAliasModel = aliasModel;
        mTimeslot = timeslot;
    }

    /**
     * Constructs an instance
     * @param aliasModel for alias lookups
     */
    public ChannelMetadata(AliasModel aliasModel)
    {
        this(aliasModel, null);
    }

    /**
     * Creates a textual description of this channel metadata.
     */
    public String getDescription()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Channel Metadata Description\n");
        sb.append("\tTimeslot: ").append(mTimeslot).append("\n");
        Identifier decoder = getDecoderTypeConfigurationIdentifier();
        sb.append("\tDecoder: ").append(decoder != null ? decoder : "(null)").append("\n");
        Identifier state = getChannelStateIdentifier();
        sb.append("\tState: ").append(state != null ? state : "(null)").append("\n");
        Identifier system = getSystemConfigurationIdentifier();
        sb.append("\tSystem: ").append(system != null ? system : "(null)").append("\n");
        Identifier site = getSiteConfigurationIdentifier();
        sb.append("\tSite: ").append(site != null ? site : "(null)").append("\n");
        Identifier channel = getChannelNameConfigurationIdentifier();
        sb.append("\tChannel: ").append(channel != null ? channel : "(null)").append("\n");
        Identifier frequency = getFrequencyConfigurationIdentifier();
        sb.append("\tFrequency: ").append(frequency != null ? frequency : "(null)").append("\n");
        Identifier logical = getDecoderLogicalChannelNameIdentifier();
        sb.append("\tLogical Channel: ").append(logical != null ? logical : "(null)").append("\n");
        return sb.toString();
    }

    public Integer getTimeslot()
    {
        return mTimeslot;
    }

    public boolean hasTimeslot()
    {
        return mTimeslot != null;
    }

    @Override
    public Listener<IdentifierUpdateNotification> getIdentifierUpdateListener()
    {
        return this;
    }

    /**
     * System configuration identifier
     */
    public SystemConfigurationIdentifier getSystemConfigurationIdentifier()
    {
        return mSystemConfigurationIdentifier;
    }

    public boolean hasSystemConfigurationIdentifier()
    {
        return mSystemConfigurationIdentifier != null;
    }

    /**
     * Site configuration identifier
     */
    public SiteConfigurationIdentifier getSiteConfigurationIdentifier()
    {
        return mSiteConfigurationIdentifier;
    }

    public boolean hasSiteConfigurationIdentifier()
    {
        return mSiteConfigurationIdentifier != null;
    }

    /**
     * Channel configuration identifier
     */
    public ChannelNameConfigurationIdentifier getChannelNameConfigurationIdentifier()
    {
        return mChannelNameConfigurationIdentifier;
    }

    public boolean hasChannelConfigurationIdentifier()
    {
        return mChannelNameConfigurationIdentifier != null;
    }

    /**
     * Logical channel name/number expoded by the decoder
     */
    public DecoderLogicalChannelNameIdentifier getDecoderLogicalChannelNameIdentifier()
    {
        return mDecoderLogicalChannelNameIdentifier;
    }

    public boolean hasDecoderLogicalChannelNameIdentifier()
    {
        return mDecoderLogicalChannelNameIdentifier != null;
    }

    /**
     * Frequency configuration identifier
     */
    public FrequencyConfigurationIdentifier getFrequencyConfigurationIdentifier()
    {
        return mFrequencyConfigurationIdentifier;
    }

    public boolean hasFrequencyConfigurationIdentifier()
    {
        return mFrequencyConfigurationIdentifier != null;
    }

    /**
     * Decoder state identifier
     */
    public ChannelStateIdentifier getChannelStateIdentifier()
    {
        return mChannelStateIdentifier;
    }

    public boolean hasDecoderStateIdentifier()
    {
        return mChannelStateIdentifier != null;
    }

    /**
     * Decoder type identifier
     */
    public DecoderTypeConfigurationIdentifier getDecoderTypeConfigurationIdentifier()
    {
        return mDecoderTypeConfigurationIdentifier;
    }

    public boolean hasDecoderTypeIdentifier()
    {
        return mDecoderTypeConfigurationIdentifier != null;
    }

    /**
     * Current call event FROM identifier
     */
    public Identifier getFromIdentifier()
    {
        return mFromIdentifier;
    }

    public boolean hasFromIdentifier()
    {
        return mFromIdentifier != null;
    }

    /**
     * Optional alias associated with the FROM identifier
     */
    public List<Alias> getFromIdentifierAliases()
    {
        return mFromIdentifierAliases;
    }

    /**
     * Optional talker alias identifier
     */
    public Identifier getTalkerAliasIdentifier()
    {
        return mTalkerAliasIdentifier;
    }

    /**
     * Indicates if we have a non-null talker alias identifier
     */
    public boolean hasTalkerAliasIdentifier()
    {
        return mTalkerAliasIdentifier != null;
    }

    /**
     * Current call event TO identifier
     */
    public Identifier getToIdentifier()
    {
        return mToIdentifier;
    }

    public boolean hasToIdentifier()
    {
        return mToIdentifier != null;
    }

    /**
     * Optional alias associated with the TO identifier
     */
    public List<Alias> getToIdentifierAliases()
    {
        return mToIdentifierAliases;
    }

    /**
     * Registers the listener for receiving field update events
     */
    public void setUpdateEventListener(IChannelMetadataUpdateListener listener)
    {
        mIChannelMetadataUpdateListener = listener;
    }

    /**
     * Deregisters the listener from receiving field update events
     */
    public void removeUpdateEventListener()
    {
        mIChannelMetadataUpdateListener = null;
    }

    /**
     * Broadcasts this metadata and the field that has been updated
     *
     * @param field that has been updated
     */
    private void broadcastUpdate(ChannelMetadataField field)
    {
        if(mIChannelMetadataUpdateListener != null)
        {
            mIChannelMetadataUpdateListener.updated(this, field);
        }
    }

    @Override
    public void receive(IdentifierUpdateNotification update)
    {
//        mLog.debug("Received update: " + update +
//            " class:" + update.getIdentifier().getIdentifierClass() +
//            " form:" + update.getIdentifier().getForm() +
//            " role:" + update.getIdentifier().getRole() +
//            " class:" + update.getIdentifier().getClass());

        Identifier identifier = update.getIdentifier();

        switch(identifier.getIdentifierClass())
        {
            case CONFIGURATION:
                switch(identifier.getForm())
                {
                    case ALIAS_LIST:
                        if(update.isAdd())
                        {
                            mAliasList = mAliasModel.getAliasList((AliasListConfigurationIdentifier)identifier);

                            if(mAliasList != null)
                            {
                                if(mToIdentifier != null)
                                {
                                    mToIdentifierAliases = mAliasList.getAliases(mToIdentifier);
                                }
                                if(mFromIdentifier != null)
                                {
                                    mFromIdentifierAliases = mAliasList.getAliases(mFromIdentifier);
                                }
                            }
                        }
                        break;
                    case CHANNEL:
                        if(identifier instanceof ChannelNameConfigurationIdentifier && (update.isAdd() || update.isSilentAdd()))
                        {
                            mChannelNameConfigurationIdentifier = (ChannelNameConfigurationIdentifier)identifier;
                        }
                        else
                        {
                            mChannelNameConfigurationIdentifier = null;
                        }
                        broadcastUpdate(ChannelMetadataField.CONFIGURATION_CHANNEL);
                        break;
                    case CHANNEL_FREQUENCY:
                        if(identifier instanceof FrequencyConfigurationIdentifier && (update.isAdd() || update.isSilentAdd()))
                        {
                            mFrequencyConfigurationIdentifier = (FrequencyConfigurationIdentifier)identifier;
                        }
                        else
                        {
                            mFrequencyConfigurationIdentifier = null;
                        }
                        broadcastUpdate(ChannelMetadataField.CONFIGURATION_FREQUENCY);
                        break;
                    case DECODER_TYPE:
                        if(identifier instanceof DecoderTypeConfigurationIdentifier && (update.isAdd() || update.isSilentAdd()))
                        {
                            mDecoderTypeConfigurationIdentifier = (DecoderTypeConfigurationIdentifier)identifier;
                        }
                        else
                        {
                            mDecoderTypeConfigurationIdentifier = null;
                        }
                        broadcastUpdate(ChannelMetadataField.DECODER_TYPE);
                        break;
                    case SITE:
                        if(identifier instanceof SiteConfigurationIdentifier && (update.isAdd() || update.isSilentAdd()))
                        {
                            mSiteConfigurationIdentifier = (SiteConfigurationIdentifier)identifier;
                        }
                        else
                        {
                            mSiteConfigurationIdentifier = null;
                        }
                        broadcastUpdate(ChannelMetadataField.CONFIGURATION_SITE);
                        break;
                    case SYSTEM:
                        if(identifier instanceof SystemConfigurationIdentifier && (update.isAdd() || update.isSilentAdd()))
                        {
                            mSystemConfigurationIdentifier = (SystemConfigurationIdentifier)identifier;
                        }
                        else
                        {
                            mSystemConfigurationIdentifier = null;
                        }
                        broadcastUpdate(ChannelMetadataField.CONFIGURATION_SYSTEM);
                        break;
                }
                break;
            case DECODER:
                switch(identifier.getForm())
                {
                    case CHANNEL_NAME:
                        if(identifier instanceof DecoderLogicalChannelNameIdentifier && (update.isAdd() || update.isSilentAdd()))
                        {
                            mDecoderLogicalChannelNameIdentifier = (DecoderLogicalChannelNameIdentifier)identifier;
                        }
                        else
                        {
                            mDecoderLogicalChannelNameIdentifier = null;
                        }
                        broadcastUpdate(ChannelMetadataField.DECODER_CHANNEL_NAME);
                        break;
                    case STATE:
                        if(identifier instanceof ChannelStateIdentifier && (update.isAdd() || update.isSilentAdd()))
                        {
                            mChannelStateIdentifier = (ChannelStateIdentifier)identifier;
                        }
                        else
                        {
                            mChannelStateIdentifier = null;
                        }
                        broadcastUpdate(ChannelMetadataField.DECODER_STATE);
                        break;
                }
                break;
            case USER:
                if(identifier.getRole() == Role.FROM)
                {
                    if(identifier.getForm() == Form.TALKER_ALIAS)
                    {
                        mTalkerAliasIdentifier = identifier;
                    }
                    else
                    {
                        mFromIdentifier = update.isAdd() ? identifier : null;

                        if(mAliasList != null && mFromIdentifier != null)
                        {
                            mFromIdentifierAliases = mAliasList.getAliases(mFromIdentifier);
                        }
                        else
                        {
                            mFromIdentifierAliases = Collections.EMPTY_LIST;
                        }
                    }

                    broadcastUpdate(ChannelMetadataField.USER_FROM);
                }
                else if(identifier.getRole() == Role.TO)
                {
                    mToIdentifier = update.isAdd() ? identifier : null;

                    if(mAliasList != null && mToIdentifier != null)
                    {
                        mToIdentifierAliases = mAliasList.getAliases(mToIdentifier);
                    }
                    else
                    {
                        mToIdentifierAliases = Collections.EMPTY_LIST;
                    }

                    broadcastUpdate(ChannelMetadataField.USER_TO);
                }
                break;
        }
    }
}
