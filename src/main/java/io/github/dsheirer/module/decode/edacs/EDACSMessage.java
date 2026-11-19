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

package io.github.dsheirer.module.decode.edacs;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.protocol.Protocol;
import java.util.Collections;
import java.util.List;

/**
 * One EDACS Extended Addressing outbound control channel frame - two 28-bit messages, each after the
 * BCH check.  The second message is absent (-1) when it failed the check.
 *
 * Field layout follows DSD-FME's edacs-fme.c (GPL) for Extended Addressing and was checked on MBTA (2026-11-09):
 * <pre>
 *   message 1, bits 27-23  MT1 (bits 22-19 MT2 when MT1 = 0x1F)
 *   MT1 = 0x03 / 0x06      digital (ProVoice) / analog group voice call, assignment or update:
 *                            message 1: LCN bits 21-17, update flag bit 16, group bits 15-0
 *                            message 2: transmission trunking bit 21, emergency bit 20, source (LID) bits 19-0
 * </pre>
 * LCN numbering matches the Radio Reference channel list.  LCNs above 25 are status values (busy, queued, denied).
 */
public class EDACSMessage implements IMessage
{
    public static final int MT1_DIGITAL_GROUP_VOICE = 0x03;
    public static final int MT1_ANALOG_GROUP_VOICE = 0x06;
    public static final int MAX_LCN = 25;

    private final int mMessage1;
    private final int mMessage2;
    private final long mTimestamp;
    private final boolean mCorrected;

    /**
     * @param message1 28 data bits
     * @param message2 28 data bits, or -1 when the second message failed the BCH check
     * @param timestamp of the frame
     * @param corrected true when the three copies disagreed and the majority vote was needed
     */
    public EDACSMessage(int message1, int message2, long timestamp, boolean corrected)
    {
        mMessage1 = message1 & 0xFFFFFFF;
        mMessage2 = message2 < 0 ? -1 : message2 & 0xFFFFFFF;
        mTimestamp = timestamp;
        mCorrected = corrected;
    }

    public int getMessage1()
    {
        return mMessage1;
    }

    public int getMessage2()
    {
        return mMessage2;
    }

    public boolean hasMessage2()
    {
        return mMessage2 >= 0;
    }

    @Override
    public long getTimestamp()
    {
        return mTimestamp;
    }

    public boolean isCorrected()
    {
        return mCorrected;
    }

    public int getMT1()
    {
        return (mMessage1 >> 23) & 0x1F;
    }

    public int getMT2()
    {
        return (mMessage1 >> 19) & 0xF;
    }

    /**
     * Group voice call assignment or update, with its source (message 2 valid)
     */
    public boolean isGroupVoice()
    {
        return (getMT1() == MT1_DIGITAL_GROUP_VOICE || getMT1() == MT1_ANALOG_GROUP_VOICE) && hasMessage2();
    }

    /**
     * Digital (ProVoice) group voice call, as opposed to analog
     */
    public boolean isDigital()
    {
        return getMT1() == MT1_DIGITAL_GROUP_VOICE;
    }

    /**
     * Update for a call in progress, as opposed to its first assignment
     */
    public boolean isUpdate()
    {
        return ((mMessage1 >> 16) & 1) == 1;
    }

    public int getLCN()
    {
        return (mMessage1 >> 17) & 0x1F;
    }

    public int getGroup()
    {
        return mMessage1 & 0xFFFF;
    }

    public int getSource()
    {
        return hasMessage2() ? mMessage2 & 0xFFFFF : 0;
    }

    public boolean isEmergency()
    {
        return hasMessage2() && ((mMessage2 >> 20) & 1) == 1;
    }

    public boolean isTransmissionTrunking()
    {
        return hasMessage2() && ((mMessage2 >> 21) & 1) == 1;
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.EDACS;
    }

    @Override
    public int getTimeslot()
    {
        return 0;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }

    @Override
    public String toString()
    {
        if(isGroupVoice())
        {
            return String.format("EDACS EA %s GROUP CALL %s GROUP:%d SOURCE:%d LCN:%d%s%s",
                isDigital() ? "DIGITAL" : "ANALOG", isUpdate() ? "UPDATE" : "ASSIGNMENT", getGroup(), getSource(),
                getLCN(), isEmergency() ? " EMERGENCY" : "", isTransmissionTrunking() ? "" : " MESSAGE TRUNKING");
        }

        return String.format("EDACS EA MT1:%02X%s MSG1:%07X MSG2:%s", getMT1(),
            getMT1() == 0x1F ? " MT2:" + Integer.toHexString(getMT2()).toUpperCase() : "", mMessage1,
            hasMessage2() ? String.format("%07X", mMessage2) : "-");
    }
}
