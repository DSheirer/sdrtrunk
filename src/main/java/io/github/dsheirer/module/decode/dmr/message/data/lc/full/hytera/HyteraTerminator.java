/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.data.lc.full.hytera;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import java.util.ArrayList;
import java.util.List;

/**
 * Hytera Terminator
 */
public class HyteraTerminator extends HyteraFullLC
{
    private TalkgroupIdentifier mTalkgroup;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance.
     *
     * @param message for the link control payload
     */
    public HyteraTerminator(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("[CRC-ERROR] ");
        }

        sb.append("FLC HYTERA TERMINATOR USER FM:");
        sb.append(getSourceRadio());
        sb.append(" TO:").append(getTalkgroup());
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
        sb.append(" ").append(getServiceOptions());
        return sb.toString();
    }

    /**
     * Talkgroup address
     */
    public TalkgroupIdentifier getTalkgroup()
    {
        if(mTalkgroup == null)
        {
            mTalkgroup = DMRTalkgroup.create(getMessage().getInt(TARGET_ADDRESS));
        }

        return mTalkgroup;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTalkgroup());
            mIdentifiers.add(getSourceRadio());
        }

        return mIdentifiers;
    }
}
