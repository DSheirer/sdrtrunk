/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.channel.metadata;

import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;
import java.awt.EventQueue;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChannelMetadataModel extends AbstractTableModel implements Listener<MutableMetadataChangeEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelMetadataModel.class);

    private final DecimalFormat FREQUENCY_FORMATTER = new DecimalFormat( "#.00000" );

    public static final int COLUMN_STATE = 0;
    public static final int COLUMN_DECODER = 1;
    public static final int COLUMN_CHANNEL = 2;
    public static final int COLUMN_FREQUENCY = 3;
    public static final int COLUMN_PRIMARY_FROM = 4;
    public static final int COLUMN_PRIMARY_TO = 5;
    public static final int COLUMN_SECONDARY_FROM = 6;
    public static final int COLUMN_SECONDARY_TO = 7;
    public static final int COLUMN_CONFIGURATION_NAME = 8;
    public static final int COLUMN_MESSAGE = 9;

    private static final String[] COLUMNS = {"Status", "Decoder", "Channel", "Frequency", "Primary From", "Primary To",
         "Secondary From", "Secondary To", "Channel Name", "Message"};

    private List<MutableMetadata> mChannelMetadata = new ArrayList();
    private Map<MutableMetadata,Channel> mMetadataChannelMap = new HashMap();

    public void add(MutableMetadata metadata, Channel channel)
    {
        //Execute on the swing thread to avoid threading issues
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                mChannelMetadata.add(metadata);
                mMetadataChannelMap.put(metadata, channel);

                int index = mChannelMetadata.indexOf(metadata);

                fireTableRowsInserted(index, index);

                metadata.addListener(ChannelMetadataModel.this);
            }
        });
    }

    public void remove(MutableMetadata metadata)
    {
        //Execute on the swing thread to avoid threading issues
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                metadata.removeListener(ChannelMetadataModel.this);

                int index = mChannelMetadata.indexOf(metadata);

                mChannelMetadata.remove(metadata);
                mMetadataChannelMap.remove(metadata);

                fireTableRowsDeleted(index, index);
            }
        });
    }

    /**
     * Get the channel metadata at the specified model row index
     */
    public Metadata getMetadata(int row)
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
    public Channel getChannelFromMetadata(Metadata metadata)
    {
        return mMetadataChannelMap.get(metadata);
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
            case COLUMN_STATE:
                return State.class;

            case COLUMN_DECODER:
            case COLUMN_CHANNEL:
            case COLUMN_FREQUENCY:
            case COLUMN_MESSAGE:
            case COLUMN_CONFIGURATION_NAME:
                return String.class;

            case COLUMN_PRIMARY_TO:
            case COLUMN_PRIMARY_FROM:
            case COLUMN_SECONDARY_TO:
            case COLUMN_SECONDARY_FROM:
                return MutableMetadata.class;

            default:
                return String.class;
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        if(rowIndex <= mChannelMetadata.size())
        {
            Metadata metadata = mChannelMetadata.get(rowIndex);

            switch(columnIndex)
            {
                case COLUMN_STATE:
                    return metadata.getState();
                case COLUMN_DECODER:
                    if(metadata.hasPrimaryDecoderType())
                    {
                        return metadata.getPrimaryDecoderType().getShortDisplayString();
                    }

                    return null;
                case COLUMN_CHANNEL:
                    return metadata.getChannelFrequencyLabel();
                case COLUMN_FREQUENCY:
                    if(metadata.hasChannelFrequency())
                    {
                        return FREQUENCY_FORMATTER.format((double)metadata.getChannelFrequency() / 1E6d);
                    }

                    return null;
                case COLUMN_MESSAGE:
                    if(metadata.isBufferOverflow())
                    {
                        return "**OVERFLOW**";
                    }
                    else if(metadata.hasMessageType() || metadata.hasMessage())
                    {
                        StringBuilder sb = new StringBuilder();
                        if(metadata.hasMessageType())
                        {
                            sb.append(metadata.getMessageType()).append(" ");
                        }

                        if(metadata.hasMessage())
                        {
                            sb.append(metadata.getMessage());
                        }

                        return sb.toString();
                    }

                    return null;
                case COLUMN_CONFIGURATION_NAME:
                    return metadata.getChannelConfigurationName();
                case COLUMN_PRIMARY_TO:
                case COLUMN_PRIMARY_FROM:
                case COLUMN_SECONDARY_TO:
                case COLUMN_SECONDARY_FROM:
                    return metadata;

                default:
                    return String.class;
            }
        }

        return null;
    }

    @Override
    public void receive(MutableMetadataChangeEvent mutableMetadataChangeEvent)
    {
        //Execute on the swing thread to avoid threading issues
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                int rowIndex = mChannelMetadata.indexOf(mutableMetadataChangeEvent.getMetadata());

                if(rowIndex >= 0)
                {
                    switch(mutableMetadataChangeEvent.getAttribute())
                    {
                        case CHANNEL_CONFIGURATION_SYSTEM:
                            break;
                        case CHANNEL_CONFIGURATION_SITE:
                            break;
                        case CHANNEL_CONFIGURATION_NAME:
                            fireTableCellUpdated(rowIndex, COLUMN_CONFIGURATION_NAME);
                            break;
                        case CHANNEL_FREQUENCY:
                            fireTableCellUpdated(rowIndex, COLUMN_FREQUENCY);
                            break;
                        case CHANNEL_FREQUENCY_LABEL:
                            fireTableCellUpdated(rowIndex, COLUMN_CHANNEL);
                            break;
                        case CHANNEL_STATE:
                            fireTableCellUpdated(rowIndex, COLUMN_STATE);
                            break;
                        case BUFFER_OVERFLOW:
                        case MESSAGE:
                        case MESSAGE_TYPE:
                            fireTableCellUpdated(rowIndex, COLUMN_MESSAGE);
                            break;
                        case PRIMARY_ADDRESS_FROM:
                            fireTableCellUpdated(rowIndex, COLUMN_PRIMARY_FROM);
                            break;
                        case PRIMARY_ADDRESS_TO:
                            fireTableCellUpdated(rowIndex, COLUMN_PRIMARY_TO);
                            break;
                        case PRIMARY_DECODER_TYPE:
                            fireTableCellUpdated(rowIndex, COLUMN_DECODER);
                            break;
                        case SECONDARY_ADDRESS_FROM:
                            fireTableCellUpdated(rowIndex, COLUMN_SECONDARY_FROM);
                            break;
                        case SECONDARY_ADDRESS_TO:
                            fireTableCellUpdated(rowIndex, COLUMN_SECONDARY_TO);
                            break;
                    }
                }
            }
        });
    }
}
