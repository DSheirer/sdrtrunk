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

package io.github.dsheirer.module.decode.p25.phase2.message.isch;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ISCHSequence;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.LCHType;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.SuperframeSequence;
import io.github.dsheirer.module.decode.p25.phase2.message.P25P2Message;
import java.util.Collections;
import java.util.List;

/**
 * Informational Inter-Slot Signaling Channel (I-ISCH)
 */
public class IISCH extends P25P2Message
{
    private static final IntField LCH_TYPE = IntField.range(0, 1);
    private static final IntField CHANNEL_NUMBER = IntField.range(2, 3);
    private static final IntField ISCH_SEQUENCE = IntField.range(4, 5);
    private static final int LCH_FLAG = 6;
    private static final IntField SUPERFRAME_SEQUENCE = IntField.range(7, 8);

    /**
     * Constructs the message
     *
     * @param message
     * @param offset
     * @param timeslot
     * @param timestamp of the final bit of the message
     */
    public IISCH(CorrectedBinaryMessage message, int offset, int timeslot, long timestamp)
    {
        super(message, offset, timeslot, timestamp);

        if(message.getCorrectedBitCount() >= 8)
        {
            setValid(false);
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("TS").append(getTimeslot());
        sb.append(" I-ISCH");
        if(!isValid())
        {
            sb.append(" [CRC-ERROR]");
        }
        sb.append(" ").append(getLCHType()).append(" TIMESLOT");
        sb.append(" ").append(getLCHFlagMeaning());
        sb.append(" ").append(getSuperframeSequence());
        sb.append(" ").append(getIschSequence());
//        sb.append(" MSG:").append(getMessage().toHexString());

        return sb.toString();
    }

    /**
     *
     * @return
     */
    public String getLCHFlagMeaning()
    {
        return getLCHType().getLCHFlagLabel(getMessage().get(LCH_FLAG));
    }

    /**
     * Indicates how the timeslot is configured (VCH, DATA, or LCCH).
     */
    public LCHType getLCHType()
    {
        return LCHType.fromValue(getInt(LCH_TYPE));
    }

    /**
     * Timeslot for this ISCH
     *
     * @return timeslot 1 or 2
     */
    public int getTimeslot()
    {
        return getInt(CHANNEL_NUMBER) + 1;
    }

    /**
     * Indicates this ISCH sequence's location within a super-frame
     *
     * @return location 1, 2, or 3(final)
     */
    public ISCHSequence getIschSequence()
    {
        return ISCHSequence.fromValue(getInt(ISCH_SEQUENCE));
    }

    /**
     * Superframe sequence/location within an ultraframe
     *
     * @return location, 1-4
     */
    public SuperframeSequence getSuperframeSequence()
    {
        return SuperframeSequence.fromValue(getInt(SUPERFRAME_SEQUENCE));
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
