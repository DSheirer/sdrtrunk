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

import sample.Listener;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BroadcastModel extends AbstractTableModel
{
    private static final int COLUMN_CHANNEL_NAME = 0;
    private static final int COLUMN_SERVER_TYPE = 1;
    private static final int COLUMN_SERVER_ADDRESS = 2;

    private List<BroadcastConfiguration> mBroadcastConfigurations = new CopyOnWriteArrayList<>();
    private sample.Broadcaster<BroadcastConfigurationEvent> mBroadcastEventBroadcaster = new sample.Broadcaster<>();

    public BroadcastModel()
    {
    }

    /**
     * List of broadcast configuration names
     */
    public List<String> getBroadcastConfigurationNames()
    {
        List<String> names = new ArrayList<>();

        for(BroadcastConfiguration configuration: mBroadcastConfigurations)
        {
            names.add(configuration.getName());
        }

        return names;
    }

    /**
     * Current list of broadcast configurations
     */
    public List<BroadcastConfiguration> getBroadcastConfigurations()
    {
        return mBroadcastConfigurations;
    }

    /**
     * Adds the list of broadcast configurations to this model
     */
    public void addBroadcastConfigurations(List<BroadcastConfiguration> configurations)
    {
        for(BroadcastConfiguration configuration: configurations)
        {
            addBroadcastConfiguration(configuration);
        }
    }

    /**
     * Adds the broadcast configuration to this model
     */
    public void addBroadcastConfiguration(BroadcastConfiguration configuration)
    {
        if(configuration != null)
        {
            mBroadcastConfigurations.add(configuration);

            int index = mBroadcastConfigurations.size() - 1;

            fireTableRowsInserted( index, index );

            broadcast( new BroadcastConfigurationEvent( configuration, BroadcastConfigurationEvent.Event.ADD ) );
        }
    }

    public void removeBroadcastConfiguration(BroadcastConfiguration configuration)
    {
        if(configuration != null && mBroadcastConfigurations.contains(configuration))
        {
            int index = mBroadcastConfigurations.indexOf(configuration);

            mBroadcastConfigurations.remove(configuration);

            fireTableRowsDeleted( index, index );

            broadcast( new BroadcastConfigurationEvent( configuration, BroadcastConfigurationEvent.Event.DELETE ) );
        }
    }

    /**
     * Registers the listener to receive broadcast configuration events
     */
    public void addListener(Listener<BroadcastConfigurationEvent> listener)
    {
        mBroadcastEventBroadcaster.addListener(listener);
    }

    /**
     * Removes the listener from receiving broadcast configuration events
     */
    public void removeListener(Listener<BroadcastConfigurationEvent> listener)
    {
        mBroadcastEventBroadcaster.removeListener(listener);
    }

    /**
     * Broadcasts the broadcast configuration change event
     */
    public void broadcast( BroadcastConfigurationEvent event )
    {
        if( event.getEvent() == BroadcastConfigurationEvent.Event.CHANGE )
        {
            int index = mBroadcastConfigurations.indexOf( event.getBroadcastConfiguration() );

            fireTableRowsUpdated( index, index );
        }

        mBroadcastEventBroadcaster.broadcast( event );
    }

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
                        return configuration.getName();
                    case COLUMN_SERVER_TYPE:
                        return configuration.getBroadcastServerType().toString();
                    case COLUMN_SERVER_ADDRESS:
                        return configuration.getHost();
                    default:
                        break;
                }
            }
        }

        return null;
    }

    /**
     * Broadcast configuration at the specified model row
     */
    public BroadcastConfiguration getConfigurationAt(int rowIndex)
    {
        return mBroadcastConfigurations.get(rowIndex);
    }

    /**
     * Model row number for the specified configuration
     */
    public int getRowForConfiguration(BroadcastConfiguration configuration)
    {
        return mBroadcastConfigurations.indexOf(configuration);
    }

    @Override
    public String getColumnName(int column)
    {
        switch(column)
        {
            case COLUMN_CHANNEL_NAME:
                return "Channel";
            case COLUMN_SERVER_TYPE:
                return "Server";
            case COLUMN_SERVER_ADDRESS:
                return "Address";
        }

        return null;
    }
}
