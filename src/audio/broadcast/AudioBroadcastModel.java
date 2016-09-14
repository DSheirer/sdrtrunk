/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
package audio.broadcast;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class AudioBroadcastModel extends AbstractTableModel
{
    private static final int COLUMN_CHANNEL_NAME = 0;
    private static final int COLUMN_CHANNEL_FORMAT = 1;
    private static final int COLUMN_SERVER_NAME = 2;

    private List<BroadcastConfiguration> mBroadcastConfigurations = new ArrayList<>();

    @Override
    public int getRowCount()
    {
        return mBroadcastConfigurations.size();
    }

    @Override
    public int getColumnCount()
    {
        return 3;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        if( rowIndex <= mBroadcastConfigurations.size())
        {
            BroadcastConfiguration configuration = mBroadcastConfigurations.get(rowIndex);

            if(configuration != null)
            {
                switch(columnIndex)
                {
                    case COLUMN_CHANNEL_NAME:
                        return configuration.getAlias();
                    case COLUMN_CHANNEL_FORMAT:
                        return configuration.getBroadcastFormat().name();
                    case COLUMN_SERVER_NAME:
                        return configuration.getHost();
                    default:
                        break;
                }
            }
        }

        return null;
    }

    @Override
    public String getColumnName(int column)
    {
        switch(column)
        {
            case COLUMN_CHANNEL_NAME:
                return "Channel";
            case COLUMN_CHANNEL_FORMAT:
                return "Format";
            case COLUMN_SERVER_NAME:
                return "Server";
        }

        return null;
    }
}
