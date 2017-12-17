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
package io.github.dsheirer.audio.broadcast.shoutcast.v2.ultravox;

import java.io.UnsupportedEncodingException;

public enum UltravoxMetadata
{
    ALBUM_TITLE("TALB"),
    ARTIST("TPE1"),
    AUDIO_SOURCE_WEBPAGE("WOAS"),
    BROADCAST_CLIENT_APPLICATION("TENC"),
    COMMENT("COMM"),
    CUSTOM_TEXT_FIELD("TXXX"),
    ENCODE_SETTINGS("TSSE"),
    GENERAL_BINARY_OBJECT("GEOB"),
    GENRE("TCON"),
    OWNER("TOWN"),
    RADIO_STATION("TRSN"),
    RADIO_STATION_OWNER("TRSO"),
    RADIO_STATION_WEBPAGE("WORS"),
    RECORDING_TIME("TDRC"),
    SOURCE_MEDIA_TYPE("TMED"),
    TITLE_1("TIT1"),
    TITLE_2("TIT2"),
    TITLE_3("TIT3"),
    URL("URL"),
    UNKNOWN("UNKNOWN");

    private String mXMLTag;

    private UltravoxMetadata(String xmlTag)
    {
        mXMLTag = xmlTag;
    }

    public String getXMLTag()
    {
        return mXMLTag;
    }

    /**
     * Encodes the value as an XML tag stream
     * @param value to enclose in XML tags
     * @return xml tag wrapped value
     */
    public String asXML(String value) throws UnsupportedEncodingException
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(getXMLTag()).append(">");
        sb.append(new String(value.getBytes(), "UTF-8"));
        sb.append("</").append(getXMLTag()).append(">");

        return sb.toString();
    }
}
