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
package io.github.dsheirer.alias;

import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.configuration.AliasListConfigurationIdentifier;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Alias Model contains all aliases and is responsible for creation and management of alias lists.  Alias lists are a
 * set of aliases that all share a common alias list name and can be attached to a decoding channel for aliasing
 * identifiers produced by channel decoder(s).
 */
public class AliasModel
{
    private final static Logger mLog = LoggerFactory.getLogger(AliasModel.class);
    public static final String NO_ALIAS_LIST = "(No Alias List)";
    private ObservableList<Alias> mAliases = FXCollections.observableArrayList(Alias.extractor());
    private ObservableList<String> mAliasListNames = FXCollections.observableArrayList();
    private Broadcaster<AliasEvent> mAliasEventBroadcaster = new Broadcaster<>();
    private Map<String,AliasList> mAliasListMap = new HashMap<>();

    public AliasModel()
    {
        //Register a listener to detect alias changes and broadcast change events to cause playlist save requests
        mAliases.addListener(new AliasListChangeListener());
    }

    public ObservableList<Alias> aliasList()
    {
        return mAliases;
    }

    public ObservableList<String> aliasListNames()
    {
        return mAliasListNames;
    }

    /**
     * Unmodifiable list of all aliases currently in the model
     */
    public List<Alias> getAliases()
    {
        return Collections.unmodifiableList(mAliases);
    }

    /**
     * Removes all aliases from the list and broadcasts the alias delete event for each
     */
    public void clear()
    {
        List<Alias> aliasToRemove = new ArrayList<>(mAliases);

        for(Alias alias: aliasToRemove)
        {
            removeAlias(alias);
        }
    }

    /**
     * Returns an optional alias list associated with the identifier collection
     *
     * @return alias list or null
     */
    public AliasList getAliasList(IdentifierCollection identifierCollection)
    {
        if(identifierCollection != null)
        {
            return getAliasList(identifierCollection.getAliasListConfiguration());
        }

        return null;
    }

    /**
     * Retrieves an alias list specified by the alias list configuration identifier
     *
     * @param configurationIdentifier containing the name of an alias list
     * @return alias list or null.
     */
    public AliasList getAliasList(AliasListConfigurationIdentifier configurationIdentifier)
    {
        if(configurationIdentifier != null && configurationIdentifier.isValid())
        {
            return getAliasList(configurationIdentifier.getValue());
        }

        return null;
    }

    /**
     * Creates a new alias list containing all aliases that match the alias name, or returns a previously created and
     * cached alias list.  Returned alias list is automatically registered as a listener to this model so that any
     * updates to the list by the user will automatically be reflected in constructed alias lists.
     */
    public AliasList getAliasList(String name)
    {
        if(name == null || name.isEmpty())
        {
            return new AliasList(name);
        }

        AliasList mapValue = mAliasListMap.get(name);
        if (mapValue != null) {
            return mapValue;
        }

        AliasList aliasList = new AliasList(name);

        for(Alias alias : mAliases)
        {
            if(alias.hasList() && alias.getAliasListName().equalsIgnoreCase(name))
            {
                aliasList.addAlias(alias);
            }
        }

        mAliasListMap.put(name, aliasList);

        //Register the new alias list to receive updates from this model
        addListener(aliasList);

        return aliasList;
    }

    /**
     * Returns a list of unique alias list names from across the alias set
     */
    public List<String> getListNames()
    {
        return mAliasListNames;
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
            addAliasList(alias.getAliasListName());
            int index = mAliases.size() - 1;
            broadcast(new AliasEvent(alias, AliasEvent.Event.ADD));
            return index;
        }

        return -1;
    }

    public void addAliasList(String aliasListName)
    {
        if(aliasListName != null && !aliasListName.isEmpty())
        {
            if(!mAliasListNames.contains(aliasListName))
            {
                mAliasListNames.add(aliasListName);
                FXCollections.sort(mAliasListNames);
            }
        }
        else if(!mAliasListNames.contains(NO_ALIAS_LIST))
        {
            //This list allows users to view unassigned aliases so that they can move them to a valid alias list, but
            // it is not assignable to a channel
            mAliasListNames.add(NO_ALIAS_LIST);
        }
    }

    /**
     * Removes the channel from the model and broadcasts a channel remove event
     */
    public void removeAlias(Alias alias)
    {
        if(alias != null)
        {
            mAliases.remove(alias);
            broadcast(new AliasEvent(alias, AliasEvent.Event.DELETE));
        }
    }

    /**
     * Retrieves all aliases that match the alias list and have at least one alias ID of the specified type
     * @param aliasListName to search
     * @param type to find
     * @return list of aliases
     */
    public List<Alias> getAliases(String aliasListName, AliasIDType type)
    {
        List<Alias> aliases = new ArrayList<>();

        for(Alias alias : mAliases)
        {
            if(alias.hasList() && alias.getAliasListName().equalsIgnoreCase(aliasListName))
            {
                for(AliasID aliasID: alias.getAliasIdentifiers())
                {
                    if(aliasID.getType() == type)
                    {
                        aliases.add(alias);
                        break;
                    }
                }
            }
        }

        return aliases;
    }

    /**
     * Indicates that one or more of the aliases managed by this model are configured to stream to the specified
     * broadcast channel argument.
     * @param broadcastChannel to check
     * @return true if the broadcast channel is non-null, non-empty and at least one alias is configured to stream to
     * the specified stream name.
     */
    public boolean hasAliasesWithBroadcastChannel(String broadcastChannel)
    {
        if(broadcastChannel == null || broadcastChannel.isEmpty())
        {
            return false;
        }

        for(Alias alias: mAliases)
        {
            if(alias.hasBroadcastChannel(broadcastChannel))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Updates all aliases configured to stream to the previousStreamName with the updatedStreamName
     * @param previousStreamName to be removed
     * @param updatedStreamName to be added
     */
    public void updateBroadcastChannel(String previousStreamName, String updatedStreamName)
    {
        if(previousStreamName == null || previousStreamName.isEmpty() || updatedStreamName == null || updatedStreamName.isEmpty())
        {
            return;
        }

        for(Alias alias: mAliases)
        {
            if(alias.hasBroadcastChannel(previousStreamName))
            {
                for(BroadcastChannel broadcastChannel: alias.getBroadcastChannels())
                {
                    if(broadcastChannel.getChannelName().contentEquals(previousStreamName))
                    {
                        alias.removeAliasID(broadcastChannel);

                        if(!alias.hasBroadcastChannel(updatedStreamName))
                        {
                            alias.addAliasID(new BroadcastChannel(updatedStreamName));
                        }
                    }
                }
            }
        }
    }

    public void broadcast(AliasEvent event)
    {
        Alias alias = event.getAlias();

        //Validate the alias following a user action that changed the alias or any alias IDs
        if(alias != null)
        {
            alias.validate();
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


    /**
     * Observable list change listener for both channels and traffic channels lists
     */
    public class AliasListChangeListener implements ListChangeListener<Alias>
    {
        @Override
        public void onChanged(ListChangeListener.Change<? extends Alias> change)
        {
            while(change.next())
            {
                if(change.wasAdded())
                {
                    for(Alias alias: change.getAddedSubList())
                    {
                        mAliasEventBroadcaster.broadcast(new AliasEvent(alias, AliasEvent.Event.ADD));
                    }
                }
                else if(change.wasRemoved())
                {
                    for(Alias alias: change.getRemoved())
                    {
                        mAliasEventBroadcaster.broadcast(new AliasEvent(alias, AliasEvent.Event.DELETE));
                    }
                }
                else if(change.wasUpdated())
                {
                    for(int x = change.getFrom(); x < change.getTo(); x++)
                    {
                        mAliasEventBroadcaster.broadcast(new AliasEvent(change.getList().get(x), AliasEvent.Event.CHANGE));
                    }
                }
            }
        }
    }
}
