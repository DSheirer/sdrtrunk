/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3.type;

/**
 * Control channel notification enumeration
 */
public enum ChannelNotification
{
    CURRENT,
    NEW,
    ADD,
    DELETE;


    /**
     * Utility method to lookup the channel notification type from the transmitted value.
     * @param value from the message
     * @return matching entry or UNKNOWN
     */
    public static ChannelNotification fromValue(int value)
    {
        return switch(value)
        {
            case 0x1 -> DELETE;
            case 0x2 -> ADD;
            case 0x4 -> NEW;
            default -> CURRENT;
        };
    }
}
