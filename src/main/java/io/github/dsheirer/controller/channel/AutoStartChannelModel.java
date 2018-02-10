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
package io.github.dsheirer.controller.channel;

import javax.swing.table.AbstractTableModel;
import java.util.List;

/**
 * Auto-Start Channel Model
 */
public class AutoStartChannelModel extends AbstractTableModel
{
    private static final long serialVersionUID = 1L;

    public static final int COLUMN_ORDER = 0;
    public static final int COLUMN_SYSTEM = 1;
    public static final int COLUMN_SITE = 2;
    public static final int COLUMN_NAME = 3;
    public static final int COLUMN_DECODER = 4;

    private static final String[] COLUMN_NAMES = new String[] {"Order", "System", "Site", "Name", "Decoder"};

    private List<Channel> mChannels;

    public AutoStartChannelModel(List<Channel> autoStartChannels)
    {
        mChannels = autoStartChannels;
    }

    @Override
    public int getRowCount()
    {
        return mChannels.size();
    }

    @Override
    public int getColumnCount()
    {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int columnIndex)
    {
        if(columnIndex < COLUMN_NAMES.length)
        {
            return COLUMN_NAMES[columnIndex];
        }

        return null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        Channel channel = mChannels.get(rowIndex);

        switch(columnIndex)
        {
            case COLUMN_ORDER:
                return channel.getAutoStartOrder();
            case COLUMN_SYSTEM:
                return channel.getSystem();
            case COLUMN_SITE:
                return channel.getSite();
            case COLUMN_NAME:
                return channel.getName();
            case COLUMN_DECODER:
                return channel.getDecodeConfiguration().getDecoderType().getShortDisplayString();
        }

        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        throw new IllegalArgumentException("Not yet implemented");
    }
}
