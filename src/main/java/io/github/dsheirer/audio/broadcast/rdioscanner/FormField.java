/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.audio.broadcast.rdioscanner;

/**
 * HTTP headers used for posting to Rdio Scanner API
 */
public enum FormField
{
    AUDIO("audio"),
    AUDIO_NAME("audioName"),
    AUDIO_TYPE("audioType"),
    DATE_TIME("dateTime"),
    FREQUENCIES("frequencies"),
    FREQUENCY("frequency"),
    KEY("key"),
    PATCHES("patches"),
    SOURCE("source"),
    SOURCES("sources"),
    SYSTEM("system"),
    SYSTEM_LABEL("systemLabel"),
    TALKER_ALIAS("talkerAlias"),
    TALKGROUP_ID("talkgroup"),
    TALKGROUP_GROUP("talkgroupGroup"),
    TALKGROUP_LABEL("talkgroupLabel"),
    TALKGROUP_TAG("talkgroupTag"),
    TEST("test");
    
    private String mHeader;

    FormField(String header)
    {
        mHeader = header;
    }

    public String getHeader()
    {
        return mHeader;
    }
}
