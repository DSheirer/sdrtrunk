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
import io.github.dsheirer.dsp.c4fm.C4FMMessageFramer;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.DibitDelayLine;
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
 * <p>
 * Supports two symbol stream inputs:
 * <ul>
 *     <li>Listener receive(Dibit) and syncDetected(DMRSyncPattern) where the sync detection is signalled when the
 *     next dibit is the first dibit of the burst (ie the symbol processor maintains a burst delay line)</li>
 *     <li>C4FM message framer process(Dibit) and syncDetected() where the sync detection is signalled when the most
 *     recent dibit is the final dibit of the sync pattern.  This framer maintains a delay line to retain the burst
 *     dibits from CACH through the sync pattern and the sync pattern must be provided via setSyncPattern() prior to
 *     the sync detection notification.</li>
 * </ul>
 */
public class DMRMessageFramer extends C4FMMessageFramer implements Listener<Dibit>
{
    private static final int DIBIT_CACH_START = 0;
    private static final int DIBIT_BURST_END = 144;
    //Dibit delay line sizing: CACH(12) + MESSAGE_PREFIX(54) + SYNC(24)
    private static final int DELAY_LINE_LENGTH = 90;
    private final DibitDelayLine mDibitDelayLine = new DibitDelayLine(DELAY_LINE_LENGTH);
    //Soft symbols (radians) delay line parallel to the dibit delay line, or NaN when the soft symbol wasn't provided
    private final float[] mSoftSymbolDelayLine = new float[DELAY_LINE_LENGTH];
    private int mSoftSymbolDelayLinePointer = 0;
    private DMRSyncPattern mSyncPattern = DMRSyncPattern.UNKNOWN;
    private Listener<IMessage> mMessageListener;
    private final Dibit[] mBufferA = new Dibit[144];
    private final Dibit[] mBufferB = new Dibit[144];
    //Soft symbols (radians) parallel to the dibit buffers, or NaN when the soft symbol wasn't provided
    private final float[] mSoftBufferA = new float[144];
    private final float[] mSoftBufferB = new float[144];
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
        Arrays.fill(mSoftSymbolDelayLine, Float.NaN);
        Arrays.fill(mSoftBufferA, Float.NaN);
        Arrays.fill(mSoftBufferB, Float.NaN);
        mMessageFactory = new DMRMessageFactory(crcMaskManager);
    }

    /**
     * Sets the sync pattern for the next C4FM sync detection notification via syncDetected().
     * @param pattern that was detected.
     */
    public void setSyncPattern(DMRSyncPattern pattern)
    {
        mSyncPattern = pattern;
    }

    /**
     * C4FM symbol stream input method.  Dibits are delayed via the burst delay line before framing.
     * @param symbol to process
     * @return true if a valid message was dispatched.
     */
    @Override
    public boolean process(Dibit symbol)
    {
        return process(symbol, Float.NaN);
    }

    /**
     * C4FM symbol stream input method.  Dibits and soft symbols are delayed via the burst delay lines before framing.
     * @param symbol decision
     * @param softSymbol equalized soft symbol in radians
     * @return true if a valid message was dispatched.
     */
    @Override
    public boolean process(Dibit symbol, float softSymbol)
    {
        float ejectedSoftSymbol = mSoftSymbolDelayLine[mSoftSymbolDelayLinePointer];
        mSoftSymbolDelayLine[mSoftSymbolDelayLinePointer++] = softSymbol;
        mSoftSymbolDelayLinePointer %= DELAY_LINE_LENGTH;
        return processDibit(mDibitDelayLine.insert(symbol), ejectedSoftSymbol);
    }

    /**
     * C4FM sync detection notification where the most recently processed dibit is the final dibit of the sync pattern
     * that was set via setSyncPattern().  Overwrites the sync dibits in the delay line with the detected sync pattern
     * to eliminate any sync bit errors and signals that the next dibit ejected from the delay line is the first dibit
     * of the burst.
     */
    @Override
    public void syncDetected()
    {
        if(DMRSyncPattern.SYNC_PATTERNS.contains(mSyncPattern))
        {
            mDibitDelayLine.update(mSyncPattern.toDibits());
            syncDetected(mSyncPattern);
        }
    }

    /**
     * Primary input method for receiving the stream of demodulated dibit symbols.
     * @param dibit to process.
     */
    @Override
    public void receive(Dibit dibit)
    {
        processDibit(dibit, Float.NaN);
    }

    /**
     * Processes the dibit
     * @param dibit to process
     * @param softSymbol for the dibit in radians, or NaN.
     * @return true if a valid message was dispatched.
     */
    private boolean processDibit(Dibit dibit, float softSymbol)
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
                mSoftBufferA[mBufferAPointer] = softSymbol;
                mBufferA[mBufferAPointer++] = dibit;

                if(mBufferAPointer >= mBufferA.length)
                {
                    return dispatchBufferA();
                }
            }
            else
            {
                mSoftBufferB[mBufferBPointer] = softSymbol;
                mBufferB[mBufferBPointer++] = dibit;

                if(mBufferBPointer >= mBufferB.length)
                {
                    return dispatchBufferB();
                }
            }
        }

        return false;
    }

    /**
     * Indicates if the framer is assembling a burst and the active timeslot is assembling a voice superframe.
     */
    public boolean isVoiceSuperFrame()
    {
        return mAssemblingBurst && ((mBufferAActive && mBufferAPattern.isVoicePattern()) ||
                                    (!mBufferAActive && mBufferBPattern.isVoicePattern()));
    }

    /**
     * Dispatches the burst assembled in buffer A.
     * @return true if the dispatched burst message is valid.
     */
    private boolean dispatchBufferA()
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
        float[] softBits = getSoftBits(mSoftBufferA);
        CACH cach = CACH.getCACH(message, softBits);

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

        IMessage burstMessage = mMessageFactory.create(mBufferAPattern, message, softBits, cach,
                getTimestamp(), mBufferATimeslot);
        dispatch(burstMessage);

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

        return burstMessage != null && burstMessage.isValid();
    }

    /**
     * Dispatches the burst assembled in buffer B.
     * @return true if the dispatched burst message is valid.
     */
    private boolean dispatchBufferB()
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
        float[] softBits = getSoftBits(mSoftBufferB);
        CACH cach = CACH.getCACH(burst, softBits);

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

        IMessage burstMessage = mMessageFactory.create(mBufferBPattern, burst, softBits, cach,
                getTimestamp(), mBufferBTimeslot);
        dispatch(burstMessage);

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

        return burstMessage != null && burstMessage.isValid();
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
     * Converts the soft symbol buffer into soft bits (one per message bit) where a positive value favors a 1 bit.
     * @param softSymbols buffer
     * @return soft bits or null if any of the soft symbols were not provided.
     */
    private static float[] getSoftBits(float[] softSymbols)
    {
        float[] softBits = new float[softSymbols.length * 2];

        for(int x = 0; x < softSymbols.length; x++)
        {
            if(Float.isNaN(softSymbols[x]))
            {
                return null;
            }

            setSoftBits(softSymbols[x], softBits, x * 2);
        }

        return softBits;
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
