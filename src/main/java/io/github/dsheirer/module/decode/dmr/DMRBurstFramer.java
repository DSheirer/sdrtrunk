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

package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.psk.pll.IPhaseLockedLoop;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.DibitDelayBuffer;
import io.github.dsheirer.dsp.symbol.QPSKCarrierLock;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes a stream of Dibit symbols and performs burst detection and timeslot framing.  This framer
 * also detects abnormal PLL phase locks and issues PLL phase lock corrections.
 *
 * Timeslot Map:
 *  0: Common Association Channel (CACH) and Slow Link Control (SLC)
 *  1. Timeslot 1
 *  2. Timeslot 2
 */
public class DMRBurstFramer implements Listener<Dibit>
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRBurstFramer.class);

    private static final int DMR_SYMBOL_RATE = 4800;
    private static final double PLL_PHASE_CORRECTION_90_DEGREES = (double)DMR_SYMBOL_RATE / 4.0;
    private static final double PLL_PHASE_CORRECTION_180_DEGREES = (double)DMR_SYMBOL_RATE / 2.0;

    private static final int BURST_DIBIT_START_TS1 = 0;
    private static final int BURST_DIBIT_START_TS2 = 144;
    private static final int BURST_DIBIT_LENGTH = 144;
    private static final int TWO_TIMESLOT_DIBIT_LENGTH = BURST_DIBIT_LENGTH * 2;
    private static final int SYNC_DIBIT_OFFSET_TS1 = 66;
    private static final int SYNC_DIBIT_OFFSET_TS2 = 210;
    private static final int SYNC_DIBIT_LENGTH = 24;
    private static final int MAX_STREAMING_SYNC_DETECT_BIT_ERRORS = 4;
    private static final int MAX_EXPLICIT_SYNC_DETECT_BIT_ERRORS = 6;

    /**
     * Threshold for issuing a sync loss message.  This is set to trigger once the dibit count exceeds one second of
     * dibits (4800 baud/dibits) plus two bursts dibit length.  The two burst length padding is so that we don't reset
     * the dibit counter below the burst sync check threshold.
     */
    private static final int SYNC_LOSS_MESSAGE_THRESHOLD = DMR_SYMBOL_RATE + TWO_TIMESLOT_DIBIT_LENGTH;

    /**
     * The message buffer is sized to hold two DMR bursts of 144 dibits (288 bits) each.
     */
    private DibitDelayBuffer mMessageBuffer = new DibitDelayBuffer(BURST_DIBIT_LENGTH * 2);

    /**
     * The sync delay buffer is sized to align sync detection with two bursts being fully loaded into the message and
     * timeslot 1 region of the message buffer is aligned with the sync delay buffer.
     *
     * This delay equals the number of dibits in the second half of the burst payload: 54 dibits (108 bits).
     */
    private DibitDelayBuffer mSyncDelayBuffer = new DibitDelayBuffer(54);

    /**
     * Synchronized indicates that either the primary or secondary sync tracker is currently synchronized to
     * one of the timeslot bursts.  Once either sync tracker obtains a valid sync pattern, this flag is set
     * to true.  The synchronized state will remain true as long as at least one of the two sync trackers
     * maintains sync state.  Once both sync trackers lose sync, the framer will fallback into sync search/detection
     * mode to inspect each dibit until a sync match can be (re)discovered.
     */
    private boolean mSynchronized = false;

    /**
     * Tracks the number of dibits received to trigger burst or sync loss message processing
     */
    private int mDibitCounter = 0;

    /**
     * The Sync detection is used to gain initial lock to a sync pattern or to regain sync if lost.  The sync detector
     * is used to align a DMR burst into the timeslot 1 region of the message dibit delay buffer so that it can be
     * inspected to ensure the timeslots are aligned.
     */
    private DMRSyncDetector mSyncDetectorTimeslot1 = new DMRSyncDetector(MAX_STREAMING_SYNC_DETECT_BIT_ERRORS,
        MAX_EXPLICIT_SYNC_DETECT_BIT_ERRORS);
    private DMRSyncDetector mSyncDetectorTimeslot2 = new DMRSyncDetector(MAX_STREAMING_SYNC_DETECT_BIT_ERRORS,
        MAX_EXPLICIT_SYNC_DETECT_BIT_ERRORS);

    /**
     * Timeslot sync trackers track the synchronization state for each of the two timeslots.
     */
    private SyncTracker mSyncTrackerTimeslot1 = new SyncTracker();
    private SyncTracker mSyncTrackerTimeslot2 = new SyncTracker();

    /**
     * Timeslot alignment tracker tracks that the timeslots in the delay buffer are aligned.
     */
    private TimeslotAlignmentTracker mTimeslotAlignmentTracker = new TimeslotAlignmentTracker();

    private IDMRBurstDetectListener mBurstDetectListener;
    private IPhaseLockedLoop mPhaseLockedLoop;

    /**
     * Constructs an instance
     * @param listener to be notified of framed burst detections and/or sync loss bits processed
     * @param phaseLockedLoop to receive symbol alignment corrections
     */
    public DMRBurstFramer(IDMRBurstDetectListener listener, IPhaseLockedLoop phaseLockedLoop)
    {
        mBurstDetectListener = listener;
        mPhaseLockedLoop = phaseLockedLoop;
    }

    /**
     * Primary dibit symbol input method
     * @param dibit to process
     */
    @Override
    public void receive(Dibit dibit)
    {
        mDibitCounter++;

        //Feed the message buffer first to ensure buffer contains two full bursts when a sync is detected
        mMessageBuffer.put(dibit);

        //Feed the sync delay buffer and reassign the delayed dibit to feed the sync detector if we're not synchronized
        dibit = mSyncDelayBuffer.getAndPut(dibit);

        if(mSynchronized)
        {
            if(mDibitCounter >= TWO_TIMESLOT_DIBIT_LENGTH)
            {
                if(mDibitCounter > TWO_TIMESLOT_DIBIT_LENGTH)
                {
                    processSyncLossDibits(mDibitCounter - TWO_TIMESLOT_DIBIT_LENGTH, 0);
                }

                dispatch();
            }
        }
        else
        {
            mSyncDetectorTimeslot2.add(dibit);

            if(mSyncDetectorTimeslot2.hasSync())
            {
                dispatch();
            }
            else if(mDibitCounter >= SYNC_LOSS_MESSAGE_THRESHOLD)
            {
                processSyncLossDibits(DMR_SYMBOL_RATE, 0);
            }
        }
    }

    /**
     * Dispatches burst 1 and/or burst 2 that is currently in the message buffer
     */
    private void dispatch()
    {
        if(mBurstDetectListener != null)
        {
            if(mDibitCounter > TWO_TIMESLOT_DIBIT_LENGTH)
            {
                processSyncLossDibits(mDibitCounter - TWO_TIMESLOT_DIBIT_LENGTH, 0);
            }

            mSyncDetectorTimeslot1.setCurrentSyncValue(getSyncValue(SYNC_DIBIT_OFFSET_TS1));
            mSyncDetectorTimeslot2.setCurrentSyncValue(getSyncValue(SYNC_DIBIT_OFFSET_TS2));

            mSyncTrackerTimeslot1.update(mSyncDetectorTimeslot1.getSyncPattern(), mSyncDetectorTimeslot1.getCarrierLock());
            mSyncTrackerTimeslot2.update(mSyncDetectorTimeslot2.getSyncPattern(), mSyncDetectorTimeslot2.getCarrierLock());

            CorrectedBinaryMessage burst1 = null;
            CorrectedBinaryMessage burst2 = null;

            CACH cach1 = null;
            CACH cach2 = null;

            //If we have valid sync but PLL tracker misalignment, correct both timeslots before we extract messages
            if((mSyncTrackerTimeslot1.hasSync() && mSyncTrackerTimeslot1.hasCarrierMisAlignment()) ||
               (mSyncTrackerTimeslot2.hasSync() && mSyncTrackerTimeslot2.hasCarrierMisAlignment()))
            {
//                mLog.debug("Repairing Message Bits - PLL Mis-Alignment Detected");
                if(mSyncTrackerTimeslot1.hasCarrierMisAlignment())
                {
                    repairPLLMisalignment(BURST_DIBIT_START_TS1, mSyncTrackerTimeslot1.getCarrierLock());
                    repairPLLMisalignment(BURST_DIBIT_START_TS2, mSyncTrackerTimeslot1.getCarrierLock());
                }
                else
                {
                    repairPLLMisalignment(BURST_DIBIT_START_TS1, mSyncTrackerTimeslot2.getCarrierLock());
                    repairPLLMisalignment(BURST_DIBIT_START_TS2, mSyncTrackerTimeslot2.getCarrierLock());
                }
            }

            if(mSyncTrackerTimeslot1.hasSync())
            {
                burst1 = mMessageBuffer.getMessage(BURST_DIBIT_START_TS1, BURST_DIBIT_LENGTH);
                burst1.incrementCorrectedBitCount(mSyncDetectorTimeslot1.getPatternMatchBitErrorCount());

                if(mSyncTrackerTimeslot1.getSyncPattern().hasCACH())
                {
                    cach1 = CACH.getCACH(burst1);
                }
            }

            if(mSyncTrackerTimeslot2.hasSync())
            {
                burst2 = mMessageBuffer.getMessage(BURST_DIBIT_START_TS2, BURST_DIBIT_LENGTH);
                burst2.incrementCorrectedBitCount(mSyncDetectorTimeslot2.getPatternMatchBitErrorCount());

                if(mSyncTrackerTimeslot2.getSyncPattern().hasCACH())
                {
                    cach2 = CACH.getCACH(burst2);
                }
            }

            //Update the timeslot alignment tracker using the sync patterns and optional CACHes from each timeslot
            mTimeslotAlignmentTracker.update(cach1, cach2, mSyncDetectorTimeslot1.getSyncPattern(),
                mSyncDetectorTimeslot2.getSyncPattern());

            if(mTimeslotAlignmentTracker.hasSufficientData())
            {
                if(mTimeslotAlignmentTracker.isAligned())
                {
                    if(burst1 != null)
                    {
                        mBurstDetectListener.burstDetected(burst1, mSyncTrackerTimeslot1.getSyncPattern(), 1);
                        mDibitCounter -= BURST_DIBIT_LENGTH;
                    }
                    else
                    {
                        processSyncLossDibits(BURST_DIBIT_LENGTH, 1);
                    }

                    if(burst2 != null)
                    {
                        mBurstDetectListener.burstDetected(burst2, mSyncTrackerTimeslot2.getSyncPattern(), 2);
                        mDibitCounter -= BURST_DIBIT_LENGTH;
                    }
                    else
                    {
                        processSyncLossDibits(BURST_DIBIT_LENGTH, 2);
                    }

                    mSynchronized = mSyncTrackerTimeslot1.hasSync() || mSyncTrackerTimeslot2.hasSync();
                }
                else
                {
                    if(burst1 != null)
                    {
                        mBurstDetectListener.burstDetected(burst1, mSyncTrackerTimeslot1.getSyncPattern(), 2);
                        mDibitCounter -= BURST_DIBIT_LENGTH;

                        //Transfer the sync pattern from tracker 1 to tracker 2 so we don't lose voice framing
                        mSyncTrackerTimeslot2.setSyncPattern(mSyncTrackerTimeslot1.getSyncPattern());
                    }
                    else
                    {
                        processSyncLossDibits(BURST_DIBIT_LENGTH, 2);
                        mSyncTrackerTimeslot2.reset();
                    }

                    mTimeslotAlignmentTracker.reset();
                    mSyncTrackerTimeslot1.reset();
                    mDibitCounter = BURST_DIBIT_LENGTH;
                    mSynchronized = true;
                }
            }
            else
            {
                //When we don't have sufficient timeslot tracking data, dump the messages to timeslot 0 so that they
                //don't corrupt the decoder states for either timeslot, until we positively regain timeslot tracking
                if(burst1 != null)
                {
                    mBurstDetectListener.burstDetected(burst1, mSyncTrackerTimeslot1.getSyncPattern(), 0);
                    mDibitCounter -= BURST_DIBIT_LENGTH;
                }
                else
                {
                    processSyncLossDibits(BURST_DIBIT_LENGTH, 0);
                }

                if(burst2 != null)
                {
                    mBurstDetectListener.burstDetected(burst2, mSyncTrackerTimeslot2.getSyncPattern(), 0);
                    mDibitCounter -= BURST_DIBIT_LENGTH;
                }
                else
                {
                    processSyncLossDibits(BURST_DIBIT_LENGTH, 0);
                }

                mSynchronized = mSyncTrackerTimeslot1.hasSync() || mSyncTrackerTimeslot2.hasSync();
            }

            if(!mSynchronized)
            {
                mTimeslotAlignmentTracker.reset();
                mSyncTrackerTimeslot1.reset();
                mSyncTrackerTimeslot2.reset();
            }

            if(mDibitCounter < 0)
            {
                mDibitCounter = 0;
            }
        }
    }

    /**
     * Repairs the captured dibits in the message buffer when a PLL lock misalignment is detected.
     * @param offset to start of burst
     * @param carrierLock that indicates the mis-alignment
     */
    private void repairPLLMisalignment(int offset, QPSKCarrierLock carrierLock)
    {
        int end = offset + BURST_DIBIT_LENGTH;

        for(int x = offset; x < end; x++)
        {
            Dibit misalignedDibit = mMessageBuffer.get(x);
            mMessageBuffer.set(x, carrierLock.correct(misalignedDibit));
        }

        if(mPhaseLockedLoop != null)
        {
//            mLog.warn("Repairing PLL Phase Lock Mis-Alignment - Detected: " + carrierLock);
            switch(carrierLock)
            {
                case PLUS_90:
                    mPhaseLockedLoop.correctInversion(PLL_PHASE_CORRECTION_90_DEGREES);
                    break;
                case MINUS_90:
                    mPhaseLockedLoop.correctInversion(-PLL_PHASE_CORRECTION_90_DEGREES);
                    break;
                case INVERTED:
                    mPhaseLockedLoop.correctInversion(PLL_PHASE_CORRECTION_180_DEGREES);
                    break;
            }
        }
    }

    /**
     * Resets the message and sync delay buffers and resets the dibit counter to zero.
     */
    public void reset()
    {
        mDibitCounter = 0;
        mMessageBuffer.reset();
        mSyncDelayBuffer.reset();
        mTimeslotAlignmentTracker.reset();
    }

    /**
     * Calculates the value of the sync field currently in the message buffer as specified by the offset value.
     */
    private long getSyncValue(int dibitOffset)
    {
        Dibit[] syncDibits = mMessageBuffer.getBuffer(dibitOffset, SYNC_DIBIT_LENGTH);

        long value = 0;

        for(Dibit dibit: syncDibits)
        {
            value = Long.rotateLeft(value, 2);
            value += dibit.getValue();
        }

        return value;
    }

    /**
     * Processes dibits received without a sync condition
     * @param dibitCount with sync loss
     */
    private void processSyncLossDibits(int dibitCount, int timeslot)
    {
        mDibitCounter -= dibitCount;

        if(mBurstDetectListener != null)
        {
            mBurstDetectListener.syncLost(dibitCount * 2, timeslot);
        }
    }

    /**
     * Tracks the sync state for a timeslot to account for voice framing that does not transmit a sync pattern in each
     * frame.
     */
    public class SyncTracker
    {
        private DMRSyncPattern mSyncPattern = DMRSyncPattern.UNKNOWN;
        private QPSKCarrierLock mCarrierLock = QPSKCarrierLock.NORMAL;

        /**
         * Updates the sync pattern for the most recently detected burst to the specified pattern.  When the previous
         * sync pattern is one of either base or mobile voice frame and the detected pattern is unknown, automatically
         * advances the sync pattern to the next voice frame in the super frame sequence to account for voice
         * frames B - F that do not have a sync pattern.
         */
        public void update(DMRSyncPattern pattern, QPSKCarrierLock carrierLock)
        {
            if(pattern == DMRSyncPattern.UNKNOWN)
            {
                switch(mSyncPattern)
                {
                    case BASE_STATION_VOICE: //Voice frame A
                        mSyncPattern = DMRSyncPattern.BS_VOICE_FRAME_B;
                        break;
                    case BS_VOICE_FRAME_B:
                        mSyncPattern = DMRSyncPattern.BS_VOICE_FRAME_C;
                        break;
                    case BS_VOICE_FRAME_C:
                        mSyncPattern = DMRSyncPattern.BS_VOICE_FRAME_D;
                        break;
                    case BS_VOICE_FRAME_D:
                        mSyncPattern = DMRSyncPattern.BS_VOICE_FRAME_E;
                        break;
                    case BS_VOICE_FRAME_E:
                        mSyncPattern = DMRSyncPattern.BS_VOICE_FRAME_F;
                        break;

                    case MOBILE_STATION_VOICE: //Voice frame A
                    case DIRECT_MODE_VOICE_TIMESLOT_1:
                    case DIRECT_MODE_VOICE_TIMESLOT_2:
                        mSyncPattern = DMRSyncPattern.MS_VOICE_FRAME_B;
                        break;
                    case MS_VOICE_FRAME_B:
                        mSyncPattern = DMRSyncPattern.MS_VOICE_FRAME_C;
                        break;
                    case MS_VOICE_FRAME_C:
                        mSyncPattern = DMRSyncPattern.MS_VOICE_FRAME_D;
                        break;
                    case MS_VOICE_FRAME_D:
                        mSyncPattern = DMRSyncPattern.MS_VOICE_FRAME_E;
                        break;
                    case MS_VOICE_FRAME_E:
                        mSyncPattern = DMRSyncPattern.MS_VOICE_FRAME_F;
                        break;
                    default:
                        mSyncPattern = pattern;
                        mCarrierLock = carrierLock;
                        break;
                }
            }
            else
            {
                mSyncPattern = pattern;
                mCarrierLock = carrierLock;
            }
        }

        /**
         * Most recently detected sync pattern
         */
        public DMRSyncPattern getSyncPattern()
        {
            return mSyncPattern;
        }

        /**
         * Explicitly sets the pattern for this tracker
         */
        public void setSyncPattern(DMRSyncPattern pattern)
        {
            mSyncPattern = pattern;
        }

        /**
         * Carrier lock as indicated by the sync detector.
         */
        public QPSKCarrierLock getCarrierLock()
        {
            return mCarrierLock;
        }

        /**
         * Indicates if the burst has a QPSK constellation mis-alignment that has to be corrected
         */
        public boolean hasCarrierMisAlignment()
        {
            return getCarrierLock() != QPSKCarrierLock.NORMAL;
        }

        /**
         * Checks the contents of the message buffer to determine if the message has a valid sync pattern, or if the
         * message is a continuation voice frame from a voice super frame.  The identified sync pattern and number of
         * bit errors for that pattern are stored in the sync detector so that they can be accessed for dispatching
         * a message.
         */
        public boolean hasSync()
        {
            return getSyncPattern() != DMRSyncPattern.UNKNOWN;
        }

        /**
         * Resets the sync pattern to unknown
         */
        public void reset()
        {
            mSyncPattern = DMRSyncPattern.UNKNOWN;
        }
    }
}
