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

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ChannelNumber;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ISCHSequence;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.ScramblingSequence;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.Timeslot;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.TimeslotFactory;
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
    private List<Timeslot> mChannel0Timeslots;
    private List<Timeslot> mChannel1Timeslots;
    private ScramblingSequence mScramblingSequence;

    /**
     * Constructs a fragment from the message with the specified timestamp.
     *
     * @param message containing 1440-bit super-frame fragment (ie 1/3 of a superframe)
     * @param timestamp of the final bit of this fragment
     */
    public SuperFrameFragment(CorrectedBinaryMessage message, long timestamp, ScramblingSequence scramblingSequence)
    {
        mMessage = message;
        mTimestamp = timestamp;
        mScramblingSequence = scramblingSequence;
    }

    /**
     * Transmitted binary message that represents this fragment
     */
    protected CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * Timestamp for the final bit of this fragment
     */
    @Override
    public long getTimestamp()
    {
        return mTimestamp;
    }

    /**
     * Channel 0 Inter-slot Signalling CHannel (ISCH)
     */
    public InterSlotSignallingChannel getIschChannel0()
    {
        if(mChannel0Isch == null)
        {
            mChannel0Isch = new InterSlotSignallingChannel(
                getMessage().getSubMessage(CHANNEL_A_ISCH_START, TIMESLOT_A_START), ChannelNumber.CHANNEL_0);
        }

        return mChannel0Isch;
    }

    /**
     * Channel 1 Inter-slot Signalling CHannel (ISCH)
     */
    public InterSlotSignallingChannel getIschChannel1()
    {
        if(mChannel1Isch == null)
        {
            mChannel1Isch = new InterSlotSignallingChannel(
                getMessage().getSubMessage(CHANNEL_B_ISCH_START, TIMESLOT_B_START), ChannelNumber.CHANNEL_1);
        }

        return mChannel1Isch;
    }


    /**
     * Channel 0 timeslots
     */
    public List<Timeslot> getChannel0Timeslots()
    {
        if(mChannel0Timeslots == null)
        {
            mChannel0Timeslots = new ArrayList<>();
            mChannel0Timeslots.add(getTimeslot(TIMESLOT_A_START, CHANNEL_B_ISCH_START, 0));

            if(isFinalFragment())
            {
                mChannel0Timeslots.add(getTimeslot(TIMESLOT_D_START, TIMESLOT_D_END, 3));
            }
            else
            {
                mChannel0Timeslots.add(getTimeslot(TIMESLOT_C_START, CHANNEL_D_ISCH_START, 2));
            }
        }

        return mChannel0Timeslots;
    }

    /**
     * Channel 1 timeslots
     */
    public List<Timeslot> getChannel1Timeslots()
    {
        if(mChannel1Timeslots == null)
        {
            mChannel1Timeslots = new ArrayList<>();
            mChannel1Timeslots.add(getTimeslot(TIMESLOT_B_START, CHANNEL_C_ISCH_START, 1));

            if(isFinalFragment())
            {
                mChannel1Timeslots.add(getTimeslot(TIMESLOT_C_START, CHANNEL_D_ISCH_START, 2));
            }
            else
            {
                mChannel1Timeslots.add(getTimeslot(TIMESLOT_D_START, TIMESLOT_D_END, 3));
            }
        }

        return mChannel1Timeslots;
    }

    /**
     * Extracts the timeslot from the parent message and creates a timeslot parser instance
     *
     * @param start bit of the timeslot
     * @param end bit for the message (exclusive)
     * @param index of the timeslot (0-11)
     * @return timeslot parser instance
     */
    private Timeslot getTimeslot(int start, int end, int index)
    {
        CorrectedBinaryMessage message = getMessage().getSubMessage(start, end);
        BinaryMessage timeslotSequence = mScramblingSequence.getTimeslotSequence(getTimeslotOffset() + index);
        return TimeslotFactory.getTimeslot(message, timeslotSequence);
    }

    /**
     * Indicates the timeslot (index) offset to apply to each of the timeslots in this fragment
     */
    private int getTimeslotOffset()
    {
        ISCHSequence sequence0 = getIschChannel0().getIschSequence();

        if(getIschChannel0().isValid())
        {
            return sequence0.getTimeslotOffset();
        }

        ISCHSequence sequence1 = getIschChannel1().getIschSequence();
        if(getIschChannel1().isValid())
        {
            return sequence1.getTimeslotOffset();
        }

        return 0;
    }

    /**
     * Indicates if this fragment is the final fragment (3 of 3) for a superframe which indicates that the final two
     * timeslots are inverted.
     */
    private boolean isFinalFragment()
    {
        ISCHSequence sequence0 = getIschChannel0().getIschSequence();

        if(getIschChannel0().isValid())
        {
            return sequence0.isFinalFragment();
        }

        ISCHSequence sequence1 = getIschChannel1().getIschSequence();
        if(getIschChannel1().isValid())
        {
            return sequence1.isFinalFragment();
        }

        //Can't determine, so higher probability is that it's not the final fragment
        return false;
    }

    /**
     * Indicates if this fragment is valid
     */
    @Override
    public boolean isValid()
    {
        //TODO: can we do validity checking here??
        return true;
    }

    /**
     * Proocol for this fragment
     */
    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25_PHASE2;
    }

    /**
     * No identifiers for this message
     */
    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }

    /**
     * Decoded representation of this fragment
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getIschChannel0());
        sb.append(" | ").append(getIschChannel1());
        sb.append(" | Channel 0:").append(getChannel0Timeslots());
        sb.append(" | Channel 1:").append(getChannel1Timeslots());

        return sb.toString();
//        return getMessage().toHexString() + " SYNC BIT ERRORS:" + getMessage().getCorrectedBitCount() +
//            " | " + getIschChannel0().toString() + " | " + getIschChannel1().toString();
    }
}
