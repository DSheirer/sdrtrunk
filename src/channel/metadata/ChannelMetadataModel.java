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

import controller.channel.Channel;
import sample.Listener;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChannelMetadataModel extends AbstractTableModel implements Listener<MetadataChangeEvent>
{
    public static final int COLUMN_STATE = 0;
    public static final int COLUMN_PRIMARY = 1;
    public static final int COLUMN_SECONDARY = 2;
    public static final int COLUMN_MESSAGE = 3;
    public static final int COLUMN_NETWORK = 4;
    public static final int COLUMN_FREQUENCY = 5;
    public static final int COLUMN_CONFIGURATION = 6;

    private static final String[] COLUMNS =
        {"State", "Primary", "Secondary", "Message", "Network", "Frequency", "Configuration"};

    private List<Metadata> mChannelMetadata = new ArrayList();
    private Map<Metadata,Channel> mMetadataChannelMap = new HashMap();

    public void add(Metadata metadata, Channel channel)
    {
        mChannelMetadata.add(metadata);
        mMetadataChannelMap.put(metadata, channel);

        int index = mChannelMetadata.indexOf(metadata);

        fireTableRowsInserted(index, index);

        metadata.addListener(this);
    }

    public void remove(Metadata metadata)
    {
        metadata.removeListener(this);

        int index = mChannelMetadata.indexOf(metadata);

        mChannelMetadata.remove(metadata);
        mMetadataChannelMap.remove(metadata);

        fireTableRowsDeleted(index, index);
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
        return Metadata.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        return mChannelMetadata.get(rowIndex);
    }

    @Override
    public void receive(MetadataChangeEvent metadataChangeEvent)
    {
        int rowIndex = mChannelMetadata.indexOf(metadataChangeEvent.getMetadata());

        if(rowIndex >= 0)
        {
            switch(metadataChangeEvent.getAttribute())
            {
                case CHANNEL_CONFIGURATION_LABEL_1:
                case CHANNEL_CONFIGURATION_LABEL_2:
                    fireTableCellUpdated(rowIndex, COLUMN_CONFIGURATION);
                    break;
                case CHANNEL_FREQUENCY:
                case CHANNEL_ID:
                    fireTableCellUpdated(rowIndex, COLUMN_FREQUENCY);
                    break;
                case CHANNEL_STATE:
                    fireTableCellUpdated(rowIndex, COLUMN_STATE);
                    break;
                case MESSAGE:
                case MESSAGE_TYPE:
                    fireTableCellUpdated(rowIndex, COLUMN_MESSAGE);
                    break;
                case NETWORK_ID_1:
                case NETWORK_ID_2:
                    fireTableCellUpdated(rowIndex, COLUMN_NETWORK);
                    break;
                case PRIMARY_ADDRESS_FROM:
                case PRIMARY_ADDRESS_TO:
                    fireTableCellUpdated(rowIndex, COLUMN_PRIMARY);
                    break;
                case SECONDARY_ADDRESS_FROM:
                case SECONDARY_ADDRESS_TO:
                    fireTableCellUpdated(rowIndex, COLUMN_SECONDARY);
                    break;
            }
        }
    }
}
