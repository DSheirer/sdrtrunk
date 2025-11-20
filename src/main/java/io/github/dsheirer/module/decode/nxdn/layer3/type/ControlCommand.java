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
 * Commands used with remote control.
 */
public enum ControlCommand
{
    STUN("STUN"),
    REVIVAL("REVIVAL"),
    KILL("KILL"),
    REMOTE_MONITOR("REMOTE MONITOR"),
    RESERVED("RESERVED");

    private final String mLabel;

    /**
     * Constructs an instance
     * @param label to display
     */
    ControlCommand(String label)
    {
        mLabel = label;
    }

    /**
     * Display value
     */
    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Look up an entry from a transmitted value.
     * @param value transmitted
     * @return matching entry or RESERVED
     */
    public static ControlCommand fromValue(int value)
    {
        return switch(value)
        {
            case 0 -> STUN;
            case 1 -> REVIVAL;
            case 2 -> KILL;
            case 4 -> REMOTE_MONITOR;
            default -> RESERVED;
        };
    }
}
