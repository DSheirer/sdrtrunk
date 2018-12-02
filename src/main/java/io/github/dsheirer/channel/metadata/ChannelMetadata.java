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

package io.github.dsheirer.channel.metadata;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
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
import io.github.dsheirer.identifier.decoder.DecoderStateIdentifier;
import io.github.dsheirer.sample.Listener;
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
    private DecoderStateIdentifier mDecoderStateIdentifier = DecoderStateIdentifier.IDLE;
    private DecoderTypeConfigurationIdentifier mDecoderTypeConfigurationIdentifier;
    private Identifier mFromIdentifier;
    private Alias mFromIdentifierAlias;
    private Identifier mToIdentifier;
    private Alias mToIdentifierAlias;

    private IChannelMetadataUpdateListener mIChannelMetadataUpdateListener;
    private AliasModel mAliasModel;
    private AliasList mAliasList;

    public ChannelMetadata(AliasModel aliasModel)
    {
        mAliasModel = aliasModel;
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
    public DecoderStateIdentifier getDecoderStateIdentifier()
    {
        return mDecoderStateIdentifier;
    }

    public boolean hasDecoderStateIdentifier()
    {
        return mDecoderStateIdentifier != null;
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
    public Alias getFromIdentifierAlias()
    {
        return mFromIdentifierAlias;
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
    public Alias getToIdentifierAlias()
    {
        return mToIdentifierAlias;
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
        Identifier identifier = update.getIdentifier();

        switch(identifier.getIdentifierClass())
        {
            case CONFIGURATION:
                switch(identifier.getForm())
                {
                    case ALIAS_LIST:
                        if(update.isAdded())
                        {
                            mAliasList = mAliasModel.getAliasList((AliasListConfigurationIdentifier)identifier);

                            if(mAliasList != null)
                            {
                                if(mToIdentifier != null)
                                {
                                    mToIdentifierAlias = mAliasList.getAlias(mToIdentifier);
                                }
                                if(mFromIdentifier != null)
                                {
                                    mFromIdentifierAlias = mAliasList.getAlias(mFromIdentifier);
                                }
                            }
                        }
                        break;
                    case CHANNEL:
                        mChannelNameConfigurationIdentifier = update.isAdded() ? (ChannelNameConfigurationIdentifier)identifier : null;
                        broadcastUpdate(ChannelMetadataField.CONFIGURATION_CHANNEL);
                        break;
                    case CHANNEL_FREQUENCY:
                        mFrequencyConfigurationIdentifier = update.isAdded() ? (FrequencyConfigurationIdentifier)identifier : null;
                        broadcastUpdate(ChannelMetadataField.CONFIGURATION_FREQUENCY);
                        break;
                    case DECODER_TYPE:
                        mDecoderTypeConfigurationIdentifier = update.isAdded() ? (DecoderTypeConfigurationIdentifier)identifier : null;
                        broadcastUpdate(ChannelMetadataField.DECODER_TYPE);
                        break;
                    case SITE:
                        mSiteConfigurationIdentifier = update.isAdded() ? (SiteConfigurationIdentifier)identifier : null;
                        broadcastUpdate(ChannelMetadataField.CONFIGURATION_SITE);
                        break;
                    case SYSTEM:
                        mSystemConfigurationIdentifier = update.isAdded() ? (SystemConfigurationIdentifier)identifier : null;
                        broadcastUpdate(ChannelMetadataField.CONFIGURATION_SYSTEM);
                        break;
                }
                break;
            case DECODER:
                switch(identifier.getForm())
                {
                    case STATE:
                        mDecoderStateIdentifier = update.isAdded() ? (DecoderStateIdentifier)identifier : null;
                        broadcastUpdate(ChannelMetadataField.DECODER_STATE);
                        break;
                }
                break;
            case USER:
                if(identifier.getRole() == Role.FROM)
                {
                    mFromIdentifier = update.isAdded() ? identifier : null;

                    if(mAliasList != null && mFromIdentifier != null)
                    {
                        mFromIdentifierAlias = mAliasList.getAlias(mFromIdentifier);
                    }
                    else
                    {
                        mFromIdentifierAlias = null;
                    }

                    broadcastUpdate(ChannelMetadataField.USER_FROM);
                }
                else if(identifier.getRole() == Role.TO)
                {
                    mToIdentifier = update.isAdded() ? identifier : null;

                    if(mAliasList != null && mToIdentifier != null)
                    {
                        mToIdentifierAlias = mAliasList.getAlias(mToIdentifier);
                    }
                    else
                    {
                        mToIdentifierAlias = null;
                    }

                    broadcastUpdate(ChannelMetadataField.USER_TO);
                }
                break;
        }
    }
}
