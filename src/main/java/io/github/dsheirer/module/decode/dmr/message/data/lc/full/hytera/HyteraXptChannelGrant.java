/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.FullLCMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Hytera XPT Channel Grant
 *
 * 49 68 50 100034 002AF9 1A770C
 */
public class HyteraXptChannelGrant extends FullLCMessage
{
    private static final int[] CHANNEL = new int[]{16, 17, 18, 19};
    //Unknown: 20-23
    private static final int[] TARGET = new int[]{24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] SOURCE = new int[]{48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};

    private RadioIdentifier mSourceRadio;
    private TalkgroupIdentifier mTalkgroup;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance.
     *
     * @param message for the link control payload
     */
    public HyteraXptChannelGrant(CorrectedBinaryMessage message, long timestamp, int timeslot)
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

        sb.append("FLC HYTERA XPT CHANNEL GRANT FM:");
        sb.append(getSourceRadio());
        sb.append(" TO:").append(getTalkgroup());
        sb.append(" CHANNEL:").append(getChannel());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    public int getChannel()
    {
        return getMessage().getInt(CHANNEL);
    }

    /**
     * Talkgroup address
     */
    public TalkgroupIdentifier getTalkgroup()
    {
        if(mTalkgroup == null)
        {
            mTalkgroup = DMRTalkgroup.create(getMessage().getInt(TARGET));
        }

        return mTalkgroup;
    }

    /**
     * Source radio address
     */
    public RadioIdentifier getSourceRadio()
    {
        if(mSourceRadio == null)
        {
            mSourceRadio = DMRRadio.createFrom(getMessage().getInt(SOURCE));
        }

        return mSourceRadio;
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
