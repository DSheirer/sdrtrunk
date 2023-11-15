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
package io.github.dsheirer.audio.broadcast;

public enum BroadcastServerType
{
    /**
     * Broadcastify feeds (ie streaming) service
     */
    BROADCASTIFY("Broadcastify Feed", "images/broadcastify.png"), //Icecast Server 2.3.2

    /**
     * Broadcastify calls - completed audio recording push service
     */
    BROADCASTIFY_CALL("Broadcastify Call", "images/broadcastify.png"),

    ICECAST_HTTP("Icecast 2 (v2.4+)", "images/icecast.png"),
    RDIOSCANNER_CALL("Rdio Scanner", "images/rdioscanner.png"),
    OPENMHZ("OpenMHz", "images/openmhz.png"),
    ICECAST_TCP("Icecast (v2.3)", "images/icecast.png"),
    SHOUTCAST_V1("Shoutcast v1.x", "images/shoutcast.png"),
    SHOUTCAST_V2("Shoutcast v2.x", "images/shoutcast.png"),
    UNKNOWN("Unknown", null);

    private String mLabel;
    private String mIconPath;

    BroadcastServerType(String label, String iconPath)
    {
        mLabel = label;
        mIconPath = iconPath;
    }

    public String toString()
    {
        return mLabel;
    }

    public String getIconPath()
    {
        return mIconPath;
    }
}
