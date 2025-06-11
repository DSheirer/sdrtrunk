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

package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import io.github.dsheirer.module.decode.dmr.message.DMRMessageFactory;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Listener;
import java.util.Arrays;

/**
 * DMR message framer.  Processes a DMR symbol stream to produce DMR messages/bursts.  Employs two symbol buffers to
 * track and frame both DMR timeslots.  Responds to external sync detections to alternately activate one of the two
 * buffers and capture a DMR burst, convert it to the correct message format and send it to a message listener.
 * <p>
 * This framer is able to detect DMR Direct mode and track one active and one inactive timeslot across each voice
 * super frame in addition to framing base and mobile station data and voice bursts.
 */
public class DMRMessageFramer implements Listener<Dibit>
{
    private static final int DIBIT_CACH_START = 0;
    private static final int DIBIT_BURST_END = 144;
    private Listener<IMessage> mMessageListener;
    private final Dibit[] mBufferA = new Dibit[144];
    private final Dibit[] mBufferB = new Dibit[144];
    private int mBufferAPointer = 0;
    private int mBufferBPointer = 0;
    private DMRSyncPattern mBufferAPattern = DMRSyncPattern.UNKNOWN;
    private DMRSyncPattern mBufferBPattern = DMRSyncPattern.UNKNOWN;
    private int mBufferATimeslot;
    private int mBufferBTimeslot;
    private boolean mBufferAActive = false;
    private boolean mAssemblingBurst = false;
    private int mDibitCounter = 0;
    private int mDibitSinceTimestampCounter = 0;
    private long mReferenceTimestamp = 0;
    private boolean mRunning = false;
    private final DMRMessageFactory mMessageFactory;

    /**
     * Constructs an instance
     */
    public DMRMessageFramer(DMRCrcMaskManager crcMaskManager)
    {
        Arrays.fill(mBufferA, Dibit.D00_PLUS_1);
        Arrays.fill(mBufferB, Dibit.D00_PLUS_1);
        mMessageFactory = new DMRMessageFactory(crcMaskManager);
    }

    /**
     * Primary input method for receiving the stream of demodulated dibit symbols.
     * @param dibit to process.
     */
    @Override
    public void receive(Dibit dibit)
    {
        mDibitCounter++;
        mDibitSinceTimestampCounter++;

        //Issue a sync loss for each full 1-second interval (ie 4800 dibits) of no sync.
        if(!mAssemblingBurst && mDibitCounter >= 4800)
        {
            dispatch(new SyncLossMessage(getTimestamp(), 9600, Protocol.DMR, DMRMessage.TIMESLOT_0));
            mDibitCounter -= 4800;
            mBufferAPattern = DMRSyncPattern.UNKNOWN;
            mBufferBPattern = DMRSyncPattern.UNKNOWN;
        }

        if(mAssemblingBurst)
        {
            if(mBufferAActive)
            {
                mBufferA[mBufferAPointer++] = dibit;

                if(mBufferAPointer >= mBufferA.length)
                {
                    dispatchBufferA();
                }
            }
            else
            {
                mBufferB[mBufferBPointer++] = dibit;

                if(mBufferBPointer >= mBufferB.length)
                {
                    dispatchBufferB();
                }
            }
        }
    }

    /**
     * Indicates if the framer is assembling a burst and the active timeslot is assembling a voice superframe.
     */
    public boolean isVoiceSuperFrame()
    {
        return mAssemblingBurst && ((mBufferAActive && mBufferAPattern.isVoicePattern()) ||
                                    (!mBufferAActive && mBufferBPattern.isVoicePattern()));
    }

    private void dispatchBufferA()
    {
        mDibitCounter -= 144;

        if(mDibitCounter > 0)
        {
            //If there are 144 dibits then tag the sync loss to other timeslot, otherwise dump it to timeslot 0.
            if(mDibitCounter == 144)
            {
                //Note: since buffer b got skipped, we'll use buffer a timeslot and then it will get reassigned from
                //the CACH to the correct timeslot.
                dispatch(new SyncLossMessage(getTimestamp(), 288, Protocol.DMR, mBufferATimeslot));
            }
            else
            {
                dispatch(new SyncLossMessage(getTimestamp(), mDibitCounter * 2, Protocol.DMR, DMRMessage.TIMESLOT_0));

                if(mDibitCounter > 1 && !mBufferAPattern.isDirect())
                {
                    mBufferATimeslot = 0;
                    mBufferBTimeslot = 0;
                    mBufferBPattern = DMRSyncPattern.UNKNOWN;
                }
            }
        }

        mDibitCounter = 0;

        CorrectedBinaryMessage message = getMessage(mBufferA);
        CACH cach = CACH.getCACH(message);

        if(mBufferAPattern.hasCACH() && cach.isValid() && mBufferATimeslot != cach.getTimeslot())
        {
            mBufferATimeslot = cach.getTimeslot();
            mBufferBTimeslot = (mBufferATimeslot == 1 ? 2 : 1);
        }
        else if(mBufferAPattern.isMobileStationSyncPattern())
        {
            mBufferATimeslot = 1;
            mBufferBTimeslot = 2;
            if(mBufferBPattern == DMRSyncPattern.UNKNOWN)
            {
                mBufferBPattern = DMRSyncPattern.DIRECT_EMPTY_TIMESLOT;
            }
        }

        dispatch(mMessageFactory.create(mBufferAPattern, message, cach, getTimestamp(), mBufferATimeslot));

        //Since voice frames only have sync on the first burst, we use pseudo-patterns to track the rest of the bursts
        //across the voice super-frame.  If the current pattern is a voice frame, set it to the next pseudo voice sync.
        if(mBufferAPattern.isVoicePattern())
        {
            mBufferAPattern = DMRSyncPattern.getNextVoice(mBufferAPattern);
        }

        //Automatically trigger buffer B burst collection if it is collecting a voice super frame or mobile direct.
        if(mBufferBPattern.isVoicePattern() || mBufferBPattern == DMRSyncPattern.DIRECT_EMPTY_TIMESLOT)
        {
            mAssemblingBurst = true;
            mBufferAActive = false;
            mBufferBPointer = 0;
        }
        else
        {
            mAssemblingBurst = false;
        }
    }

    private void dispatchBufferB()
    {
        mDibitCounter -= 144;

        if(mDibitCounter > 0)
        {
            //If there are 144 dibits then tag the sync loss to other timeslot, otherwise dump it to timeslot 0.
            if(mDibitCounter == 144)
            {
                //Note: since buffer a got skipped, we'll use buffer b timeslot and then it will get reassigned from
                //the CACH to the correct timeslot.
                dispatch(new SyncLossMessage(getTimestamp(), 288, Protocol.DMR, mBufferBTimeslot));
            }
            else
            {
                dispatch(new SyncLossMessage(getTimestamp(), mDibitCounter * 2, Protocol.DMR, DMRMessage.TIMESLOT_0));

                if(mDibitCounter > 1 && !mBufferBPattern.isDirect())
                {
                    mBufferATimeslot = 0;
                    mBufferBTimeslot = 0;
                    mBufferAPattern = DMRSyncPattern.UNKNOWN;
                }
            }
        }

        mDibitCounter = 0;

        CorrectedBinaryMessage burst = getMessage(mBufferB);
        CACH cach = CACH.getCACH(burst);

        if(mBufferBPattern.hasCACH() && cach.isValid() && mBufferBTimeslot != cach.getTimeslot())
        {
            mBufferBTimeslot = cach.getTimeslot();
            mBufferATimeslot = (mBufferBTimeslot == 1 ? 2 : 1);
        }
        else if(mBufferBPattern.isMobileStationSyncPattern())
        {
            mBufferBTimeslot = 1;
            mBufferATimeslot = 2;

            if(mBufferAPattern == DMRSyncPattern.UNKNOWN)
            {
                mBufferAPattern = DMRSyncPattern.DIRECT_EMPTY_TIMESLOT;
            }
        }

        dispatch(mMessageFactory.create(mBufferBPattern, burst, cach, getTimestamp(), mBufferBTimeslot));

        //Since voice frames only have sync on the first burst, we use pseudo-patterns to track the rest of the bursts
        //across the voice super-frame.  If the current pattern is a voice frame, set it to the next pseudo voice sync.
        if(mBufferBPattern.isVoicePattern())
        {
            mBufferBPattern = DMRSyncPattern.getNextVoice(mBufferBPattern);
        }

        //Automatically trigger buffer A burst collection if it is collecting a voice super frame or mobile direct.
        if(mBufferAPattern.isVoicePattern() || mBufferAPattern == DMRSyncPattern.DIRECT_EMPTY_TIMESLOT)
        {
            mAssemblingBurst = true;
            mBufferAActive = true;
            mBufferAPointer = 0;
        }
        else
        {
            mAssemblingBurst = false;
        }
    }

    /**
     * Dispatches the message to the registered message listener.
     * @param message to dispatch
     */
    private void dispatch(IMessage message)
    {
        if(mRunning && mMessageListener != null)
        {
            mMessageListener.receive(message);
        }
    }

    /**
     * Starts this framer dispatching messages
     */
    public void start()
    {
        mRunning = true;
    }

    /**
     * Stops this framer from dispatching messages
     */
    public void stop()
    {
        mRunning = false;
    }

    /**
     * Sets the listener to receive framed DMR messages.
     * @param listener for messages.
     */
    public void setListener(Listener<IMessage> listener)
    {
        mMessageListener = listener;
    }

    /**
     * Creates a binary message from the dibit buffer.
     *
     * @param buffer containing dibits
     * @return binary message
     */
    private CorrectedBinaryMessage getMessage(Dibit[] buffer)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(2 * (DMRMessageFramer.DIBIT_BURST_END - DMRMessageFramer.DIBIT_CACH_START));

        Dibit dibit;
        for(int i = DMRMessageFramer.DIBIT_CACH_START; i < DMRMessageFramer.DIBIT_BURST_END; i++)
        {
            dibit = buffer[i];
            message.add(dibit.getBit1(), dibit.getBit2());
        }

        return message;
    }

    /**
     * Externally triggered sync detection to initiate DMR burst capture.  Indicates a sync pattern is detected and the
     * next received dibit symbol is the first dibit of the DMR burst.
     * @param pattern detected
     */
    public void syncDetected(DMRSyncPattern pattern)
    {
        if(mAssemblingBurst)
        {
            if(mBufferAActive && mBufferAPointer < mBufferA.length)
            {
                mBufferAPointer = 144;
                mBufferAPattern = DMRSyncPattern.UNKNOWN;
            }
            else if(!mBufferAActive && mBufferBPointer < mBufferB.length)
            {
                mBufferBPointer = 144;
                mBufferBPattern = DMRSyncPattern.UNKNOWN;
            }
        }

        mAssemblingBurst = true;

        if(mBufferAActive)
        {
            mBufferAActive = false;
            mBufferBPointer = 0;
            mBufferBPattern = pattern;

            if(pattern.isDirect())
            {
                if(pattern.isDirectTS1())
                {
                    mBufferATimeslot = 2;
                    mBufferBTimeslot = 1;
                }
                else if(pattern.isDirectTS2())
                {
                    mBufferATimeslot = 1;
                    mBufferBTimeslot = 2;
                }

                if(mBufferAPattern == DMRSyncPattern.UNKNOWN)
                {
                    mBufferAPattern = DMRSyncPattern.DIRECT_EMPTY_TIMESLOT;
                }
            }
        }
        else
        {
            mBufferAActive = true;
            mBufferAPointer = 0;
            mBufferAPattern = pattern;

            if(pattern.isDirect())
            {
                if(pattern.isDirectTS1())
                {
                    mBufferATimeslot = 1;
                    mBufferBTimeslot = 2;
                }
                else if(pattern.isDirectTS2())
                {
                    mBufferATimeslot = 2;
                    mBufferBTimeslot = 1;
                }

                if(mBufferBPattern == DMRSyncPattern.UNKNOWN)
                {
                    mBufferBPattern = DMRSyncPattern.DIRECT_EMPTY_TIMESLOT;
                }
            }
        }
    }

    /**
     * Sets or updates the current dibit stream time from an incoming sample buffer.
     * @param time to use as a reference timestamp.
     */
    public void setTimestamp(long time)
    {
        mReferenceTimestamp = time;
        mDibitSinceTimestampCounter = 0;
    }

    /**
     * Calculates the timestamp accurate to the currently received dibit.
     * @return timestamp in milliseconds.
     */
    private long getTimestamp()
    {
        if(mReferenceTimestamp > 0)
        {
            return mReferenceTimestamp + (long)(1000.0 * mDibitSinceTimestampCounter / 4800);
        }
        else
        {
            mDibitSinceTimestampCounter = 0;
            return System.currentTimeMillis();
        }
    }
}
