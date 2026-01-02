/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
 * Services enumeration
 */
public enum Service
{
    MULTI_SITE("MULTI-SITE", 0x8000),
    MULTI_SYSTEM("MULTI-SYSTEM", 0x4000),
    LOCATION_REGISTRATION("LOCATION REG", 0x2000),
    GROUP_REGISTRATION("GROUP REG", 0x1000),
    AUTHENTICATION("AUTH", 0x0800),
    COMPOSITE_CONTROL_CHANNEL("COMPOSITE CONTROL", 0x0400),
    VOICE_CALL("VOICE", 0x0200),
    DATA_CALL("DATA", 0x0100),
    SHORT_DATA_CALL("SHORT DATA", 0x0080),
    STATUS_CALL_AND_REMOTE_CONTROL("STATUS & REMOTE CON", 0x0040),
    PSTN_NETWORK("PSTN", 0x0020),
    IP_NETWORK("IP", 0x0010);

    private final String mLabel;
    private final int mValue;

    /**
     * Constructs an instance
     * @param label pretty display
     * @param value of the flag
     */
    Service(String label, int value)
    {
        mLabel = label;
        mValue = value;
    }

    public int getValue()
    {
        return mValue;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
