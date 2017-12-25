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
 * Metadata tag details:
 * LIST: https://sno.phy.queensu.ca/~phil/exiftool/TagNames/RIFF.html#Info
 * ID3: http://id3.org/id3v2.3.0
 *
 */
public enum WaveMetadataType
{
    //Primary tags
    ARTIST_NAME("TPE1", "IART", true), //System
    ALBUM_TITLE("TALB", "IPRD", true), //Site
    TRACK_TITLE("TIT2", "INAM", true),  //Channel Name
    COMMENTS("COMM", "ICMT", true),
    DATE_CREATED("TDRC", "ICRD", true),
    GENRE("TCON", "IGNR", true),
    SOFTWARE("TSSE", "ISFT", true),  //sdrtrunk application name
    SOURCE_FORM("TMED", "ISRF", true), //protocol

    //Secondary tags
    ALIAS_LIST_NAME("TALN", "ALLN", false),
    CHANNEL_FREQUENCY("TCHF", "CHFQ", false),
    CHANNEL_ID("TCHI", "CHID", false),
    CHANNEL_TIMESLOT("TCHT", "CHTS", false),
    NETWORK_ID_1("TNT1", "NTW1", false),
    NETWORK_ID_2("TNT2", "NTW2", false),
    TALKGROUP_PRIMARY_PATCHED_1("TPP1", "TPP1", false),
    TALKGROUP_PRIMARY_PATCHED_2("TPP2", "TPP2", false),
    TALKGROUP_PRIMARY_PATCHED_3("TPP3", "TPP3", false),
    TALKGROUP_PRIMARY_PATCHED_4("TPP4", "TPP4", false),
    TALKGROUP_PRIMARY_PATCHED_5("TPP5", "TPP5", false),
    TALKGROUP_PRIMARY_FROM("TPFM", "TPFM", false),
    TALKGROUP_PRIMARY_FROM_ALIAS("TPFA", "TPFA", false),
    TALKGROUP_PRIMARY_FROM_ICON("TPFI", "TPFI", false),
    TALKGROUP_PRIMARY_TO("TPTO", "TPTO", false),
    TALKGROUP_PRIMARY_TO_ALIAS("TOTA", "TPTA", false),
    TALKGROUP_PRIMARY_TO_ICON("TPTI", "TPTI", false),
    TALKGROUP_SECONDARY_FROM("TSFM", "TSFM", false),
    TALKGROUP_SECONDARY_FROM_ALIAS("TSFA", "TSFA", false),
    TALKGROUP_SECONDARY_FROM_ICON("TSFI", "TSFI", false),
    TALKGROUP_SECONDARY_TO("TSTO", "TSTO", false),
    TALKGROUP_SECONDARY_TO_ALIAS("TSTA", "TSTA", false),
    TALKGROUP_SECONDARY_TO_ICON("TSTI", "TSTI", false);

    private String mID3Tag;
    private String mLISTTag;
    private boolean mPrimaryTag;

    /**
     * Wave file LIST and ID3 chunk metadata tag types
     * @param id3Tag used in the ID3 chunk
     * @param listTag used in the LIST chunk
     * @param PrimaryTag indicates if this is a custom LIST tag (true) or standard LIST tag (false)
     */
    WaveMetadataType(String id3Tag, String listTag, boolean PrimaryTag)
    {
        mID3Tag = id3Tag;
        mLISTTag = listTag;
        mPrimaryTag = PrimaryTag;
    }

    /**
     * Tag to use when this metadata type is included in a WAV ID3 chunk
     */
    public String getID3Tag()
    {
        return mID3Tag;
    }

    /**
     * Tag to use when this metadata type is included in a WAV INFO/LIST chunk
     */
    public String getLISTTag()
    {
        return mLISTTag;
    }

    /**
     * Indicates if this is a primary tag that should be listed first, before the secondary tags
     */
    public boolean isPrimaryTag()
    {
        return mPrimaryTag;
    }
}
