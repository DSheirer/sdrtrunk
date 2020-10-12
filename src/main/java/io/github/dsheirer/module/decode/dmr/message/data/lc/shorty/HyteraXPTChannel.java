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

package io.github.dsheirer.module.decode.dmr.message.data.lc.shorty;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;

import java.util.Collections;
import java.util.List;

/**
 * Hytera Channel Information
 */
public class HyteraXPTChannel extends ShortLCMessage
{
    private static final int[] FREE_REPEATER = new int[]{12, 13, 14, 15};
    private static final int[] PRIORITY_CALL_REPEATER = new int[]{16, 17, 18, 19};
    private static final int[] PRIORITY_CALL_HASHED_ADDRESS = new int[]{20, 21, 22, 23, 24, 25, 26, 27};

    /**
     * Constructs an instance
     *
     * @param message containing the short link control message bits
     */
    public HyteraXPTChannel(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if(!isValid())
        {
            sb.append("[CRC ERROR] ");
        }

        sb.append("SLC HYTERA XPT");
        if(isAllChannelsBusy())
        {
            sb.append(" ALL REPEATERS BUSY");
        }
        else
        {
            sb.append(" FREE REPEATER:").append(getFreeRepeater());
        }

        if(hasPriorityCall())
        {
            sb.append(" PRIORITY CALL FOR:").append(getPriorityCallHashedAddress());
            sb.append(" ON REPEATER:").append(getPriorityCallRepeater());
        }

        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Indicates if all free repreaters are busy
     */
    public boolean isAllChannelsBusy()
    {
        return getFreeRepeater() == 0;
    }

    /**
     * Next available free repeater
     */
    public int getFreeRepeater()
    {
        return getMessage().getInt(FREE_REPEATER);
    }

    /**
     * Indicates if there is a priority call on another repeater channel that users should monitor when their
     * talkgroup matches the priority call hashed address
     */
    public boolean hasPriorityCall()
    {
        return getPriorityCallRepeater() > 0;
    }

    /**
     * Priority call repeater number
     */
    public int getPriorityCallRepeater()
    {
        return getMessage().getInt(PRIORITY_CALL_REPEATER);
    }

    /**
     * Hashed address for priority call
     */
    public String getPriorityCallHashedAddress()
    {
        return String.format("%02X", getMessage().getInt(PRIORITY_CALL_HASHED_ADDRESS));
    }


    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
