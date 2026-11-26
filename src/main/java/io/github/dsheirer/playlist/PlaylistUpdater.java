/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.playlist;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.alias.id.legacy.fleetsync.FleetsyncID;
import io.github.dsheirer.alias.id.legacy.mdc.MDC1200ID;
import io.github.dsheirer.alias.id.legacy.mobileID.Min;
import io.github.dsheirer.alias.id.legacy.mpt1327.MPT1327ID;
import io.github.dsheirer.alias.id.legacy.talkgroup.LegacyTalkgroupID;
import io.github.dsheirer.alias.id.legacy.uniqueID.UniqueID;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.alias.id.radio.RadioRange;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.alias.id.talkgroup.TalkgroupRange;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.module.log.EventLogType;
import io.github.dsheirer.module.log.config.EventLogConfiguration;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.record.RecorderType;
import io.github.dsheirer.record.config.RecordConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Playlist updater will automatically update a playlist to the current version specified by the Playlist manager
 */
public class PlaylistUpdater
{
    private final static Logger mLog = LoggerFactory.getLogger(PlaylistUpdater.class);
    private static final String APCO25_RADIO_ID = "[A-Fa-f\\d\\*]{6}";
    private static final String APCO25_TALKGROUP = "[A-Fa-f\\d\\*]{4}";
    private static final String FLEETSYNC_TALKGROUP = "(\\d{3})-(\\d{4})";
    private static final String LTR_TALKGROUP = "([\\d\\*]{1})-([\\d\\*]{2})-([\\d\\*]{3})";
    private static final String LTR_TALKGROUP_WILDCARD = "\\*-\\*{2}-([\\d]{3})";
    private static final String MDC1200_TALKGROUP = "[A-Fa-f\\d\\*]{4}";
    private static final String MOBILE_ID_NUMBER = "[A-Fa-f\\d\\*]{6}";
    private static final String MPT1327_TALKGROUP = "(\\d{3})-(\\d{4})";
    private static final String PASSPORT_TALKGROUP = "[\\d\\*]{5}";
    private static final Pattern FLEETSYNC_PATTERN = Pattern.compile(FLEETSYNC_TALKGROUP);
    private static final Pattern LTR_PATTERN = Pattern.compile(LTR_TALKGROUP);
    private static final Pattern MPT1327_PATTERN = Pattern.compile(MPT1327_TALKGROUP);

    /**
     * Updates the playlist as necessary.
     *
     * @param playlist to update
     * @return true if the playlist was updated
     */
    public static boolean update(PlaylistV2 playlist)
    {
        boolean updated = false;

        if(playlist.getVersion() != PlaylistManager.PLAYLIST_CURRENT_VERSION)
        {
            mLog.info("Updating Playlist from version [" + playlist.getVersion() + "] to version [" +
                PlaylistManager.PLAYLIST_CURRENT_VERSION + "]");
        }

        switch(playlist.getVersion())
        {
            //Deliberate fall-through on case statements so that all updates are applied
            case 1:
            case 2:
                removeVersion1AudioRecordType(playlist);
                removeVersion1BinaryMessageLogger(playlist);
                removeVersion1NonRecordableAliasIdentifiers(playlist);
                removeVersion1SiteIdentifiers(playlist);
                updateVersion1Talkgroups(playlist);
                updated = true;
            case 3:
                convertP25TalkgroupsToRadioIdentifiers(playlist);
                updated = true;
                break;
        }

        return updated;
    }

    /**
     * Converts P25 alias talkgroup identifiers to use the new radio identifiers.  This update only impacts talkgroups
     * that have a value greater than 65,535
     * @param playlist to update.
     */
    private static void convertP25TalkgroupsToRadioIdentifiers(PlaylistV2 playlist)
    {
        int converted = 0;

        for(Alias alias: playlist.getAliases())
        {
            Iterator<AliasID> it = alias.getAliasIdentifiers().iterator();

            List<AliasID> convertedAliasIds = new ArrayList<>();

            AliasID id = null;

            while(it.hasNext())
            {
                id = it.next();

                if(id instanceof Talkgroup)
                {
                    Talkgroup talkgroup = (Talkgroup)id;

                    if(talkgroup.getProtocol() == Protocol.APCO25 && talkgroup.getValue() > 65535)
                    {
                        it.remove();

                        Radio radio = new Radio();
                        radio.setProtocol(Protocol.APCO25);
                        radio.setValue(talkgroup.getValue());
                        convertedAliasIds.add(radio);
                    }
                }
                else if(id instanceof TalkgroupRange)
                {
                    TalkgroupRange talkgroupRange = (TalkgroupRange)id;

                    if(talkgroupRange.getProtocol() == Protocol.APCO25)
                    {
                        if(talkgroupRange.getMinTalkgroup() > 65535)
                        {
                            it.remove();

                            RadioRange radioRange = new RadioRange();
                            radioRange.setProtocol(Protocol.APCO25);
                            radioRange.setMinRadio(talkgroupRange.getMinTalkgroup());
                            radioRange.setMaxRadio(talkgroupRange.getMaxTalkgroup());
                            convertedAliasIds.add(radioRange);
                        }
                        else if(talkgroupRange.getMaxTalkgroup() > 65535)
                        {
                            RadioRange radioRange = new RadioRange();
                            radioRange.setProtocol(Protocol.APCO25);
                            radioRange.setMinRadio(65536);
                            radioRange.setMaxRadio(talkgroupRange.getMaxTalkgroup());
                            convertedAliasIds.add(radioRange);

                            talkgroupRange.setMaxTalkgroup(65535);
                        }
                    }
                }
            }

            for(AliasID aliasID: convertedAliasIds)
            {
                converted++;
                alias.addAliasID(aliasID);
            }
        }

        if(converted > 0)
        {
            mLog.info("Converted [" + converted + "] P25 talkgroups and/or talkgroup ranges to radio id or radio id ranges");
        }
    }

    /**
     * Removes version 1 AUDIO record type from the record configuration
     */
    private static void removeVersion1AudioRecordType(PlaylistV2 playlist)
    {
        int removed = 0;

        for(Channel channel : playlist.getChannels())
        {
            RecordConfiguration recordConfiguration = channel.getRecordConfiguration();

            if(recordConfiguration.contains(RecorderType.AUDIO))
            {
                Iterator<RecorderType> it = recordConfiguration.getRecorders().iterator();

                while(it.hasNext())
                {
                    if(it.next() == RecorderType.AUDIO)
                    {
                        it.remove();
                        removed++;
                    }
                }
            }
        }

        if(removed > 0)
        {
            mLog.info("Removed audio-record setting from [" + removed + "] channels. Audio recording is now a setting for each individual alias");
        }
    }

    /**
     * Removes version 1 BINARY message event logger
     */
    private static void removeVersion1BinaryMessageLogger(PlaylistV2 playlist)
    {
        int removed = 0;

        for(Channel channel : playlist.getChannels())
        {
            EventLogConfiguration eventLogConfiguration = channel.getEventLogConfiguration();

            if(eventLogConfiguration.getLoggers().contains(EventLogType.BINARY_MESSAGE))
            {
                Iterator<EventLogType> it = eventLogConfiguration.getLoggers().iterator();

                while(it.hasNext())
                {
                    if(it.next() == EventLogType.BINARY_MESSAGE)
                    {
                        it.remove();
                        removed++;
                    }
                }
            }
        }

        if(removed > 0)
        {
            mLog.info("Removed binary message event logging from [" + removed +
                "] channels. Use demodulated bitstream recorder instead");
        }
    }

    /**
     * Removes non-recordable alias identifiers from all aliases.  These have been replaced with the 'Record' alias
     * identifier.
     */
    private static void removeVersion1NonRecordableAliasIdentifiers(PlaylistV2 playlist)
    {
        int removed = 0;

        for(Alias alias : playlist.getAliases())
        {
            Iterator<AliasID> it = alias.getAliasIdentifiers().iterator();

            while(it.hasNext())
            {
                if(it.next().getType() == AliasIDType.NON_RECORDABLE)
                {
                    it.remove();
                    removed++;
                }
            }
        }

        if(removed > 0)
        {
            mLog.info("Removed [" + removed + "] non-recordable alias identifiers from aliases");
        }
    }

    /**
     * Removes site identifiers from all aliases.  These are no longer supported
     */
    private static void removeVersion1SiteIdentifiers(PlaylistV2 playlist)
    {
        int removed = 0;

        for(Alias alias : playlist.getAliases())
        {
            Iterator<AliasID> it = alias.getAliasIdentifiers().iterator();

            while(it.hasNext())
            {
                if(it.next().getType() == AliasIDType.SITE)
                {
                    it.remove();
                    removed++;
                }
            }
        }

        if(removed > 0)
        {
            mLog.info("Removed [" + removed + "] site identifiers from aliases");
        }
    }

    /**
     * Converts legacy version 1 talkgroups to the new talkgroup format.  Previously, each protocol had a talkgroup
     * flavor and now there is a generic talkgroup identifier with a protocol specifier.
     *
     * @param playlist to update
     */
    private static void updateVersion1Talkgroups(PlaylistV2 playlist)
    {
        int updated = 0;
        int notUpdated = 0;

        AliasID next = null;
        List<AliasID> toAdd = new ArrayList<>();

        for(Alias alias : playlist.getAliases())
        {
            Iterator<AliasID> it = alias.getAliasIdentifiers().iterator();

            while(it.hasNext())
            {
                next = it.next();

                switch(next.getType())
                {
                    case LEGACY_TALKGROUP:
                        LegacyTalkgroupID legacyTalkgroupId = (LegacyTalkgroupID)next;

                        String talkgroup = legacyTalkgroupId.getTalkgroup();

                        if(talkgroup != null)
                        {
                            if(talkgroup.matches(APCO25_TALKGROUP) || talkgroup.matches(APCO25_RADIO_ID))
                            {
                                if(talkgroup.contains("*"))
                                {
                                    notUpdated++;
                                }
                                else
                                {
                                    try
                                    {
                                        Integer value = Integer.parseInt(talkgroup, 16);
                                        it.remove();
                                        toAdd.add(new Talkgroup(Protocol.APCO25, value));
                                        updated++;
                                    }
                                    catch(Exception e)
                                    {
                                        //Unable to parse integer value from talkgroup .. do nothing
                                        notUpdated++;
                                    }
                                }
                            }
                            else if(talkgroup.matches(LTR_TALKGROUP))
                            {
                                if(talkgroup.contains("*"))
                                {
                                    if(talkgroup.matches(LTR_TALKGROUP_WILDCARD))
                                    {
                                        try
                                        {
                                            Integer group = Integer.parseInt(talkgroup.substring(5));

                                            for(int area = 0; area <= 1; area++)
                                            {
                                                for(int channel = 1; channel <= 20; channel++)
                                                {
                                                    int value = (area << 13) + (channel << 8) + group;
                                                    toAdd.add(new Talkgroup(Protocol.LTR, value));
                                                    mLog.debug("LTR Talkgroup [" + talkgroup + "] updated to [" + value + "]");
                                                }
                                            }
                                            it.remove();
                                            updated++;
                                        }
                                        catch(Exception e)
                                        {
                                            notUpdated++;
                                        }
                                    }
                                    else
                                    {
                                        notUpdated++;
                                    }
                                }
                                else
                                {
                                    try
                                    {
                                        Matcher m = LTR_PATTERN.matcher(talkgroup);

                                        if(m.matches())
                                        {
                                            int value = Integer.valueOf(m.group(3));
                                            value += (Integer.valueOf(m.group(2)) << 8);
                                            toAdd.add(new Talkgroup(Protocol.LTR, value));
                                            it.remove();
                                            updated++;
                                        }
                                    }
                                    catch(Exception e)
                                    {
                                        notUpdated++;
                                    }
                                }
                            }
                            else if(talkgroup.matches(PASSPORT_TALKGROUP))
                            {
                                if(talkgroup.contains("*"))
                                {
                                    notUpdated++;
                                }
                                else
                                {
                                    try
                                    {
                                        Integer value = Integer.parseInt(talkgroup);
                                        it.remove();
                                        toAdd.add(new Talkgroup(Protocol.PASSPORT, value));
                                        updated++;
                                    }
                                    catch(Exception e)
                                    {
                                        //Unable to parse integer value from talkgroup .. do nothing
                                        notUpdated++;
                                    }
                                }
                            }
                        }
                        else
                        {
                            //Throw away any legacy talkgroup identifiers with a null value
                            it.remove();
                        }
                        break;
                    case FLEETSYNC:
                        String fleetsync = ((FleetsyncID)next).getIdent();

                        try
                        {
                            Matcher matcher = FLEETSYNC_PATTERN.matcher(fleetsync);

                            if(matcher.matches())
                            {
                                int value = Integer.valueOf(matcher.group(2));
                                value += (Integer.valueOf(matcher.group(1)) << 12);

                                toAdd.add(new Talkgroup(Protocol.FLEETSYNC, value));
                                it.remove();
                                updated++;
                            }
                            else
                            {
                                notUpdated++;
                            }
                        }
                        catch(Exception e)
                        {
                            notUpdated++;
                        }
                        break;
                    case MDC1200:
                        String mdc = ((MDC1200ID)next).getIdent();

                        if(mdc != null && mdc.matches(MDC1200_TALKGROUP))
                        {
                            try
                            {
                                int mdcValue = Integer.parseInt(mdc, 16);
                                toAdd.add(new Talkgroup(Protocol.MDC1200, mdcValue));
                                it.remove();
                                updated++;
                            }
                            catch(Exception e)
                            {
                                notUpdated++;
                            }
                        }
                        else
                        {
                            notUpdated++;
                        }

                        break;
                    case MPT1327:
                        String mpt = ((MPT1327ID)next).getIdent();

                        if(mpt != null && mpt.matches(MPT1327_TALKGROUP))
                        {
                            try
                            {
                                Matcher matcher = MPT1327_PATTERN.matcher(mpt);

                                if(matcher.matches())
                                {
                                    int value = Integer.valueOf(matcher.group(2));
                                    value += (Integer.valueOf(matcher.group(1)) << 13);

                                    toAdd.add(new Talkgroup(Protocol.MPT1327, value));
                                    it.remove();
                                    updated++;

                                    mLog.debug("MPT-1327 Talkgroup [" + mpt + "] updated to [" + value + "]");
                                }
                                else
                                {
                                    notUpdated++;
                                }
                            }
                            catch(Exception e)
                            {
                                notUpdated++;
                            }
                        }
                        break;
                    case MIN:
                        String min = ((Min)next).getMin();

                        if(min.matches(MOBILE_ID_NUMBER))
                        {
                            if(min.contains("*"))
                            {
                                notUpdated++;
                            }
                            else
                            {
                                try
                                {
                                    Integer value = Integer.parseInt(min, 16);
                                    it.remove();
                                    toAdd.add(new Talkgroup(Protocol.PASSPORT, value));
                                    updated++;
                                }
                                catch(Exception e)
                                {
                                    //Unable to parse integer value from talkgroup .. do nothing
                                    notUpdated++;
                                }
                            }
                        }
                        break;
                    case LTR_NET_UID:
                        int uid = ((UniqueID)next).getUid();
                        toAdd.add(new Talkgroup(Protocol.LTR_NET, uid));
                        it.remove();
                        updated++;
                        break;
                }
            }

            //Add any alias identifiers that were converted.  We have to do this here in order
            //to avoid concurrent modification errors on the alias id list.
            for(AliasID aliasID : toAdd)
            {
                alias.addAliasID(aliasID);
            }

            toAdd.clear();
        }

        if(updated > 0)
        {
            mLog.info("Updated [" + updated + "] legacy talkgroup identifiers to new talkgroup format");
        }

        if(notUpdated > 0)
        {
            mLog.info("Unable to update [" + notUpdated + "] legacy talkgroup identifiers - please edit and convert to new talkgroup format");
        }
    }
}
