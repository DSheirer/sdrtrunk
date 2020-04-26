/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.channel.metadata;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.decoder.DecoderLogicalChannelNameIdentifier;
import io.github.dsheirer.preference.PreferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChannelMetadataModel extends AbstractTableModel implements IChannelMetadataUpdateListener
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelMetadataModel.class);

    public static final int COLUMN_DECODER_STATE = 0;
    public static final int COLUMN_DECODER_TYPE = 1;
    public static final int COLUMN_USER_FROM = 2;
    public static final int COLUMN_USER_FROM_ALIAS = 3;
    public static final int COLUMN_USER_TO = 4;
    public static final int COLUMN_USER_TO_ALIAS = 5;
    public static final int COLUMN_DECODER_LOGICAL_CHANNEL_NAME = 6;
    public static final int COLUMN_CONFIGURATION_FREQUENCY = 7;
    public static final int COLUMN_CONFIGURATION_CHANNEL = 8;

    private static final String[] COLUMNS = {"Status", "Decoder", "From", "Alias", "To", "Alias", "Channel", "Frequency", "Channel Name"};

    private List<ChannelMetadata> mChannelMetadata = new ArrayList();
    private Map<ChannelMetadata,Channel> mMetadataChannelMap = new HashMap();

    public ChannelMetadataModel()
    {
        MyEventBus.getEventBus().register(this);
    }

    /**
     * Receives preference update notifications via the event bus
     * @param preferenceType that was updated
     */
    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.TALKGROUP_FORMAT)
        {
            for(int row = 0; row < mChannelMetadata.size(); row++)
            {
                fireTableCellUpdated(row, COLUMN_USER_FROM);
                fireTableCellUpdated(row, COLUMN_USER_TO);
            }
        }
    }


    public void add(Collection<ChannelMetadata> channelMetadatas, Channel channel)
    {
        //Execute on the swing thread to avoid threading issues
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                for(ChannelMetadata channelMetadata: channelMetadatas)
                {
                    mChannelMetadata.add(channelMetadata);
                    mMetadataChannelMap.put(channelMetadata, channel);
                    int index = mChannelMetadata.indexOf(channelMetadata);
                    fireTableRowsInserted(index, index);
                    channelMetadata.setUpdateEventListener(ChannelMetadataModel.this);
                }
            }
        });
    }

    public void remove(ChannelMetadata channelMetadata)
    {
        //Execute on the swing thread to avoid threading issues
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                channelMetadata.removeUpdateEventListener();
                int index = mChannelMetadata.indexOf(channelMetadata);
                mChannelMetadata.remove(channelMetadata);
                mMetadataChannelMap.remove(channelMetadata);
                fireTableRowsDeleted(index, index);
            }
        });
    }

    /**
     * Get the channel metadata at the specified model row index
     */
    public ChannelMetadata getChannelMetadata(int row)
    {
        if(row < mChannelMetadata.size())
        {
            return mChannelMetadata.get(row);
        }

        return null;
    }

    /**
     * Returns the channel that matches the metadata or null
     */
    public Channel getChannelFromMetadata(ChannelMetadata channelMetadata)
    {
        return mMetadataChannelMap.get(channelMetadata);
    }

    @Override
    public int getRowCount()
    {
        return mChannelMetadata.size();
    }

    @Override
    public int getColumnCount()
    {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column)
    {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        switch(columnIndex)
        {
            case COLUMN_USER_FROM:
            case COLUMN_USER_TO:
                return ChannelMetadata.class;
            case COLUMN_USER_FROM_ALIAS:
            case COLUMN_USER_TO_ALIAS:
                return Alias.class;
            case COLUMN_DECODER_LOGICAL_CHANNEL_NAME:
                return String.class;
            default:
                return Identifier.class;
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        if(rowIndex <= mChannelMetadata.size())
        {
            ChannelMetadata channelMetadata = mChannelMetadata.get(rowIndex);

            switch(columnIndex)
            {
                case COLUMN_DECODER_STATE:
                    return channelMetadata.getChannelStateIdentifier();
                case COLUMN_DECODER_TYPE:
                    return channelMetadata.getDecoderTypeConfigurationIdentifier();
                case COLUMN_DECODER_LOGICAL_CHANNEL_NAME:
                    DecoderLogicalChannelNameIdentifier id = channelMetadata.getDecoderLogicalChannelNameIdentifier();

                    if(channelMetadata.hasTimeslot())
                    {
                        if(id == null)
                        {
                            return "TS:" + channelMetadata.getTimeslot();
                        }
                        else
                        {
                            String value = id.getValue().replace(" TS0", "").replace(" TS1", "");
                            return value + " TS:" + channelMetadata.getTimeslot();
                        }
                    }
                    else
                    {
                        if(id == null)
                        {
                            return null;
                        }
                        else
                        {
                            return id.getValue();
                        }
                    }
                case COLUMN_CONFIGURATION_FREQUENCY:
                    return channelMetadata.getFrequencyConfigurationIdentifier();
                case COLUMN_CONFIGURATION_CHANNEL:
                    return channelMetadata.getChannelNameConfigurationIdentifier();
                case COLUMN_USER_TO:
                    return channelMetadata;
                case COLUMN_USER_FROM:
                    return channelMetadata;
                case COLUMN_USER_FROM_ALIAS:
                    return channelMetadata.getFromIdentifierAliases();
                case COLUMN_USER_TO_ALIAS:
                    return channelMetadata.getToIdentifierAliases();
            }
        }

        return null;
    }

    @Override
    public void updated(ChannelMetadata channelMetadata, ChannelMetadataField channelMetadataField)
    {
        final int rowIndex = mChannelMetadata.indexOf(channelMetadata);

        if(rowIndex >= 0)
        {
            //Execute on the swing thread to avoid threading issues
            EventQueue.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    switch(channelMetadataField)
                    {
                        case CONFIGURATION_CHANNEL:
                            fireTableCellUpdated(rowIndex, COLUMN_CONFIGURATION_CHANNEL);
                            break;
                        case CONFIGURATION_FREQUENCY:
                            fireTableCellUpdated(rowIndex, COLUMN_CONFIGURATION_FREQUENCY);
                            break;
                        case DECODER_CHANNEL_NAME:
                            fireTableCellUpdated(rowIndex, COLUMN_DECODER_LOGICAL_CHANNEL_NAME);
                            break;
                        case DECODER_TYPE:
                            fireTableCellUpdated(rowIndex, COLUMN_DECODER_TYPE);
                            break;
                        case DECODER_STATE:
                            fireTableCellUpdated(rowIndex, COLUMN_DECODER_STATE);
                            break;
                        case USER_FROM:
                            fireTableCellUpdated(rowIndex, COLUMN_USER_FROM);
                            fireTableCellUpdated(rowIndex, COLUMN_USER_FROM_ALIAS);
                            break;
                        case USER_TO:
                            fireTableCellUpdated(rowIndex, COLUMN_USER_TO);
                            fireTableCellUpdated(rowIndex, COLUMN_USER_TO_ALIAS);
                            break;
                    }
                }
            });
        }
    }
}
