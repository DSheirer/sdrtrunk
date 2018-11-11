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

package io.github.dsheirer.playlist;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.fleetsync.FleetsyncID;
import io.github.dsheirer.alias.id.mdc.MDC1200ID;
import io.github.dsheirer.alias.id.mpt1327.MPT1327ID;
import io.github.dsheirer.alias.id.talkgroup.LegacyTalkgroupID;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.protocol.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaylistUpdater
{
    private final static Logger mLog = LoggerFactory.getLogger(PlaylistUpdater.class);
    private static final String APCO25_RADIO_ID = "[A-Fa-f\\d\\*]{6}";
    private static final String APCO25_TALKGROUP = "[A-Fa-f\\d\\*]{4}";
    private static final String FLEETSYNC_TALKGROUP = "(\\d{3})-(\\d{4})";
    private static final String LTR_TALKGROUP = "([\\d\\*]{1})-([\\d\\*]{2})-([\\d\\*]{3})";
    private static final String LTR_TALKGROUP_WILDCARD = "\\*-\\*{2}-([\\d]{3})";
    private static final String MDC1200_TALKGROUP = "[A-Fa-f\\d\\*]{4}";
    private static final String MPT1327_TALKGROUP = "(\\d{3})-(\\d{4})";
    private static final String MPT1327_TALKGROUP_PREFIX_WILDCARD = "(\\d{3})-\\*{4}";
    private static final String PASSPORT_TALKGROUP = "[\\d\\*]{5}";

    private static final Pattern FLEETSYNC_PATTERN = Pattern.compile(FLEETSYNC_TALKGROUP);
    private static final Pattern LTR_PATTERN = Pattern.compile(LTR_TALKGROUP);
    private static final Pattern MPT1327_PATTERN = Pattern.compile(MPT1327_TALKGROUP);
    private static final Pattern MPT1327_PREFIX_WILDCARD_PATTERN = Pattern.compile(MPT1327_TALKGROUP_PREFIX_WILDCARD);

    /**
     * Updates the playlist as necessary.
     *
     * @param playlist to update
     * @return true if the playlist was updated
     */
    public static boolean update(PlaylistV2 playlist)
    {
        boolean updated = false;

        updated |= updateLegacyTalkgroups(playlist);

        return updated;
    }

    private static boolean updateLegacyTalkgroups(PlaylistV2 playlist)
    {
        int updated = 0;
        int notUpdated = 0;

        AliasID next = null;
        List<AliasID> toAdd = new ArrayList<>();

        for(Alias alias : playlist.getAliases())
        {
            Iterator<AliasID> it = alias.getId().iterator();

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
                                            value += (Integer.valueOf(m.group(1)) << 13);

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
//                        else if(mpt != null && mpt.matches(MPT1327_TALKGROUP_PREFIX_WILDCARD))
//                        {
//                            try
//                            {
//                                Matcher matcher = MPT1327_PREFIX_WILDCARD_PATTERN.matcher(mpt);
//
//                                if(matcher.matches())
//                                {
//                                    int value = (Integer.valueOf(matcher.group(1)) << 13);
//
//                                    //TODO: use a talkgroup range here
//
//                                    toAdd.add(new Talkgroup(Protocol.MPT1327, value));
//                                    it.remove();
//                                    updated++;
//
//                                    mLog.debug("MPT-1327 Talkgroup [" + mpt + "] updated to [" + value + "]");
//                                }
//                                else
//                                {
//                                    notUpdated++;
//                                }
//                            }
//                            catch(Exception e)
//                            {
//                                notUpdated++;
//                            }
//                        }

                        break;
                }
            }

            //Add any alias identifiers that were converted.  We have to do this here in order
            //to avoid concurrent modification errors on the alias id list.
            for(AliasID aliasID: toAdd)
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

        return updated > 0;
    }
}
