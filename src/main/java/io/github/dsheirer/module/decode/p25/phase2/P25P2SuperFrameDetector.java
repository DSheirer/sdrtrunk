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
package io.github.dsheirer.module.decode.p25.phase2;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ISCHSequence;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import io.github.dsheirer.module.decode.p25.phase2.message.SuperFrameFragment;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacOpcode;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.NetworkStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.NetworkStatusBroadcastImplicit;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.AbstractSignalingTimeslot;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.ScramblingSequence;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.Timeslot;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Listener;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * APCO25 Phase 2 super-frame fragment detector uses sync detection notifications and a circular dibit buffer to
 * correctly frame a (720-dibit / 1440-bit) super-frame fragment containing 4x timeslots and 4x ISCH fragments.  Sync
 * detections are provided by the symbol processor's soft sync detector (see syncDetected()) or by checking the dibit
 * buffer for the sync pattern (see processWithHardSyncDetect()) when processing a stream of dibits without soft
 * symbols.  The first ISCH fragment consisting of 20-bits from the preceding fragment and 20-bits from
 * the current fragment are reassembled into a contiguous ISCH chunk.  The layout for the super frame fragment is:
 *
 * I-ISCH1 + TIMESLOT1 + I-ISCH2 + TIMESLOT2 + S-SISCH1(sync1) + TIMESLOT3 + S-SISCH2(sync2) + TIMESLOT4
 */
public class P25P2SuperFrameDetector
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P2SuperFrameDetector.class);

    /**
     * Number of dibits that we use to oversize the fragment delay buffer where the total oversize is 2x this quantity
     * for padding the left and padding the right by this quantity.
     */
    private static final int FRAGMENT_BUFFER_OVERSIZE = 2; //Dibits

    /**
     * A super frame fragment is 720 dibits or 1440 bits long.
     */
    private static final int FRAGMENT_DIBIT_LENGTH = 720;

    /**
     * If we misalign by detecting the first sync pattern in the stream at the second sync detector, we can simply
     * shift the stream to the left to align both the sync 1 and sync 2 patterns with the detectors.  The sync patterns
     * are separated by one timeslot (160 dibits) and one ISCH (20 dibits).
     */
    private static final int DIBIT_COUNT_MISALIGNED_SYNC = FRAGMENT_DIBIT_LENGTH - 180;

    /**
     * Threshold for broadcasting a sync loss message every 3000 dibits (0.5 seconds at 6000 dibits per second) when not
     * synchronized.  Size is the sync loss broadcast interval (3000) plus the length of one super frame fragment (720).
     */
    private static final int BROADCAST_SYNC_LOSS_DIBIT_COUNT = 3720;

    /**
     * S-ISCH Sync pattern 1 is 2x timeslots (160 dibits ea.) plus 2x I-ISCH (20 dibits ea.) from the start of the fragment.
     */
    private static final int DIBIT_DELAY_BUFFER_INDEX_SYNC_1 = 360 + FRAGMENT_BUFFER_OVERSIZE;

    /**
     * S-ISCH Sync pattern 2 is 3x timeslots (160 dibits ea.) plus 3x I-ISCH (20 dibits ea.) from the start of the fragment.
     */
    private static final int DIBIT_DELAY_BUFFER_INDEX_SYNC_2 = 540 + FRAGMENT_BUFFER_OVERSIZE;

    /**
     * The maximum sync match error threshold when in a synchronized state is 7 in order to detect a stuffed (ie extra)
     * dibit or a lost dibit that causes the stream to shift left or right.  The hamming distance for the sync pattern
     * shifted left or right by 2 bit positions (ie 1 dibit) is 8 (left) or 9 (right).  So we set the threshold to
     * just below 8.
     */
    private static final int SYNCHRONIZED_SYNC_MATCH_THRESHOLD = 7;

    /**
     * Use a lower threshold for sync reacquisition.
     */
    private static final int UN_SYNCHRONIZED_SYNC_MATCH_THRESHOLD = 4;

    /**
     * Dibit count between the final dibit of a detected sync pattern and the fragment check, which aligns the detected
     * sync pattern with sync pattern 2 in the fragment buffer.  This is the 160 dibits between the end of the second
     * sync pattern and the end of the fragment plus the extra dibits for the oversized fragment buffer.
     */
    private static final int SYNC_DETECTION_DELAY = 160 + FRAGMENT_BUFFER_OVERSIZE;

    /**
     * Dibits per second (6000 baud) for calculating message timestamps.
     */
    private static final double DIBITS_PER_SECOND = 6000.0;

    /**
     * Maximum number of fragments (1.44 seconds) following a fragment with a valid I-ISCH that the ISCH sequence can be
     * predicted for.  Testing against Phase 2 recordings with injected noise: the predicted sequence matched the
     * decoded sequence for all 5,256 fragments with a valid I-ISCH, and increasing the maximum to 48 fragments only
     * added predictions at 6 dB in-band C/N.
     */
    private static final int MAX_PREDICTION_FRAGMENT_COUNT = 12;

    /**
     * Maximum offset (dibits) from a whole number of fragments since the anchor fragment for an ISCH sequence
     * prediction, which matches the dibit stuffing/deletion offset range that the fragment checks can correct.
     */
    private static final int MAX_PREDICTION_DIBIT_OFFSET = 2;

    private ScramblingSequence mScramblingSequence = new ScramblingSequence();
    private Listener<IMessage> mMessageListener;

    /**
     * The fragment buffer is sized to hold a complete super frame fragment (720 dibits) plus two extra preceding and
     * succeeding dibitsto support shifting the extracted super frame left or right 1/2 places when we detect 1 or 2
     * stuffed or deleted dibits in the sequence.
     */
    private DibitDelayBuffer mFragmentBuffer = new DibitDelayBuffer(720 + (2 * FRAGMENT_BUFFER_OVERSIZE));

    /**
     * Soft symbol buffer, parallel to the fragment buffer, for soft decision decoding.
     */
    private final float[] mSoftSymbolBuffer = new float[720 + (2 * FRAGMENT_BUFFER_OVERSIZE)];
    private int mSoftSymbolPointer = 0;
    private boolean mSoftSymbolsAvailable = false;
    private int mDibitsProcessed = 0;
    private long mDibitCounter = 0;
    private long mAnchorDibitCounter = -1;
    private ISCHSequence mAnchorIschSequence = ISCHSequence.UNKNOWN;
    private int mSyncDetectionCountdown = 0;
    private int mDibitsSinceTimestamp = 0;
    private long mReferenceTimestamp = 0;
    private boolean mSynchronized = false;
    private boolean mValidFragment = false;

    /**
     * Constructs an instance
     */
    public P25P2SuperFrameDetector()
    {
    }

    /**
     * Sets or updates the scrambling sequence parameters.
     * @param scramblingSequence containing updated parameters
     */
    public void setScrambleParameters(ScrambleParameters scramblingSequence)
    {
        mScramblingSequence.update(scramblingSequence);
    }

    public void setListener(Listener<IMessage> listener)
    {
        mMessageListener = listener;
    }

    /**
     * Sets or updates the current dibit stream time from an incoming sample buffer.
     * @param timestamp to use as a reference timestamp.
     */
    public void setTimestamp(long timestamp)
    {
        mReferenceTimestamp = timestamp;
        mDibitsSinceTimestamp = 0;
    }

    /**
     * Calculates the timestamp accurate to the currently received dibit.
     * @return timestamp in milliseconds.
     */
    private long getCurrentTimestamp()
    {
        if(mReferenceTimestamp > 0)
        {
            return mReferenceTimestamp + (long)(1000.0 * mDibitsSinceTimestamp / DIBITS_PER_SECOND);
        }

        return System.currentTimeMillis();
    }

    /**
     * Notification that a sync pattern was detected where the final sync dibit is the most recently processed dibit.
     * When not synchronized, schedules a fragment check once the detected sync pattern aligns with the sync pattern 2
     * location in the fragment buffer.  Ignored when synchronized since the fragment is checked at each fragment
     * interval.
     */
    public void syncDetected()
    {
        if(!mSynchronized && mSyncDetectionCountdown == 0)
        {
            mSyncDetectionCountdown = SYNC_DETECTION_DELAY;
        }
    }

    /**
     * Processes the dibit and detects sync patterns from the dibits in the fragment buffer, for use when processing a
     * stream of dibits without access to the soft symbol sync detection (see syncDetected()).
     * @param dibit to process
     * @return true if a valid super frame fragment was produced.
     */
    public boolean processWithHardSyncDetect(Dibit dibit)
    {
        boolean valid = process(dibit);

        if(!mSynchronized && mSyncDetectionCountdown == 0)
        {
            int bitErrorCount = getSyncBitErrorCount(DIBIT_DELAY_BUFFER_INDEX_SYNC_2);

            if(bitErrorCount <= UN_SYNCHRONIZED_SYNC_MATCH_THRESHOLD)
            {
                mValidFragment = false;
                checkFragmentSync(bitErrorCount);
                valid |= mValidFragment;
            }
        }

        return valid;
    }

    /**
     * Processes the dibit and the soft symbol it was decided from.
     * @param dibit to process
     * @param softSymbol for the dibit (radians)
     * @return true if a valid super frame fragment (ie contains at least one valid MAC message) was produced.
     */
    public boolean process(Dibit dibit, float softSymbol)
    {
        mSoftSymbolBuffer[mSoftSymbolPointer++] = softSymbol;
        mSoftSymbolPointer %= mSoftSymbolBuffer.length;
        mSoftSymbolsAvailable = true;
        return process(dibit);
    }

    /**
     * Extracts soft symbols from the soft symbol buffer, using the same indexing as the fragment buffer.
     * @param start index relative to the oldest soft symbol in the buffer
     * @param length number of soft symbols
     * @return soft symbols or null if soft symbols are not available.
     */
    private float[] getSoftSymbols(int start, int length)
    {
        if(!mSoftSymbolsAvailable)
        {
            return null;
        }

        float[] softSymbols = new float[length];
        int pointer = (mSoftSymbolPointer + start) % mSoftSymbolBuffer.length;

        for(int x = 0; x < length; x++)
        {
            softSymbols[x] = mSoftSymbolBuffer[pointer++];
            pointer %= mSoftSymbolBuffer.length;
        }

        return softSymbols;
    }

    /**
     * Processes the dibit.
     * @param dibit to process
     * @return true if a valid super frame fragment (ie contains at least one valid MAC message) was produced.
     */
    public boolean process(Dibit dibit)
    {
        mValidFragment = false;
        mDibitsProcessed++;
        mDibitsSinceTimestamp++;
        mDibitCounter++;

        mFragmentBuffer.put(dibit);

        if(mSynchronized)
        {
            mSyncDetectionCountdown = 0;

            if(mDibitsProcessed >= FRAGMENT_DIBIT_LENGTH)
            {
                checkFragmentSync(0);
            }
        }
        else if(mSyncDetectionCountdown > 0)
        {
            mSyncDetectionCountdown--;

            if(mSyncDetectionCountdown == 0)
            {
                checkFragmentSync(getSyncBitErrorCount(DIBIT_DELAY_BUFFER_INDEX_SYNC_2));
            }
        }

        //Broadcast sync loss message every 3000 dibits (6000 bits) when we're not synchronized
        if(mDibitsProcessed > BROADCAST_SYNC_LOSS_DIBIT_COUNT)
        {
            mDibitsProcessed -= 3000;
            broadcastSyncLoss(3000);
        }

        return mValidFragment;
    }

    /**
     * Creates a super-frame fragment from the current contents of the fragment dibit buffer and broadcasts it to
     * a registered listener.
     *
     * @param bitErrors detected for first and second ISCH-S segments combined
     * @param dibitOffset to shift the extraction left (-) or right (+) by one or two dibits when dibit stuffing or
     * deletion are detected.
     */
    private void broadcastFragment(int bitErrors, int dibitOffset)
    {
        if((mDibitsProcessed + dibitOffset) > FRAGMENT_DIBIT_LENGTH)
        {
            broadcastSyncLoss(mDibitsProcessed + dibitOffset - FRAGMENT_DIBIT_LENGTH);
        }

        mDibitsProcessed = 0 + dibitOffset;
        CorrectedBinaryMessage message = mFragmentBuffer.getMessage(FRAGMENT_BUFFER_OVERSIZE + dibitOffset, 720);
        message.setCorrectedBitCount(bitErrors);
        float[] softSymbols = getSoftSymbols(FRAGMENT_BUFFER_OVERSIZE + dibitOffset, 720);
        SuperFrameFragment frameFragment = new SuperFrameFragment(message, softSymbols, getCurrentTimestamp(),
                mScramblingSequence);
        updateIschSequence(frameFragment, mDibitCounter - FRAGMENT_BUFFER_OVERSIZE + dibitOffset);
        updateScramblingCode(frameFragment);
        mValidFragment = hasValidMacMessage(frameFragment);
        broadcast(frameFragment);
    }

    /**
     * Creates a super-frame fragment from the current contents of the fragment dibit buffer and broadcasts it to
     * a registered listener.  This method supports the use case when sync pattern 1 can be aligned by a small offset
     * and sync pattern 2 can also be aligned by a small offset, however there is also an offset between sync 1 and
     * sync 2 within the current stream, so we have to 'Frankenstein' together a message from the two fragments where
     * the first half is aligned to sync 1 and the second half is aligned to sync 2.  This possibly happens when the
     * dibit stuffing or deletion occurs between the two sync patterns in the stream and will likely mean that timeslot
     * 3 is corrupted, but we're (potentially) salvaging timeslots 1, 2 and 4 of the fragment.
     *
     * @param bitErrors detected for first and second ISCH-S segments combined
     * @param sync1Offset to align the first part of the message with sync pattern 1.
     * @param sync2Offset to align the second part of the message with sync patter 2.
     */
    private void broadcastSplitFragment(int bitErrors, int sync1Offset, int sync2Offset)
    {
        if((mDibitsProcessed) > FRAGMENT_DIBIT_LENGTH)
        {
            broadcastSyncLoss(mDibitsProcessed + FRAGMENT_DIBIT_LENGTH);
        }

        mDibitsProcessed = 0 + sync2Offset; //We're only concerned with adjusting for sync 2 offset from here on out.
        CorrectedBinaryMessage message1 = mFragmentBuffer.getMessage(FRAGMENT_BUFFER_OVERSIZE + sync1Offset, 720);
        //Clear the bits from sync 2 start bit index 1080 (dibit 540) inclusive through bit index 1440 (exclusive).
        message1.clear(1080, 1440);
        CorrectedBinaryMessage message2 = mFragmentBuffer.getMessage(FRAGMENT_BUFFER_OVERSIZE + sync2Offset, 720);
        //Clear the bits from bit index 0 (inclusive) through bit index 1080 (dibit 540) exclusive
        message2.clear(0, 1080);
        //Xor the two messages to produce a unified super frame fragment
        message1.xor(message2);
        message1.setCorrectedBitCount(bitErrors); //Not even sure what the correct bit error count is here?
        //Soft symbols are similarly aligned to sync 1 before dibit 540 and to sync 2 from dibit 540 onward
        float[] softSymbols = getSoftSymbols(FRAGMENT_BUFFER_OVERSIZE + sync1Offset, 720);
        float[] softSymbols2 = getSoftSymbols(FRAGMENT_BUFFER_OVERSIZE + sync2Offset, 720);

        if(softSymbols != null)
        {
            System.arraycopy(softSymbols2, 540, softSymbols, 540, 180);
        }

        SuperFrameFragment frameFragment = new SuperFrameFragment(message1, softSymbols, getCurrentTimestamp(),
                mScramblingSequence);
        updateIschSequence(frameFragment, mDibitCounter - FRAGMENT_BUFFER_OVERSIZE + sync2Offset);
        updateScramblingCode(frameFragment);
        mValidFragment = hasValidMacMessage(frameFragment);
        broadcast(frameFragment);
    }

    /**
     * Tracks the ISCH sequence (ie fragment 1, 2 or 3 of the superframe) of fragments with a valid I-ISCH as an anchor
     * and predicts the ISCH sequence for fragments where neither I-ISCH is valid, from the number of whole fragments
     * since the anchor fragment.  The ISCH sequence determines the scrambling sequence offset and the order of the
     * final two timeslots in the fragment.
     * @param fragment to update
     * @param fragmentEnd dibit counter value for the final dibit of the fragment
     */
    private void updateIschSequence(SuperFrameFragment fragment, long fragmentEnd)
    {
        if(fragment.hasDecodedIschSequence())
        {
            mAnchorDibitCounter = fragmentEnd;
            mAnchorIschSequence = fragment.getIschSequence();
        }
        else if(mAnchorDibitCounter >= 0)
        {
            long elapsed = fragmentEnd - mAnchorDibitCounter;
            long fragments = Math.round(elapsed / (double)FRAGMENT_DIBIT_LENGTH);

            if(fragments >= 1 && fragments <= MAX_PREDICTION_FRAGMENT_COUNT &&
                Math.abs(elapsed - fragments * FRAGMENT_DIBIT_LENGTH) <= MAX_PREDICTION_DIBIT_OFFSET)
            {
                fragment.setPredictedIschSequence(mAnchorIschSequence.advance((int)fragments));
            }
        }
    }

    /**
     * Updates the scrambling code when we receive a network status broadcast with WACN, SYSTEM and NAC.
     * @param superFrameFragment
     */
    private void updateScramblingCode(SuperFrameFragment superFrameFragment)
    {
        boolean updated = false;

        for(Timeslot timeslot: superFrameFragment.getTimeslots())
        {
            if(timeslot instanceof AbstractSignalingTimeslot abstractSignalingTimeslot)
            {
                List<MacMessage> macMessages = abstractSignalingTimeslot.getMacMessages();

                for(MacMessage macMessage: macMessages)
                {
                    if(macMessage.isValid())
                    {
                        MacOpcode macOpcode = macMessage.getMacStructure().getOpcode();

                        if(macOpcode == MacOpcode.PHASE1_7B_NETWORK_STATUS_BROADCAST_IMPLICIT &&
                                macMessage.getMacStructure() instanceof NetworkStatusBroadcastImplicit nsba)
                        {
                            updated |= mScramblingSequence.update(nsba.getScrambleParameters());
                        }
                        else if(macOpcode == MacOpcode.PHASE1_FB_NETWORK_STATUS_BROADCAST_EXPLICIT &&
                                macMessage.getMacStructure() instanceof NetworkStatusBroadcastExplicit nsbe)
                        {
                            updated |= mScramblingSequence.update(nsbe.getScrambleParameters());
                        }
                    }
                }
            }
        }

        //If we updated the scramble sequence, nullify the timeslots so they can be recreated descrambled.
        if(updated)
        {
            superFrameFragment.resetTimeslots();
        }
    }

    /**
     * Indicates if the super frame fragment contains at least one valid (ie CRC check passes) MAC message.
     * @param superFrameFragment to inspect
     * @return true if valid.
     */
    private static boolean hasValidMacMessage(SuperFrameFragment superFrameFragment)
    {
        for(Timeslot timeslot: superFrameFragment.getTimeslots())
        {
            if(timeslot instanceof AbstractSignalingTimeslot abstractSignalingTimeslot)
            {
                for(MacMessage macMessage: abstractSignalingTimeslot.getMacMessages())
                {
                    if(macMessage.isValid())
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void broadcastSyncLoss(int dibitsProcessed)
    {
        broadcast(new SyncLossMessage(getCurrentTimestamp(), dibitsProcessed * 2, Protocol.APCO25_PHASE2));
    }

    private void broadcast(IMessage message)
    {
        if(mMessageListener != null)
        {
            mMessageListener.receive(message);
        }
    }

    /**
     * Checks the current buffered fragment to detect sync pattern 1 and sync pattern 2 and broadcast the fragment.
     * @param syncDetectorBitErrorCount number of bit errors in the detected sync pattern.
     */
    private void checkFragmentSync(int syncDetectorBitErrorCount)
    {
        //Since we're using multi-sync detection, only proceed if we've processed enough dibits.  The first sync detector
        //to fire will cause the fragment to be processed and any subsequent, simultaneous detection will be ignored.
        if(mDibitsProcessed > 0)
        {
            if(mSynchronized)
            {
                //If we're synchronized, then this is a counter based trigger and we check both sync locations
                int sync1BitErrorCount = getSyncBitErrorCount(DIBIT_DELAY_BUFFER_INDEX_SYNC_1);

                if(sync1BitErrorCount <= SYNCHRONIZED_SYNC_MATCH_THRESHOLD)
                {
                    int sync2BitErrorCount = getSyncBitErrorCount(DIBIT_DELAY_BUFFER_INDEX_SYNC_2);

                    if(sync2BitErrorCount <= SYNCHRONIZED_SYNC_MATCH_THRESHOLD)
                    {
                        broadcastFragment(sync1BitErrorCount + sync2BitErrorCount, 0);
                        return;
                    }
                    else
                    {
                        //We are synchronized on sync 1 but not on sync 2.  Check to see if we have a good sync 2
                        //pattern by shifting left or right by one or two dibits to detect dibit stuffing/deletion
                        // between sync 1 and sync 2.
                        int sync2Offset = getSynchronizedSyncOffset(DIBIT_DELAY_BUFFER_INDEX_SYNC_2);

                        if(isValidSyncOffset(sync2Offset))
                        {
                            //Recalculate the sync 2 bit error count using the offset value.
                            sync2BitErrorCount = getSyncBitErrorCount(DIBIT_DELAY_BUFFER_INDEX_SYNC_2 + sync2Offset);
                            broadcastSplitFragment(sync1BitErrorCount + sync2BitErrorCount, 0, sync2Offset);
                        }

                        //Since we're getting misaligned, set unsynchronized to re-enter active sync inspection
                        mSynchronized = false;
                        return;
                    }
                }
                else
                {
                    //We are synchronized but we've lost sync on current sync 1.  Check to see if we have a good sync
                    //pattern by shifting left or right by one or two dibits to detect dibit stuffing/deletion.
                    int sync1Offset = getSynchronizedSyncOffset(DIBIT_DELAY_BUFFER_INDEX_SYNC_1);

                    if(isValidSyncOffset(sync1Offset))
                    {
                        //Update the sync 1 bit error count, calculated using the new offset.
                        sync1BitErrorCount = getSyncBitErrorCount(DIBIT_DELAY_BUFFER_INDEX_SYNC_1 + sync1Offset);
                        int sync2BitErrorCount = getSyncBitErrorCount(DIBIT_DELAY_BUFFER_INDEX_SYNC_2 + sync1Offset);

                        if(sync2BitErrorCount <= SYNCHRONIZED_SYNC_MATCH_THRESHOLD)
                        {
                            //Broadcast the fragment using just the sync 1 offset.
                            broadcastFragment(sync1BitErrorCount + sync2BitErrorCount, sync1Offset);
                        }
                        else
                        {
                            //Check to see if there's a different offset for sync 2, relative to the new sync 1 offset
                            int sync2Offset = getSynchronizedSyncOffset(DIBIT_DELAY_BUFFER_INDEX_SYNC_2 + sync1Offset);

                            if(isValidSyncOffset(sync2Offset))
                            {
                                int totalOffset = sync1Offset + sync2Offset;

                                //Don't allow the total (sync 1 + sync2) offset to exceed the range (-2 to +2)
                                if(isValidSyncOffset(totalOffset))
                                {
                                    broadcastSplitFragment(sync1BitErrorCount + sync2BitErrorCount, sync1Offset, sync2Offset);
                                }
                            }
                        }
                    }

                    //Since we're getting misaligned, set unsynchronized to re-enter active sync inspection
                    mSynchronized = false;
                    return;
                }
            }

            //If we're not synchronized, this is a sync detector trigger and we only have to check sync 1 for error
            // count because the sync detector has already triggered on sync 2
            int sync1BitErrorCount = getSyncBitErrorCount(DIBIT_DELAY_BUFFER_INDEX_SYNC_1);

            if(sync1BitErrorCount <= UN_SYNCHRONIZED_SYNC_MATCH_THRESHOLD)
            {
                mSynchronized = true;
                broadcastFragment(sync1BitErrorCount + syncDetectorBitErrorCount, 0);
            }
            else
            {
                //We're probably mis-aligned on the fragment.  Setup as if we're synchronized and adjust the dibits
                // processed counter so that we guarantee a fragment check after another 180 dibits have arrived which
                // means that sync1 and sync2 should be aligned
                mSynchronized = true;

                if(mDibitsProcessed > DIBIT_COUNT_MISALIGNED_SYNC)
                {
                    broadcastSyncLoss(mDibitsProcessed - DIBIT_COUNT_MISALIGNED_SYNC);
                }

                mDibitsProcessed = DIBIT_COUNT_MISALIGNED_SYNC;
            }
        }
    }

    /**
     * Indicates if the sync offset returned from the getSyncOffset() method is valid.
     * @param offset to test
     * @return true if the offset falls in the range: (-2 to 2)
     */
    private boolean isValidSyncOffset(int offset)
    {
        return -2 <= offset && offset <= 2;
    }

    /**
     * Attempts to identify a positive or negative dibit offset value that would allow a valid sync pattern match
     * and stream alignment.  Looks to the left and right of the specified index by 1 or 2 dibits of offset to
     * determine if the dibit sequence at that location has a bit error count lower than the synchronized threshold
     * and reduces that threshold accordingly as we shift left/right by 1/2 dibit places.
     * @param index for a normal sync detection.
     * @return offset in the range of (-2 to +2) or Integer.MIN_VALUE if none of the tested offsets would produce sync.
     */
    private int getSynchronizedSyncOffset(int index)
    {
        if(getSyncBitErrorCount(index - 1) <= SYNCHRONIZED_SYNC_MATCH_THRESHOLD - 1)
        {
            return -1;
        }
        else if(getSyncBitErrorCount(index + 1) <= SYNCHRONIZED_SYNC_MATCH_THRESHOLD - 1)
        {
            return 1;
        }
        else if(getSyncBitErrorCount(index - 2) <= SYNCHRONIZED_SYNC_MATCH_THRESHOLD - 2)
        {
            return -2;
        }
        else if(getSyncBitErrorCount(index + 2) <= SYNCHRONIZED_SYNC_MATCH_THRESHOLD - 2)
        {
            return 2;
        }

        return Integer.MIN_VALUE;
    }

    /**
     * Calculates the bit error count for the sequence of 20-dibits starting at the specified index against the
     * normal P25 Phase 2 sync pattern.
     * @param index for the start of the 20-dibit sequence.
     * @return bit error count (ie hamming distance) of the 20-dibit sequence compared to the sync pattern.
     */
    private int getSyncBitErrorCount(int index)
    {
        Dibit[] dibits = mFragmentBuffer.getBuffer(index, 20);
        return P25P2SyncPattern.getBitErrorCount(dibits);
    }
}
