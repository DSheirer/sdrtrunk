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
package audio.stream;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class StreamModel extends AbstractTableModel
{
    private static final int COLUMN_CHANNEL_NAME = 0;
    private static final int COLUMN_CHANNEL_FORMAT = 1;
    private static final int COLUMN_SERVER_NAME = 2;

    private List<Stream> mStreams = new ArrayList<>();

    @Override
    public int getRowCount()
    {
        return mStreams.size();
    }

    @Override
    public int getColumnCount()
    {
        return 3;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        if( rowIndex <= mStreams.size())
        {
            Stream stream = mStreams.get(rowIndex);

            if(stream != null)
            {
                switch(columnIndex)
                {
                    case COLUMN_CHANNEL_NAME:
                        return stream.getChannel().getName();
                    case COLUMN_CHANNEL_FORMAT:
                        return stream.getChannel().getSourceFormat().name();
                    case COLUMN_SERVER_NAME:
                        return stream.getServer().getName();
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
