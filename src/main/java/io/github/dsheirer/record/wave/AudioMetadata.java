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

package io.github.dsheirer.record.wave;

/**
 * WAVE and ID3 audio metadata tags.
 *
 * Metadata tag details:
 * LIST: https://sno.phy.queensu.ca/~phil/exiftool/TagNames/RIFF.html#Info
 * ID3: http://id3.org/id3v2.3.0
 *
 */
public enum AudioMetadata
{
    //Primary tags
    ARTIST_NAME("TPE1", "IART", true),
    ALBUM_TITLE("TALB", "IPRD", true),
    GROUPING("TIT1", "ISBJ", true),
    TRACK_TITLE("TIT2", "INAM", true),
    COMMENTS("COMM", "ICMT", true),
    DATE_CREATED("TDRC", "ICRD", true),
    GENRE("TCON", "IGNR", true),
    YEAR("TYER", "ICOP", true),
    COMPOSER("TCOM", "ISFT", true);

    private String mID3Tag;
    private String mLISTTag;
    private boolean mPrimaryTag;

    /**
     * Wave file LIST and ID3 chunk metadata tag types
     * @param id3Tag used in the ID3 chunk
     * @param listTag used in the LIST chunk
     * @param PrimaryTag indicates if this is a custom LIST tag (true) or standard LIST tag (false)
     */
    AudioMetadata(String id3Tag, String listTag, boolean PrimaryTag)
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
