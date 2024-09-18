/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
 * ****************************************************************************
 */
package io.github.dsheirer.alias;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.alias.id.dcs.Dcs;
import io.github.dsheirer.alias.id.esn.Esn;
import io.github.dsheirer.alias.id.priority.Priority;
import io.github.dsheirer.alias.id.radio.P25FullyQualifiedRadio;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.alias.id.radio.RadioRange;
import io.github.dsheirer.alias.id.status.UnitStatusID;
import io.github.dsheirer.alias.id.status.UserStatusID;
import io.github.dsheirer.alias.id.talkgroup.P25FullyQualifiedTalkgroup;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.alias.id.talkgroup.TalkgroupRange;
import io.github.dsheirer.alias.id.tone.TonesID;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.dcs.DCSIdentifier;
import io.github.dsheirer.identifier.esn.ESNIdentifier;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.radio.FullyQualifiedRadioIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.status.UnitStatusIdentifier;
import io.github.dsheirer.identifier.status.UserStatusIdentifier;
import io.github.dsheirer.identifier.talkgroup.FullyQualifiedTalkgroupIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.identifier.tone.ToneIdentifier;
import io.github.dsheirer.identifier.tone.ToneSequence;
import io.github.dsheirer.module.decode.dcs.DCSCode;
import io.github.dsheirer.protocol.Protocol;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * List of aliases that share the same alias list name and provides convenient methods for looking up alias
 * objects that match an identifier.
 */
public class AliasList
{
    private final static Logger mLog = LoggerFactory.getLogger(AliasList.class);
    private Map<Protocol,TalkgroupAliasList> mTalkgroupProtocolMap = new EnumMap<>(Protocol.class);
    private Map<Protocol,RadioAliasList> mRadioProtocolMap = new EnumMap<>(Protocol.class);
    private Map<DCSCode,Alias> mDCSCodeAliasMap = new EnumMap<>(DCSCode.class);
    private Map<String,Alias> mESNMap = new HashMap<>();
    private Map<Integer,Alias> mUnitStatusMap = new HashMap<>();
    private Map<Integer,Alias> mUserStatusMap = new HashMap<>();
    private Map<ToneSequence,Alias> mToneSequenceMap = new HashMap<>();
    private boolean mHasAliasActions = false;
    private String mName;
    private ObservableList<Alias> mAliases = FXCollections.observableArrayList(Alias.extractor());

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
     * Observable list of aliases contained in this alias list
     */
    public ObservableList<Alias> aliases()
    {
        return mAliases;
    }

    /**
     * Adds the alias to this list
     */
    public void addAlias(Alias alias)
    {
        if(alias != null)
        {
            alias.getAliasIdentifiers().stream().forEach(aliasID -> addAliasID(aliasID, alias));
        }

        if(alias.hasActions())
        {
            mHasAliasActions = true;
        }

        if(!mAliases.contains(alias))
        {
            mAliases.add(alias);
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
                    case P25_FULLY_QUALIFIED_RADIO_ID:
                        P25FullyQualifiedRadio qualifiedRadio = (P25FullyQualifiedRadio) id;

                        RadioAliasList p25RadioAliasList = mRadioProtocolMap.get(qualifiedRadio.getProtocol());

                        if(p25RadioAliasList == null)
                        {
                            p25RadioAliasList = new RadioAliasList();
                            mRadioProtocolMap.put(qualifiedRadio.getProtocol(), p25RadioAliasList);
                        }

                        p25RadioAliasList.add(qualifiedRadio, alias);
                        break;
                    case P25_FULLY_QUALIFIED_TALKGROUP:
                        P25FullyQualifiedTalkgroup qualifiedTalkgroup = (P25FullyQualifiedTalkgroup) id;

                        TalkgroupAliasList p25TalkgroupAliasList = mTalkgroupProtocolMap.get(qualifiedTalkgroup.getProtocol());

                        if(p25TalkgroupAliasList == null)
                        {
                            p25TalkgroupAliasList = new TalkgroupAliasList();
                            mTalkgroupProtocolMap.put(qualifiedTalkgroup.getProtocol(), p25TalkgroupAliasList);
                        }

                        p25TalkgroupAliasList.add(qualifiedTalkgroup, alias);
                        break;
                    case RADIO_ID:
                        Radio radio = (Radio)id;

                        RadioAliasList radioAliasList = mRadioProtocolMap.get(radio.getProtocol());

                        if(radioAliasList == null)
                        {
                            radioAliasList = new RadioAliasList();
                            mRadioProtocolMap.put(radio.getProtocol(), radioAliasList);
                        }

                        radioAliasList.add(radio, alias);
                        break;
                    case RADIO_ID_RANGE:
                        RadioRange radioRange = (RadioRange)id;

                        RadioAliasList radioRangeAliasList = mRadioProtocolMap.get(radioRange.getProtocol());

                        if(radioRangeAliasList == null)
                        {
                            radioRangeAliasList = new RadioAliasList();
                            mRadioProtocolMap.put(radioRange.getProtocol(), radioRangeAliasList);
                        }

                        radioRangeAliasList.add(radioRange, alias);
                        break;
                    case DCS:
                        if(id instanceof Dcs dcs)
                        {
                            mDCSCodeAliasMap.put(dcs.getDCSCode(), alias);
                        }
                        break;
                    case ESN:
                        String esn = ((Esn)id).getEsn();

                        if(esn != null && !esn.isEmpty())
                        {
                            mESNMap.put(esn.toLowerCase(), alias);
                        }
                        break;
                    case STATUS:
                        int userStatus = ((UserStatusID)id).getStatus();

                        if(mUserStatusMap.containsKey(userStatus) && !mUserStatusMap.get(userStatus).equals(alias))
                        {
                            id.setOverlap(true);

                            Alias existing = mUserStatusMap.get(userStatus);

                            for(AliasID aliasID: existing.getAliasIdentifiers())
                            {
                                if(aliasID instanceof UserStatusID && ((UserStatusID)aliasID).getStatus() == userStatus)
                                {
                                    aliasID.setOverlap(true);
                                }
                            }
                        }
                        mUserStatusMap.put(userStatus, alias);
                        break;
                    case UNIT_STATUS:
                        int unitStatus = ((UnitStatusID)id).getStatus();

                        if(mUnitStatusMap.containsKey(unitStatus) && !mUnitStatusMap.get(unitStatus).equals(alias))
                        {
                            id.setOverlap(true);

                            Alias existing = mUnitStatusMap.get(unitStatus);

                            for(AliasID aliasID: existing.getAliasIdentifiers())
                            {
                                if(aliasID instanceof UnitStatusID && ((UnitStatusID)aliasID).getStatus() == unitStatus)
                                {
                                    aliasID.setOverlap(true);
                                }
                            }
                        }
                        mUnitStatusMap.put(unitStatus, alias);
                        break;
                    case TONES:
                        ToneSequence toneSequence = ((TonesID)id).getToneSequence();

                        if(toneSequence != null)
                        {
                            if(mToneSequenceMap.containsKey(toneSequence) && !mToneSequenceMap.get(toneSequence).equals(alias))
                            {
                                id.setOverlap(true);

                                Alias existing = mToneSequenceMap.get(toneSequence);

                                for(AliasID aliasID: existing.getAliasIdentifiers())
                                {
                                    if(aliasID instanceof TonesID && aliasID.equals(id))
                                    {
                                        aliasID.setOverlap(true);
                                    }
                                }
                            }
                            else
                            {
                                mToneSequenceMap.put(toneSequence, alias);
                            }
                        }
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
        //Note: because the alias' identifiers could have changed from when we initially added the alias, we have to
        //inspect every collection and map to remove the alias completely.
        mAliases.remove(alias);

        mTalkgroupProtocolMap.values().stream().forEach(talkgroupAliasList -> talkgroupAliasList.remove(alias));
        mRadioProtocolMap.values().stream().forEach(radioAliasList -> radioAliasList.remove(alias));

        Collection<Alias> collection = Collections.singleton(alias);
        mESNMap.values().removeAll(collection);
        mUnitStatusMap.values().removeAll(collection);
        mUserStatusMap.values().removeAll(collection);
        mToneSequenceMap.values().removeAll(collection);

        validate();
    }

    /**
     * Identifies all aliases with an alias identifier that has the overlap flag set, resets the flag, and then readds
     * each alias back to this alias list so that overlap can be detected again.
     */
    public void validate()
    {
        Set<Alias> overlapAliases = new HashSet<>();

        List<Alias> aliases = new ArrayList<>(mAliases);

        for(Alias alias: aliases)
        {
            for(AliasID aliasID: alias.getAliasIdentifiers())
            {
                if(aliasID.overlapProperty().get())
                {
                    aliasID.setOverlap(false);
                    overlapAliases.add(alias);
                }
            }
        }

        overlapAliases.stream().forEach(alias -> addAlias(alias));
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
     * Updates the alias by removing it from this list and then adding it back to this list when the list name matches.
     */
    public void updateAlias(Alias alias)
    {
        removeAlias(alias);

        if(hasName() && getName().equals(alias.getAliasListName()))
        {
            addAlias(alias);
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
                        return toList(talkgroupAliasList.getAlias(talkgroup));
                    }
                    break;
                case PATCH_GROUP:
                    List<Alias> aliases = new ArrayList<>();

                    PatchGroupIdentifier patchGroupIdentifier = (PatchGroupIdentifier)identifier;
                    PatchGroup patchGroup = patchGroupIdentifier.getValue();

                    TalkgroupAliasList patchGroupAliasList = mTalkgroupProtocolMap.get(patchGroupIdentifier.getProtocol());

                    if(patchGroupAliasList != null)
                    {
                        Alias alias = patchGroupAliasList.getAlias(patchGroup.getPatchGroup());

                        if(alias != null)
                        {
                            aliases.add(alias);
                        }

                        for(TalkgroupIdentifier patchedTalkgroup: patchGroup.getPatchedTalkgroupIdentifiers())
                        {
                            Alias patchedTalkgroupAlias = patchGroupAliasList.getAlias(patchedTalkgroup);

                            if(patchedTalkgroupAlias != null && !aliases.contains(patchedTalkgroupAlias))
                            {
                                aliases.add(patchedTalkgroupAlias);
                            }
                        }
                    }

                    if(patchGroup.hasPatchedRadios())
                    {
                        RadioAliasList radioAliasList = mRadioProtocolMap.get(patchGroupIdentifier.getProtocol());

                        if(radioAliasList != null)
                        {
                            for(RadioIdentifier patchedRadio: patchGroup.getPatchedRadioIdentifiers())
                            {
                                Alias patchedRadioAlias = radioAliasList.getAlias(patchedRadio);

                                if(patchedRadioAlias != null && !aliases.contains(patchedRadioAlias))
                                {
                                    aliases.add(patchedRadioAlias);
                                }
                            }
                        }
                    }

                    return aliases;
                case RADIO:
                    RadioIdentifier radio = (RadioIdentifier)identifier;

                    RadioAliasList radioAliasList = mRadioProtocolMap.get(identifier.getProtocol());

                    if(radioAliasList != null)
                    {
                        return toList(radioAliasList.getAlias(radio));
                    }
                    break;
                case ESN:
                    if(identifier instanceof ESNIdentifier)
                    {
                        return toList(getESNAlias(((ESNIdentifier)identifier).getValue()));
                    }
                    break;
                case UNIT_STATUS:
                    if(identifier instanceof UnitStatusIdentifier)
                    {
                        int status = ((UnitStatusIdentifier)identifier).getValue();
                        return toList(mUserStatusMap.get(status));
                    }
                    break;
                case USER_STATUS:
                    if(identifier instanceof UserStatusIdentifier)
                    {
                        int status = ((UserStatusIdentifier)identifier).getValue();
                        return toList(mUserStatusMap.get(status));
                    }
                    break;
                case TONE:
                    if(identifier instanceof ToneIdentifier toneIdentifier)
                    {
                        ToneSequence toneSequence = toneIdentifier.getValue();

                        if(toneSequence != null && toneSequence.hasTones())
                        {
                            for(Map.Entry<ToneSequence,Alias> entry: mToneSequenceMap.entrySet())
                            {
                                if(entry.getKey().isContainedIn(toneSequence))
                                {
                                    return toList(entry.getValue());
                                }
                            }
                        }
                    }
                    else if(identifier instanceof DCSIdentifier dcsIdentifier)
                    {
                        DCSCode dcsCode = dcsIdentifier.getValue();

                        if(dcsCode != null)
                        {
                            return toList(mDCSCodeAliasMap.get(dcsCode));
                        }
                    }
                    break;
            }
        }

        return Collections.emptyList();
    }

    private static List<Alias> toList(Alias alias)
    {
        if(alias != null)
        {
            return Collections.singletonList(alias);
        }

        return Collections.emptyList();
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
     * Listing of talkgroups and ranges for a specific protocol
     */
    public class TalkgroupAliasList
    {
        private Map<String,Alias> mFullyQualifiedTalkgroupAliasMap = new HashMap<>();
        private Map<Integer,Alias> mTalkgroupAliasMap = new TreeMap<>();
        private Map<TalkgroupRange, Alias> mTalkgroupRangeAliasMap = new HashMap<>();

        public TalkgroupAliasList()
        {
        }

        public Alias getAlias(TalkgroupIdentifier identifier)
        {
            //Attempt to do a fully qualified identifier match only
            if(identifier instanceof FullyQualifiedTalkgroupIdentifier fqti)
            {
                return mFullyQualifiedTalkgroupAliasMap.get(fqti.getFullyQualifiedTalkgroupAddress());
            }

            //Attempt to match the talkgroup value
            int value = identifier.getValue();

            Alias mapValue = mTalkgroupAliasMap.get(value);
            if (mapValue != null)
            {
                return mapValue;
            }

            //Alternatively, match the talkgroup to any talkgroup ranges
            for(Map.Entry<TalkgroupRange, Alias> entry : mTalkgroupRangeAliasMap.entrySet())
            {
                if(entry.getKey().contains(value))
                {
                    return entry.getValue();
                }
            }

            return null;
        }

        public void add(Talkgroup talkgroup, Alias alias)
        {
            if(talkgroup instanceof P25FullyQualifiedTalkgroup fqt)
            {
                //Detect collisions
                if(mFullyQualifiedTalkgroupAliasMap.containsKey(fqt.getHashKey()))
                {
                    Alias existing = mFullyQualifiedTalkgroupAliasMap.get(fqt.getHashKey());

                    if(!existing.equals(alias))
                    {
                        fqt.setOverlap(true);

                        for(AliasID aliasID: existing.getAliasIdentifiers())
                        {
                            if(aliasID instanceof P25FullyQualifiedTalkgroup existingFqt &&
                                    existingFqt.getHashKey().contentEquals(fqt.getHashKey()))
                            {
                                aliasID.setOverlap(true);
                            }
                        }
                    }
                }
                else
                {
                    mFullyQualifiedTalkgroupAliasMap.put(fqt.getHashKey(), alias);
                }
            }
            else
            {
                //Detect talkgroup collisions and set overlap flag for both
                if(mTalkgroupAliasMap.containsKey(talkgroup.getValue()))
                {
                    Alias existing = mTalkgroupAliasMap.get(talkgroup.getValue());

                    if(!existing.equals(alias))
                    {
                        talkgroup.setOverlap(true);

                        for(AliasID aliasID: existing.getAliasIdentifiers())
                        {
                            if(aliasID instanceof Talkgroup && ((Talkgroup)aliasID).getValue() == talkgroup.getValue())
                            {
                                aliasID.setOverlap(true);
                            }
                        }
                    }
                }

                mTalkgroupAliasMap.put(talkgroup.getValue(), alias);
            }
        }

        public void add(TalkgroupRange talkgroupRange, Alias alias)
        {
            //Log warning if the new talkgroup range overlaps with any existing ranges
            for(Map.Entry<TalkgroupRange,Alias> entry: mTalkgroupRangeAliasMap.entrySet())
            {
                if(talkgroupRange.overlaps(entry.getKey()) && !entry.getValue().equals(alias))
                {
                    talkgroupRange.setOverlap(true);
                    entry.getKey().setOverlap(true);
                }
            }

            mTalkgroupRangeAliasMap.put(talkgroupRange, alias);
        }

        /**
         * Removes the alias from both the talkgroup and the talkgroup range maps.
         */
        public void remove(Alias alias)
        {
            mTalkgroupAliasMap.values().removeAll(Collections.singleton(alias));
            mTalkgroupRangeAliasMap.values().removeAll(Collections.singleton(alias));
        }
    }

    /**
     * Listing of radio IDs and ranges for a specific protocol
     */
    public class RadioAliasList
    {
        private Map<String,Alias> mFullyQualifiedRadioAliasMap = new HashMap<>();
        private Map<Integer,Alias> mRadioAliasMap = new TreeMap<>();
        private Map<RadioRange, Alias> mRadioRangeAliasMap = new HashMap<>();

        public RadioAliasList()
        {
        }

        public Alias getAlias(RadioIdentifier identifier)
        {
            //Match fully qualified identifier only.
            if(identifier instanceof FullyQualifiedRadioIdentifier fqri)
            {
                return mFullyQualifiedRadioAliasMap.get(fqri.getFullyQualifiedRadioAddress());
            }

            //Attempt to match against the radio identifier
            int value = identifier.getValue();

            Alias mapValue = mRadioAliasMap.get(value);
            if(mapValue != null)
            {
                return mapValue;
            }

            //Alternatively, attempt to match the radio address against any radio ranges.
            for(Map.Entry<RadioRange, Alias> entry : mRadioRangeAliasMap.entrySet())
            {
                if(entry.getKey().contains(value))
                {
                    return entry.getValue();
                }
            }

            return null;
        }

        public void add(Radio radio, Alias alias)
        {
            if(radio instanceof P25FullyQualifiedRadio fqr)
            {
                //Detect collisions
                if(mFullyQualifiedRadioAliasMap.containsKey(fqr.getHashKey()))
                {
                    Alias existing = mFullyQualifiedRadioAliasMap.get(fqr.getHashKey());

                    if(!existing.equals(alias))
                    {
                        fqr.setOverlap(true);

                        for(AliasID aliasID: existing.getAliasIdentifiers())
                        {
                            if(aliasID instanceof P25FullyQualifiedRadio existingFqr &&
                                    existingFqr.getHashKey().contentEquals(fqr.getHashKey()))
                            {
                                aliasID.setOverlap(true);
                            }
                        }
                    }
                }
                else
                {
                    mFullyQualifiedRadioAliasMap.put(fqr.getHashKey(), alias);
                }
            }
            else
            {
                //Detect collisions
                if(mRadioAliasMap.containsKey(radio.getValue()))
                {
                    Alias existing = mRadioAliasMap.get(radio.getValue());

                    if(!existing.equals(alias))
                    {
                        radio.setOverlap(true);

                        for(AliasID aliasID: existing.getAliasIdentifiers())
                        {
                            if(aliasID instanceof Radio existingRadio && (existingRadio.getValue() == radio.getValue()))
                            {
                                aliasID.setOverlap(true);
                            }
                        }
                    }
                }

                mRadioAliasMap.put(radio.getValue(), alias);
            }
        }

        public void add(RadioRange radioRange, Alias alias)
        {
            //Log warning if the new range overlaps with any existing ranges
            for(Map.Entry<RadioRange,Alias> entry: mRadioRangeAliasMap.entrySet())
            {
                if(radioRange.overlaps(entry.getKey()) && !entry.getValue().equals(alias))
                {
                    radioRange.setOverlap(true);
                    entry.getKey().setOverlap(true);
                }
            }

            mRadioRangeAliasMap.put(radioRange, alias);
        }

        /**
         * Removes the alias from both the radio and the radio range maps.
         */
        public void remove(Alias alias)
        {
            mRadioAliasMap.values().removeAll(Collections.singleton(alias));
            mRadioRangeAliasMap.values().removeAll(Collections.singleton(alias));
        }
    }
}
