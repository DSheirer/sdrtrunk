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
package io.github.dsheirer.audio.broadcast.shoutcast.v1;

import io.github.dsheirer.audio.broadcast.BroadcastFormat;

public enum ShoutcastMetadata
{
    AIM("icy-aim:"),
    AUDIO_BIT_RATE("icy-br:"),
    CHANNELS("icy-channels:"),
    CONTENT_TYPE("Content-Type:"),
    DESCRIPTION("icy-description:"),
    GENRE("icy-genre:"),
    ICE_AUDIO_INFO("ice-audio-info:"),
    ICQ("icy-icq:"),
    INTERNET_RELAY_CHAT("icy-irc:"),
    METADATA_INTERVAL("icy-metaint:"),
    METADATA_REQUEST("icy-metadata:"),
    PRE_BUFFER("icy-prebuffer:"),
    PUBLIC("icy-pub:"),
    RESET("icy-reset:"),
    SERVER("server:"),
    STREAM_NAME("icy-name:"),
    SONG_TITLE("icy-title:"),
    URL("icy-url:"),
    USER_AGENT("User-Agent:"),
    UNKNOWN("Unknown");

    private String mTag;

    private ShoutcastMetadata(String tag)
    {
        mTag = tag;
    }

    public static final String COMMAND_TERMINATOR = "\n";

    public String toString()
    {
        return mTag;
    }

    /**
     * Encodes the metadata tag and value with an end-of-line character, appropriate for sending over the network.
     */
    public String encode(String value)
    {
        if(value != null && !value.isEmpty())
        {
            StringBuilder sb = new StringBuilder();
            sb.append(mTag).append(value).append(COMMAND_TERMINATOR);
            return sb.toString();
        }

        return null;
    }

    public String encode(boolean value)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(mTag).append(value ? "1" : "0").append(COMMAND_TERMINATOR);
        return sb.toString();
    }

    public String encode(BroadcastFormat broadcastFormat)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(mTag).append(broadcastFormat.getValue()).append(COMMAND_TERMINATOR);
        return sb.toString();
    }

    public String encode(int value)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(mTag).append(String.valueOf(value)).append(COMMAND_TERMINATOR);
        return sb.toString();
    }
}
