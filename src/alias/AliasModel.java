/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package alias;

import alias.AliasEvent.Event;
import alias.id.broadcast.BroadcastChannel;
import sample.Broadcaster;
import sample.Listener;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Alias Model
 */
public class AliasModel extends AbstractTableModel
{
    private static final long serialVersionUID = 1L;

    public static final int COLUMN_LIST = 0;
    public static final int COLUMN_GROUP = 1;
    public static final int COLUMN_NAME = 2;
    public static final int COLUMN_ICON = 3;
    public static final int COLUMN_COLOR = 4;

    private List<Alias> mAliases = new CopyOnWriteArrayList<>();
    private Broadcaster<AliasEvent> mAliasEventBroadcaster = new Broadcaster<>();
    private Map<String,AliasList> mAliasListMap = new HashMap<>();

    public AliasModel()
    {
    }

    /**
     * Unmodifiable list of all aliases currently in the model
     */
    public List<Alias> getAliases()
    {
        return Collections.unmodifiableList(mAliases);
    }

    public Alias getAliasAtIndex(int row)
    {
        if(mAliases.size() >= row)
        {
            return mAliases.get(row);
        }

        return null;
    }

    public AliasList getAliasList(String name)
    {
        if(name != null && mAliasListMap.containsKey(name))
        {
            return mAliasListMap.get(name);
        }

        AliasList aliasList = new AliasList(name);

        if(name != null)
        {
            for(Alias alias : mAliases)
            {
                if(alias.hasList() && alias.getList().equalsIgnoreCase(name))
                {
                    aliasList.addAlias(alias);
                }
            }

            mAliasListMap.put(name, aliasList);

            //Register the new alias list to receive updates from this model
            addListener(aliasList);
        }

        return aliasList;
    }

    /**
     * Returns a list of unique alias list names from across the alias set
     */
    public List<String> getListNames()
    {
        List<String> listNames = new ArrayList<>();

        for(Alias alias : mAliases)
        {
            if(alias.hasList() && !listNames.contains(alias.getList()))
            {
                listNames.add(alias.getList());
            }
        }

        Collections.sort(listNames);

        return listNames;
    }

    /**
     * Returns a list of alias group names for all aliases
     */
    public List<String> getGroupNames()
    {
        List<String> groupNames = new ArrayList<>();

        for(Alias alias : mAliases)
        {
            if(alias.hasGroup() && !groupNames.contains(alias.getGroup()))
            {
                groupNames.add(alias.getGroup());
            }
        }

        Collections.sort(groupNames);

        return groupNames;
    }

    /**
     * Returns a list of alias group names for all aliases that have a matching
     * list name value
     */
    public List<String> getGroupNames(String listName)
    {
        List<String> groupNames = new ArrayList<>();

        if(listName != null)
        {
            for(Alias alias : mAliases)
            {
                if(alias.hasList() &&
                    alias.hasGroup() &&
                    listName.equals(alias.getList()) &&
                    !groupNames.contains(alias.getGroup()))
                {
                    groupNames.add(alias.getGroup());
                }
            }
        }

        Collections.sort(groupNames);

        return groupNames;
    }

    /**
     * Bulk loading of aliases
     */
    public void addAliases(List<Alias> aliases)
    {
        for(Alias alias : aliases)
        {
            addAlias(alias);
        }
    }

    /**
     * Adds the alias to the model
     */
    public int addAlias(Alias alias)
    {
        if(alias != null)
        {
            mAliases.add(alias);

            int index = mAliases.size() - 1;

            fireTableRowsInserted(index, index);

            broadcast(new AliasEvent(alias, Event.ADD));

            return index;
        }

        return -1;
    }

    /**
     * Removes the channel from the model and broadcasts a channel remove event
     */
    public void removeAlias(Alias alias)
    {
        if(alias != null)
        {
            int index = mAliases.indexOf(alias);

            mAliases.remove(alias);

            fireTableRowsDeleted(index, index);

            broadcast(new AliasEvent(alias, Event.DELETE));
        }
    }

    /**
     * Renames any broadcast channels that have the previous name.
     *
     * @param previousName to rename
     * @param newName to assign to the broadcast channel
     */
    public void renameBroadcastChannel(String previousName, String newName)
    {
        if(previousName == null || previousName.isEmpty() || newName == null || newName.isEmpty())
        {
            return;
        }

        for(Alias alias : mAliases)
        {
            if(alias.hasBroadcastChannel(previousName))
            {
                for(BroadcastChannel broadcastChannel : alias.getBroadcastChannels())
                {
                    if(broadcastChannel.getChannelName().contentEquals(previousName))
                    {
                        broadcastChannel.setChannelName(newName);
                    }
                }
            }
        }
    }

    @Override
    public int getRowCount()
    {
        return mAliases.size();
    }

    @Override
    public int getColumnCount()
    {
        return 5;
    }

    @Override
    public String getColumnName(int columnIndex)
    {
        switch(columnIndex)
        {
            case COLUMN_LIST:
                return "List";
            case COLUMN_GROUP:
                return "Group";
            case COLUMN_NAME:
                return "Name";
            case COLUMN_ICON:
                return "Icon";
            case COLUMN_COLOR:
                return "Color";
        }

        return null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        if(columnIndex == COLUMN_COLOR)
        {
            return Integer.class;
        }

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
        Alias alias = mAliases.get(rowIndex);

        switch(columnIndex)
        {
            case COLUMN_LIST:
                return alias.getList();
            case COLUMN_GROUP:
                return alias.getGroup();
            case COLUMN_NAME:
                return alias.getName();
            case COLUMN_ICON:
                return alias.getIconName();
            case COLUMN_COLOR:
                return alias.getColor();
        }

        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        throw new IllegalArgumentException("Not yet implemented");
    }

    public void broadcast(AliasEvent event)
    {
        Alias alias = event.getAlias();

        //Validate the alias following a user action that changed the alias or any alias IDs
        if(alias != null)
        {
            alias.validate();
        }

        if(event.getEvent() == Event.CHANGE)
        {
            int index = mAliases.indexOf(event.getAlias());

            fireTableRowsUpdated(index, index);
        }

        mAliasEventBroadcaster.broadcast(event);
    }

    public void addListener(Listener<AliasEvent> listener)
    {
        mAliasEventBroadcaster.addListener(listener);
    }

    public void removeListener(Listener<AliasEvent> listener)
    {
        mAliasEventBroadcaster.removeListener(listener);
    }
}
