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
package audio.broadcast;

public enum BroadcastServerType
{
    BROADCASTIFY("Broadcastify", "images/broadcastify.png"), //Icecast Server 2.3.2
    ICECAST_HTTP("Icecast 2.4+", null),
    ICECAST_TCP("Icecast 2.3", null),
    SHOUTCAST_V1("Shoutcast 1.x", null),
    SHOUTCAST_V2("Shoutcast 2.x", null),
    UNKNOWN("Unknown", null);

    private String mLabel;
    private String mIconName;

    BroadcastServerType(String label, String iconName)
    {
        mLabel = label;
        mIconName = iconName;
    }

    public String toString()
    {
        return mLabel;
    }

    public String getIconName()
    {
        return mIconName;
    }
}
