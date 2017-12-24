/*
 * *********************************************************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 * *********************************************************************************************************************
 */

package io.github.dsheirer.record.wave;

/**
 * WAVE audio metadata tags.
 *
 * INFO tags are detailed here: https://sno.phy.queensu.ca/~phil/exiftool/TagNames/RIFF.html#Info
 */
public enum WaveMetadataType
{
    ARTIST_NAME("IART", false), //System
    ALBUM_TITLE("TALB", false), //Site
    TRACK_TITLE("INAM", false),  //Channel Name
    COMMENTS("ICMT", false),
    DATE_CREATED("ICRD", false),
    GENRE("IGNR", false),  //Site
    PRODUCT("IPRD", false),  //sdrtrunk
    SOURCE_FORM("ISRF", false), //protocol

    //Custom Tags
    ALIAS_LIST_NAME("ALLN", true),
    CHANNEL_FREQUENCY("CHFQ", true),
    CHANNEL_ID("CHID", true),
    CHANNEL_TIMESLOT("CHTS", true),
    NETWORK_ID_1("NTW1", true),
    NETWORK_ID_2("NTW2", true),
    TALKGROUP_PRIMARY_PATCHED_1("TPP1", true),
    TALKGROUP_PRIMARY_PATCHED_2("TPP2", true),
    TALKGROUP_PRIMARY_PATCHED_3("TPP3", true),
    TALKGROUP_PRIMARY_PATCHED_4("TPP4", true),
    TALKGROUP_PRIMARY_PATCHED_5("TPP5", true),
    TALKGROUP_PRIMARY_FROM("TPFM", true),
    TALKGROUP_PRIMARY_FROM_ALIAS("TPFA", true),
    TALKGROUP_PRIMARY_FROM_ICON("TPFI", true),
    TALKGROUP_PRIMARY_TO("TPTO", true),
    TALKGROUP_PRIMARY_TO_ALIAS("TPTA", true),
    TALKGROUP_PRIMARY_TO_ICON("TPTI", true),
    TALKGROUP_SECONDARY_FROM("TSFM", true),
    TALKGROUP_SECONDARY_FROM_ALIAS("TSFA", true),
    TALKGROUP_SECONDARY_FROM_ICON("TSFI", true),
    TALKGROUP_SECONDARY_TO("TSTO", true),
    TALKGROUP_SECONDARY_TO_ALIAS("TSTA", true),
    TALKGROUP_SECONDARY_TO_ICON("TSTI", true);

    private String mTag;
    private boolean mCustomType;

    WaveMetadataType(String tag, boolean customType)
    {
        mTag = tag;
        mCustomType = customType;
    }

    public String getTag()
    {
        return mTag;
    }

    public boolean isCustomType()
    {
        return mCustomType;
    }
}
