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

package io.github.dsheirer.module.decode.p25.phase2.message;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.isch.IISCH;
import io.github.dsheirer.module.decode.p25.phase2.message.isch.ISCHDecoder;
import io.github.dsheirer.module.decode.p25.phase2.message.isch.SISCH;
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

    private static final ISCHDecoder ISCH_DECODER = new ISCHDecoder();
    private long mTimestamp;
    private CorrectedBinaryMessage mMessage;
    private IISCH mIISCH1;
    private IISCH mIISCH2;
    private SISCH mSISCH1;
    private SISCH mSISCH2;
    private Timeslot mTimeslotA;
    private Timeslot mTimeslotB;
    private Timeslot mTimeslotC;
    private Timeslot mTimeslotD;
    private List<Timeslot> mTimeslots;
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
     * Unused.  Implements the parent class abstract method.
     */
    @Override
    public int getTimeslot()
    {
        return 0;
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
     * IISCH Timeslot 1
     */
    public IISCH getIISCH1()
    {
        if(mIISCH1 == null)
        {
            CorrectedBinaryMessage message = ISCH_DECODER.decode(getMessage()
                    .getSubMessage(CHANNEL_A_ISCH_START, TIMESLOT_A_START), P25P2Message.TIMESLOT_1);
            mIISCH1 = new IISCH(message, 0, 1, getTimestamp());
            mIISCH1.setValid(message.getCorrectedBitCount() < 3);
        }

        return mIISCH1;
    }

    /**
     * IISCH Timeslot 2
     */
    public IISCH getIISCH2()
    {
        if(mIISCH2 == null)
        {
            CorrectedBinaryMessage message = ISCH_DECODER.decode(getMessage()
                    .getSubMessage(CHANNEL_B_ISCH_START, TIMESLOT_B_START), P25P2Message.TIMESLOT_2);
            mIISCH2 = new IISCH(message, 0, 2, getTimestamp());
            mIISCH2.setValid(message.getCorrectedBitCount() < 3);
        }

        return mIISCH2;
    }

    /**
     * SISCH Timeslot 1
     */
    public SISCH getSISCH1()
    {
        if(mSISCH1 == null)
        {
            CorrectedBinaryMessage message = getMessage().getSubMessage(CHANNEL_C_ISCH_START, TIMESLOT_C_START);
            mSISCH1 = new SISCH(message, 0, 1, getTimestamp());
        }

        return mSISCH1;
    }

    /**
     * SISCH Timeslot 2
     */
    public SISCH getSISCH2()
    {
        if(mSISCH2 == null)
        {
            CorrectedBinaryMessage message = getMessage().getSubMessage(CHANNEL_D_ISCH_START, TIMESLOT_D_START);
            mSISCH2 = new SISCH(message, 0, 2, getTimestamp());
        }

        return mSISCH2;
    }

    /**
     * Reset the timeslots when the scrambling code has been updated so that any scrambled timeslots can be correctly
     * decoded.
     */
    public void resetTimeslots()
    {
        mTimeslots = null;
        mTimeslotA = null;
        mTimeslotB = null;
        mTimeslotC = null;
        mTimeslotD = null;
    }

    /**
     * Timeslot A
     */
    public Timeslot getTimeslotA()
    {
        if(mTimeslotA == null)
        {
            mTimeslotA = getTimeslot(TIMESLOT_A_START, CHANNEL_B_ISCH_START, 0, 1);
        }

        return mTimeslotA;
    }

    /**
     * Timeslot B
     */
    public Timeslot getTimeslotB()
    {
        if(mTimeslotB == null)
        {
            mTimeslotB = getTimeslot(TIMESLOT_B_START, CHANNEL_C_ISCH_START, 1, 2);
        }

        return mTimeslotB;
    }

    /**
     * Timeslot C
     */
    public Timeslot getTimeslotC()
    {
        if(mTimeslotC == null)
        {
            mTimeslotC = getTimeslot(TIMESLOT_C_START, CHANNEL_D_ISCH_START, 2, isFinalFragment() ? 2 : 1);
        }

        return mTimeslotC;
    }

    /**
     * Timeslot D
     */
    public Timeslot getTimeslotD()
    {
        if(mTimeslotD == null)
        {
            mTimeslotD = getTimeslot(TIMESLOT_D_START, TIMESLOT_D_END, 3, isFinalFragment() ? 1 : 2);
        }

        return mTimeslotD;
    }

    public List<Timeslot> getTimeslots()
    {
        if(mTimeslots == null)
        {
            mTimeslots = new ArrayList<>();
            mTimeslots.add(getTimeslotA());
            mTimeslots.add(getTimeslotB());

            if(isFinalFragment())
            {
                mTimeslots.add(getTimeslotD());
                mTimeslots.add(getTimeslotC());
            }
            else
            {
                mTimeslots.add(getTimeslotC());
                mTimeslots.add(getTimeslotD());
            }
        }

        return mTimeslots;
    }

    /**
     * Channel 1 timeslots
     */
    public List<Timeslot> getChannel1Timeslots()
    {
        List<Timeslot> timeslots = new ArrayList<>();
        timeslots.add(getTimeslotA());

        if(isFinalFragment())
        {
            timeslots.add(getTimeslotD());
        }
        else
        {
            timeslots.add(getTimeslotC());
        }

        return timeslots;
    }

    /**
     * Channel 2 timeslots
     */
    public List<Timeslot> getChannel2Timeslots()
    {
        List<Timeslot> timeslots = new ArrayList<>();

        timeslots.add(getTimeslotB());

        if(isFinalFragment())
        {
            timeslots.add(getTimeslotC());
        }
        else
        {
            timeslots.add(getTimeslotD());
        }

        return timeslots;
    }

    /**
     * Extracts the timeslot from the parent message and creates a timeslot parser instance
     *
     * @param start bit of the timeslot
     * @param end bit for the message (exclusive)
     * @param index of the timeslot (0-11)
     * @return timeslot parser instance
     */
    private Timeslot getTimeslot(int start, int end, int index, int timeslot)
    {
        CorrectedBinaryMessage message = getMessage().getSubMessage(start, end);
        BinaryMessage timeslotSequence = mScramblingSequence.getTimeslotSequence(getTimeslotOffset() + index);
        return TimeslotFactory.getTimeslot(message, timeslotSequence, timeslot, getTimestamp());
    }

    /**
     * Indicates the timeslot (index) offset to apply to each of the timeslots in this fragment
     */
    private int getTimeslotOffset()
    {
        if(getIISCH1().isValid())
        {
            return getIISCH1().getIschSequence().getTimeslotOffset();
        }

        if(getIISCH1().isValid())
        {
            return getIISCH2().getIschSequence().getTimeslotOffset();
        }

        return 0;
    }

    /**
     * Indicates if this fragment is the final fragment (3 of 3) for a superframe which indicates that the final two
     * timeslots are inverted.
     */
    private boolean isFinalFragment()
    {
        if(getIISCH1().isValid())
        {
            return getIISCH1().getIschSequence().isFinalFragment();
        }

        if(getIISCH2().isValid())
        {
            return getIISCH2().getIschSequence().isFinalFragment();
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
        sb.append(getIISCH1());
        sb.append(" | ").append(getIISCH2());
        sb.append(" | Channel 1:").append(getChannel1Timeslots());
        sb.append(" | Channel 2:").append(getChannel2Timeslots());
        return sb.toString();
    }
}
