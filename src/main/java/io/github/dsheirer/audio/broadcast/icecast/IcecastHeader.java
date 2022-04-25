/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.audio.broadcast.icecast;

public enum IcecastHeader
{
    ACCEPT("Accept"),
    AUDIO_INFO("ice-audio-info"),
    AUTHORIZATION("Authorization"),
    BITRATE("ice-bitrate"),
    CONTENT_TYPE("Content-Type"),
    DESCRIPTION("ice-description"),
    EXPECT("Expect"),
    GENRE("ice-genre"),
    HOST("Host"),
    NAME("ice-name"),
    PUBLIC("ice-public"),
    URL("ice-url"),
    USER_AGENT("User-Agent"),
    METAINT("icy-metaint"),

    UNKNOWN("unknown");

    private String mValue;

    private IcecastHeader(String value)
    {
        mValue = value;
    }

    public String getValue()
    {
        return mValue;
    }
}
