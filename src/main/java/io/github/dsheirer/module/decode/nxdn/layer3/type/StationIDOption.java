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
 * Option for station ID length and sequencing.
 */
public class StationIDOption
{
    private static final int START_MASK = 0x80;
    private static final int END_MASK = 0x40;
    private static final int VALUE_MASK = 0x2F;
    private final int mValue;

    /**
     * Constructs an instance
     * @param value of the field.
     */
    public StationIDOption(int value)
    {
        mValue = value;
    }

    @Override
    public String toString()
    {
        if(isStart())
        {
            return "MESSAGE #1 CHARACTER COUNT:" + getValue();
        }
        else if(isEnd())
        {
            return "(FINAL) MESSAGE #" + getValue();
        }
        else
        {
            return "MESSAGE #" + (getValue() + 1);
        }
    }

    /**
     * Indicates if this is a single fragment message.
     */
    public boolean isComplete()
    {
        return isStart() & isEnd();
    }

    /**
     * Indicates if this is the first in a sequence of messages.
     */
    public boolean isStart()
    {
        return (mValue & START_MASK) == START_MASK;
    }

    /**
     * Indicates if this is the final message of the sequence
     */
    public boolean isEnd()
    {
        return (mValue & END_MASK) == END_MASK;
    }

    /**
     * Character count (if first) or message sequence number
     * @return
     */
    public int getValue()
    {
        return mValue & VALUE_MASK;
    }
}
