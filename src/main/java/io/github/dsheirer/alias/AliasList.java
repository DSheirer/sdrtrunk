/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
package io.github.dsheirer.alias;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.alias.id.esn.Esn;
import io.github.dsheirer.alias.id.priority.Priority;
import io.github.dsheirer.alias.id.status.StatusID;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.alias.id.talkgroup.TalkgroupRange;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.esn.ESNIdentifier;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.status.UnitStatusIdentifier;
import io.github.dsheirer.identifier.status.UserStatusIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AliasList implements Listener<AliasEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(AliasList.class);
    private Map<Protocol,TalkgroupAliasList> mTalkgroupProtocolMap = new HashMap<>();
    private Map<String,Alias> mESNMap = new HashMap<>();
    private Map<Integer,Alias> mStatusMap = new HashMap<>();
    private boolean mHasAliasActions = false;
    private String mName;

    /**
     * List of aliases where all aliases share the same list name.  Contains
     * several methods for alias lookup from identifier values, like talkgroups.
     *
     * Responds to alias change events to keep the internal alias list updated.
     */
    public AliasList(String name)
    {
        mName = name;
    }

    /**
     * Adds the alias to this list
     */
    public void addAlias(Alias alias)
    {
        if(alias != null)
        {
            for(AliasID aliasID : alias.getId())
            {
                addAliasID(aliasID, alias);
            }
        }

        if(alias.hasActions())
        {
            mHasAliasActions = true;
        }
    }

    /**
     * Adds the alias and alias identifier to the internal type mapping.
     */
    private void addAliasID(AliasID id, Alias alias)
    {
        if(id.isValid())
        {
            try
            {
                switch(id.getType())
                {
                    case TALKGROUP:
                        Talkgroup talkgroup = (Talkgroup)id;

                        TalkgroupAliasList talkgroupAliasList = mTalkgroupProtocolMap.get(talkgroup.getProtocol());

                        if(talkgroupAliasList == null)
                        {
                            talkgroupAliasList = new TalkgroupAliasList();
                            mTalkgroupProtocolMap.put(talkgroup.getProtocol(), talkgroupAliasList);
                        }

                        talkgroupAliasList.add(talkgroup, alias);
                        break;
                    case TALKGROUP_RANGE:
                        TalkgroupRange talkgroupRange = (TalkgroupRange)id;

                        TalkgroupAliasList talkgroupRangeAliasList = mTalkgroupProtocolMap.get(talkgroupRange.getProtocol());

                        if(talkgroupRangeAliasList == null)
                        {
                            talkgroupRangeAliasList = new TalkgroupAliasList();
                            mTalkgroupProtocolMap.put(talkgroupRange.getProtocol(), talkgroupRangeAliasList);
                        }

                        talkgroupRangeAliasList.add(talkgroupRange, alias);
                        break;
                    case ESN:
                        String esn = ((Esn) id).getEsn();

                        if(esn != null && !esn.isEmpty())
                        {
                            mESNMap.put(esn.toLowerCase(), alias);
                        }
                        break;
                    case STATUS:
                        mStatusMap.put(((StatusID) id).getStatus(), alias);
                        break;
                }
            }
            catch(Exception e)
            {
                mLog.error("Couldn't add alias ID " + id + " for alias " + alias);
            }
        }
    }

    /**
     * Removes the alias from this list
     */
    public void removeAlias(Alias alias)
    {
        for(TalkgroupAliasList talkgroupAliasList: mTalkgroupProtocolMap.values())
        {
            talkgroupAliasList.remove(alias);
        }

        remove(alias, mStatusMap);
        remove(alias, mESNMap);
    }

    /**
     * Lookup alias by ESN
     */
    public Alias getESNAlias(String esn)
    {
        Alias alias = null;

        if(esn != null)
        {
            alias = mESNMap.get(esn);
        }

        return alias;
    }

    /**
     * Alias list name
     */
    public String toString()
    {
        return mName;
    }

    /**
     * Alias list name
     */
    @JacksonXmlProperty(isAttribute = true, localName = "name")
    public String getName()
    {
        return mName;
    }

    /**
     * Indicates if this alias list has a non-null, non-empty name
     */
    private boolean hasName()
    {
        return mName != null && !mName.isEmpty();
    }

    /**
     * Receive alias change event notifications and modify this list accordingly
     */
    @Override
    public void receive(AliasEvent event)
    {
        if(hasName())
        {
            Alias alias = event.getAlias();

            switch(event.getEvent())
            {
                case ADD:
                    if(alias.getList() != null && getName().equalsIgnoreCase(alias.getList()))
                    {
                        addAlias(alias);
                    }
                    break;
                case CHANGE:
                    if(alias.getList() != null && getName().equalsIgnoreCase(alias.getList()))
                    {
                        removeAlias(alias);
                        addAlias(alias);
                    }
                    break;
                case DELETE:
                    if(alias.getList() != null && getName().equalsIgnoreCase(alias.getList()))
                    {
                        removeAlias(alias);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Returns an optional alias that is associated with the identifier
      * @param identifier to alias
     * @return list of alias or empty list
     */
    public List<Alias> getAliases(Identifier identifier)
    {
        if(identifier != null)
        {
            switch(identifier.getForm())
            {
                case TALKGROUP:
                    TalkgroupIdentifier talkgroup = (TalkgroupIdentifier)identifier;

                    TalkgroupAliasList talkgroupAliasList = mTalkgroupProtocolMap.get(identifier.getProtocol());

                    if(talkgroupAliasList != null)
                    {
                        Alias alias = talkgroupAliasList.getAlias(talkgroup);

                        if(alias != null)
                        {
                            List<Alias> aliases = new ArrayList<>();
                            aliases.add(alias);
                            return aliases;
                        }
                    }
                    break;
                case PATCH_GROUP:
                    PatchGroupIdentifier patchGroupIdentifier = (PatchGroupIdentifier)identifier;
                    PatchGroup patchGroup = patchGroupIdentifier.getValue();

                    TalkgroupAliasList patchGroupAliasList = mTalkgroupProtocolMap.get(patchGroupIdentifier.getProtocol());

                    if(patchGroupAliasList != null)
                    {
                        List<Alias> aliases = new ArrayList<>();

                        Alias alias = patchGroupAliasList.getAlias(patchGroup.getPatchGroup());

                        if(alias != null)
                        {
                            aliases.add(alias);
                        }

                        for(TalkgroupIdentifier patchedGroup: patchGroup.getPatchedGroupIdentifiers())
                        {
                            Alias patchedAlias = patchGroupAliasList.getAlias(patchedGroup);

                            if(patchedAlias != null && !aliases.contains(patchedAlias))
                            {
                                aliases.add(patchedAlias);
                            }
                        }

                        return aliases;
                    }
                    break;
                case ESN:
                    if(identifier instanceof ESNIdentifier)
                    {
                        Alias alias = getESNAlias(((ESNIdentifier)identifier).getValue());

                        if(alias != null)
                        {
                            List<Alias> aliases = new ArrayList<>();
                            aliases.add(alias);
                            return aliases;
                        }
                    }
                    break;
                case UNIT_STATUS:
                    if(identifier instanceof UnitStatusIdentifier)
                    {
                        int status = ((UnitStatusIdentifier)identifier).getValue();

                        Alias alias = mStatusMap.get(status);

                        if(alias != null)
                        {
                            List<Alias> aliases = new ArrayList<>();
                            aliases.add(alias);
                            return aliases;
                        }
                    }
                    break;
                case USER_STATUS:
                    if(identifier instanceof UserStatusIdentifier)
                    {
                        int status = ((UserStatusIdentifier)identifier).getValue();

                        Alias alias = mStatusMap.get(status);

                        if(alias != null)
                        {
                            List<Alias> aliases = new ArrayList<>();
                            aliases.add(alias);
                            return aliases;
                        }
                    }
                    break;
            }
        }

        return Collections.EMPTY_LIST;
    }

    /**
     * Indicates if any of the identifiers contain a broadcast channel for streaming of audio.
     * @param identifierCollection to inspect
     * @return true if the identifier collection is designated for streaming to one or more channels.
     */
    public boolean isStreamable(IdentifierCollection identifierCollection)
    {
        for(Identifier identifier: identifierCollection.getIdentifiers())
        {
            List<Alias> aliases = getAliases(identifier);

            for(Alias alias: aliases)
            {
                if(alias != null && alias.isStreamable())
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Indicates if any of the identifiers have been identified for recording.
     * @param identifierCollection to inspect
     * @return true if recordable.
     */
    public boolean isRecordable(IdentifierCollection identifierCollection)
    {
        for(Identifier identifier: identifierCollection.getIdentifiers())
        {
            List<Alias> aliases = getAliases(identifier);

            for(Alias alias: aliases)
            {
                if(alias != null && alias.isRecordable())
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Indicates if any of the aliases in this list have an associated alias action
     */
    public boolean hasAliasActions()
    {
        return mHasAliasActions;
    }

    /**
     * Returns the lowest audio playback priority specified by aliases for identifiers in the
     * identifier collection.
     *
     * @param identifierCollection to inspect for audio priority
     * @return audio playback priority
     */
    public int getAudioPlaybackPriority(IdentifierCollection identifierCollection)
    {
        int priority = Priority.DEFAULT_PRIORITY;

        for(Identifier identifier: identifierCollection.getIdentifiers())
        {
            List<Alias> aliases = getAliases(identifier);

            for(Alias alias: aliases)
            {
                if(alias != null && alias.getPlaybackPriority() < priority)
                {
                    priority = alias.getPlaybackPriority();
                }
            }
        }

        return priority;
    }

    /**
     * Returns a list of streaming broadcast channels specified for any of the identifiers in the collection.
     *
     * @return list of broadcast channels or an empty list
     */
    public List<BroadcastChannel> getBroadcastChannels(IdentifierCollection identifierCollection)
    {
        List<BroadcastChannel> channels = new ArrayList<>();

        for(Identifier identifier: identifierCollection.getIdentifiers())
        {
            List<Alias> aliases = getAliases(identifier);

            for(Alias alias: aliases)
            {
                if(alias != null && alias.isStreamable())
                {
                    for(BroadcastChannel broadcastChannel: alias.getBroadcastChannels())
                    {
                        if(!channels.contains(broadcastChannel))
                        {
                            channels.add(broadcastChannel);
                        }
                    }
                }
            }
        }

        return channels;
    }

    /**
     * Removes the alias (as a value) from the specified map
     */
    public static void remove(Alias alias, Map map)
    {
        Iterator<Map.Entry> it = map.entrySet().iterator();

        while(it.hasNext())
        {
            if(it.next().getValue().equals(alias))
            {
                it.remove();
            }
        }
    }


    /**
     * Listing of talkgroups and ranges for a specific protocol
     */
    public class TalkgroupAliasList
    {
        private Map<Integer,Alias> mTalkgroupAliasMap = new TreeMap<>();
        private Map<TalkgroupRange, Alias> mTalkgroupRangeAliasMap = new HashMap<>();

        public TalkgroupAliasList()
        {
        }

        public Alias getAlias(TalkgroupIdentifier identifier)
        {
            int value = identifier.getValue();

            if(mTalkgroupAliasMap.containsKey(value))
            {
                return mTalkgroupAliasMap.get(value);
            }

            for(TalkgroupRange talkgroupRange: mTalkgroupRangeAliasMap.keySet())
            {
                if(talkgroupRange.contains(value))
                {
                    return mTalkgroupRangeAliasMap.get(talkgroupRange);
                }
            }

            return null;
        }

        public void add(Talkgroup talkgroup, Alias alias)
        {
            mTalkgroupAliasMap.put(talkgroup.getValue(), alias);
        }

        public void add(TalkgroupRange talkgroupRange, Alias alias)
        {
            mTalkgroupRangeAliasMap.put(talkgroupRange, alias);
        }

        public void remove(Talkgroup talkgroup)
        {
            mTalkgroupAliasMap.remove(talkgroup.getValue());
        }

        public void remove(TalkgroupRange talkgroupRange)
        {
            mTalkgroupRangeAliasMap.remove(talkgroupRange);
        }

        /**
         * Removes the alias from all internal maps
         */
        public void remove(Alias alias)
        {
            AliasList.remove(alias, mTalkgroupAliasMap);
            AliasList.remove(alias, mTalkgroupRangeAliasMap);
        }
    }
}
