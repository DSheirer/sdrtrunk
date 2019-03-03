/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.module.decode.p25.phase2.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.Timeslot;
import io.github.dsheirer.protocol.Protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * APCO25 Phase 2 SuperFrame fragment containing 4 timeslots.  Each superframe contains 12 timeslots and
 * this fragment represents 1/3 of a superframe.
 */
public class SuperFrameFragment implements IMessage
{
    private static final int CHANNEL_A_ISCH_START = 0;
    private static final int TIMESLOT_A_START = 40;

    private static final int CHANNEL_B_ISCH_START = 360;
    private static final int TIMESLOT_B_START = 400;

    private static final int CHANNEL_C_ISCH_START = 720;
    private static final int TIMESLOT_C_START = 760;

    private static final int CHANNEL_D_ISCH_START = 1080;
    private static final int TIMESLOT_D_START = 1120;
    private static final int TIMESLOT_D_END = 1440;

    private long mTimestamp;
    private CorrectedBinaryMessage mMessage;
    private InterSlotSignallingChannel mChannel0Isch;
    private InterSlotSignallingChannel mChannel1Isch;
    private List<Timeslot> mTimeslots;

    public SuperFrameFragment(CorrectedBinaryMessage message, long timestamp)
    {
        mMessage = message;
        mTimestamp = timestamp;
    }

    protected CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    @Override
    public long getTimestamp()
    {
        return mTimestamp;
    }

    public List<Timeslot> getTimeslots()
    {
        if(mTimeslots == null)
        {
            mTimeslots = new ArrayList<>();
            mTimeslots.add(new Timeslot(getMessage().getSubMessage(TIMESLOT_A_START, CHANNEL_B_ISCH_START)));
            mTimeslots.add(new Timeslot(getMessage().getSubMessage(TIMESLOT_B_START, CHANNEL_C_ISCH_START)));
            mTimeslots.add(new Timeslot(getMessage().getSubMessage(TIMESLOT_C_START, CHANNEL_D_ISCH_START)));
            mTimeslots.add(new Timeslot(getMessage().getSubMessage(TIMESLOT_D_START, TIMESLOT_D_END)));
        }

        return mTimeslots;
    }

    @Override
    public boolean isValid()
    {
        //TODO: can we do validity checking here??
        return true;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25_PHASE2;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }

    public String toString()
    {
//        StringBuilder sb = new StringBuilder();
//        sb.append("\n");
//        sb.append(getMessage().toHexString()).append("\n");
//        sb.append(getMessage().getSubMessage(CHANNEL_A_ISCH_START, TIMESLOT_A_START).toHexString()).append(" ");
//        sb.append(getMessage().getSubMessage(TIMESLOT_A_START, CHANNEL_B_ISCH_START).toHexString()).append(" ");
//        sb.append(getMessage().getSubMessage(CHANNEL_B_ISCH_START, TIMESLOT_B_START).toHexString()).append(" ");
//        sb.append(getMessage().getSubMessage(TIMESLOT_B_START, CHANNEL_C_ISCH_START).toHexString()).append(" ");
//        sb.append(getMessage().getSubMessage(CHANNEL_C_ISCH_START, TIMESLOT_C_START).toHexString()).append(" ");
//        sb.append(getMessage().getSubMessage(TIMESLOT_C_START, CHANNEL_D_ISCH_START).toHexString()).append(" ");
//        sb.append(getMessage().getSubMessage(CHANNEL_D_ISCH_START, TIMESLOT_D_START).toHexString()).append(" ");
//        sb.append(getMessage().getSubMessage(TIMESLOT_D_START, TIMESLOT_D_END).toHexString()).append(" ");
//        return sb.toString();
            return getMessage().toHexString() + " SYNC BIT ERRORS:" + getMessage().getCorrectedBitCount();
    }
}
