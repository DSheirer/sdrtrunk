/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
import io.github.dsheirer.alias.id.WildcardID;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.alias.id.esn.Esn;
import io.github.dsheirer.alias.id.priority.Priority;
import io.github.dsheirer.alias.id.siteID.SiteID;
import io.github.dsheirer.alias.id.status.StatusID;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.alias.id.talkgroup.TalkgroupRange;
import io.github.dsheirer.alias.id.uniqueID.UniqueID;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.esn.ESNIdentifier;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AliasList implements Listener<AliasEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(AliasList.class);
    public static final String WILDCARD = "*";

    private Map<String,Alias> mESN = new HashMap<>();
    private Map<String,Alias> mSiteID = new HashMap<>();
    private Map<Integer,Alias> mStatus = new HashMap<>();
    private Map<Integer,Alias> mUniqueID = new HashMap<>();

    private List<WildcardID> mESNWildcards = new ArrayList<>();
    private List<WildcardID> mSiteWildcards = new ArrayList<>();
    private boolean mHasAliasActions = false;

    private Map<Protocol,TalkgroupAliasList> mTalkgroupProtocolMap = new HashMap<>();

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

                        if(esn != null)
                        {
                            if(esn.contains(WILDCARD))
                            {
                                mESNWildcards.add(new WildcardID(esn));
                                Collections.sort(mESNWildcards);
                            }

                            mESN.put(esn, alias);
                        }
                        break;
                    case LTR_NET_UID:
                        mUniqueID.put(((UniqueID) id).getUid(), alias);
                        break;
                    case SITE:
                        String siteID = ((SiteID) id).getSite();

                        if(siteID != null)
                        {
                            if(siteID.contains(WILDCARD))
                            {
                                mSiteWildcards.add(new WildcardID(siteID));
                                Collections.sort(mSiteWildcards);
                            }

                            mSiteID.put(siteID, alias);
                        }
                        break;
                    case STATUS:
                        mStatus.put(((StatusID) id).getStatus(), alias);
                        break;
                    case BROADCAST_CHANNEL:
                    case NON_RECORDABLE:
                    case RECORD:
                    case PRIORITY:
                        //We don't maintain lookups for these items
                        break;
                    default:
                        mLog.warn("Unrecognized Alias ID Type:" + id.getType().name() + " - can't ADD to lookup table");
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
        if(alias != null)
        {
            for(AliasID aliasID : alias.getId())
            {
                removeAliasID(aliasID, alias);
            }
        }
    }

    private void removeWildcard(String value, List<WildcardID> wildcards)
    {
        if(value != null)
        {
            for(WildcardID wildcardID : wildcards)
            {
                if(wildcardID.value().equals(value))
                {
                    wildcards.remove(wildcardID);
                }
            }
        }
    }

    /**
     * Removes the alias and alias identifier from internal mappings.
     */
    private void removeAliasID(AliasID id, Alias alias)
    {
        if(id.isValid())
        {
            switch(id.getType())
            {
                case ESN:
                    String esn = ((Esn) id).getEsn();

                    if(esn != null)
                    {
                        if(esn.contains(WILDCARD))
                        {
                            removeWildcard(esn, mESNWildcards);
                        }
                    }

                    mESN.remove(esn);
                    break;
                case LTR_NET_UID:
                    mUniqueID.remove(((UniqueID) id).getUid());
                    break;
                case SITE:
                    mSiteID.remove(((SiteID) id).getSite());
                    break;
                case STATUS:
                    mStatus.remove(((StatusID) id).getStatus());
                    break;
                case NON_RECORDABLE:
                case PRIORITY:
                case BROADCAST_CHANNEL:
                    //We don't maintain lookups for these items
                    break;
                default:
                    mLog.warn("Unrecognized Alias ID Type:" + id.getType().name() + " - can't REMOVE from lookup table");
                    break;
            }
        }
    }

    /**
     * Returns the first matching regex wildcard from the list of wildcards that matches the
     * identifier.
     *
     * @param id to match
     * @param wildcards to match against
     * @return matching wildcard ID or null
     */
    private String getWildcardMatch(String id, List<WildcardID> wildcards)
    {
        if(id != null)
        {
            for(WildcardID wildcard : wildcards)
            {
                if(wildcard.matches(id))
                {
                    return wildcard.value();
                }
            }
        }

        return null;
    }

    /**
     * Lookup alias by site ID
     */
    public Alias getSiteID(String siteID)
    {
        Alias alias = null;

        if(siteID != null)
        {
            alias = mSiteID.get(siteID);

            if(alias == null)
            {
                String wildcard = getWildcardMatch(siteID, mSiteWildcards);

                if(wildcard != null)
                {
                    alias = mSiteID.get(wildcard);
                }
            }
        }

        return alias;
    }

    /**
     * Lookup alias by status ID
     */
    public Alias getStatus(int status)
    {
        return mStatus.get(status);
    }

    /**
     * Lookup alias by Unique ID (UID)
     */
    public Alias getUniqueID(int uniqueID)
    {
        return mUniqueID.get(uniqueID);
    }

    /**
     * Lookup alias by ESN
     */
    public Alias getESNAlias(String esn)
    {
        Alias alias = null;

        if(esn != null)
        {
            alias = mESN.get(esn);

            if(alias == null)
            {
                String wildcard = getWildcardMatch(esn, mESNWildcards);

                if(wildcard != null)
                {
                    alias = mESN.get(wildcard);
                }
            }
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
     * @return alias or null
     */
    public Alias getAlias(Identifier identifier)
    {
//TODO: update this to List<Alias> return type so that we can return all of the aliases for a patch group
        if(identifier != null)
        {
            switch(identifier.getForm())
            {
                case TALKGROUP:
                    TalkgroupIdentifier talkgroup = (TalkgroupIdentifier)identifier;

                    TalkgroupAliasList talkgroupAliasList = mTalkgroupProtocolMap.get(identifier.getProtocol());

                    if(talkgroupAliasList != null)
                    {
                        return talkgroupAliasList.getAlias(talkgroup);
                    }
                    break;
                case PATCH_GROUP:
                    PatchGroupIdentifier patchGroupIdentifier = (PatchGroupIdentifier)identifier;
                    PatchGroup patchGroup = patchGroupIdentifier.getValue();

                    TalkgroupAliasList patchGroupAliasList = mTalkgroupProtocolMap.get(patchGroupIdentifier.getProtocol());

                    if(patchGroupAliasList != null)
                    {
//TODO: make this a list of aliases that match the patch group - right now it just returns the first-matched
                        Alias alias = patchGroupAliasList.getAlias(patchGroup.getPatchGroup());

                        if(alias != null)
                        {
                            return alias;
                        }

                        for(TalkgroupIdentifier patchedGroup: patchGroup.getPatchedGroupIdentifiers())
                        {
                            Alias patchedAlias = patchGroupAliasList.getAlias(patchedGroup);

                            if(patchedAlias != null)
                            {
                                return patchedAlias;
                            }
                        }
                    }
                    break;
                case ESN:
                    if(identifier instanceof ESNIdentifier)
                    {
                        return getESNAlias(((ESNIdentifier)identifier).getValue());
                    }
                    break;
            }
        }

        return null;
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
            Alias alias = getAlias(identifier);

            if(alias != null && alias.isStreamable())
            {
                return true;
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
            Alias alias = getAlias(identifier);

            if(alias != null && alias.isRecordable())
            {
                return true;
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
            Alias alias = getAlias(identifier);

            if(alias != null && alias.getPlaybackPriority() < priority)
            {
                priority = alias.getPlaybackPriority();
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
            Alias alias = getAlias(identifier);

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

        return channels;
    }

    /**
     * Listing of talkgroups and ranges for a specific protocol
     */
    public class TalkgroupAliasList
    {
        private Map<Integer,Alias> mTalkgroupAliasMap = new TreeMap<>();
        private Map<TalkgroupRange, Alias> mTalkgroupRangeMap = new HashMap<>();

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

            for(TalkgroupRange talkgroupRange: mTalkgroupRangeMap.keySet())
            {
                if(talkgroupRange.contains(value))
                {
                    return mTalkgroupRangeMap.get(talkgroupRange);
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
            mTalkgroupRangeMap.put(talkgroupRange, alias);
        }

        public void remove(Talkgroup talkgroup)
        {
            mTalkgroupAliasMap.remove(talkgroup.getValue());
        }

        public void remove(TalkgroupRange talkgroupRange)
        {
            mTalkgroupRangeMap.remove(talkgroupRange);
        }
    }
}
