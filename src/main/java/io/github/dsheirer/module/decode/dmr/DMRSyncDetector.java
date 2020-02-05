/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.bits.DMRSoftSyncDetector;
import io.github.dsheirer.bits.MultiSyncPatternMatcher;
import io.github.dsheirer.bits.SoftSyncDetector;
import io.github.dsheirer.bits.SyncDetector;
import io.github.dsheirer.dsp.psk.pll.IPhaseLockedLoop;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.FrameSync;
import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.P25P1SyncDetector;
import io.github.dsheirer.sample.Listener;

public class DMRSyncDetector implements Listener<Dibit>
{
    /* Determines the threshold for sync pattern soft matching */
    private static final int SYNC_MATCH_THRESHOLD = 4;

    public static final double DEFAULT_SAMPLE_RATE = 50000.0;

    public static final double FREQUENCY_PHASE_CORRECTION_90_DEGREES = 1296.0;
    public static final double FREQUENCY_PHASE_CORRECTION_180_DEGREES = 2592.0;

    private MultiSyncPatternMatcher mMatcher;
    private DMRSoftSyncDetector mBSDSyncDetector, mBSVSyncDetector, mMSDSyncDetector, mMSVSyncDetector;
    private DMRSoftSyncDetector mTDMATS1D;

    private PLLPhaseInversionDetector mInversionDetector90CW;
    private PLLPhaseInversionDetector mInversionDetector90CCW;
    private PLLPhaseInversionDetector mInversionDetector180;

    public DMRSyncDetector(IDMRSyncDetectListener syncDetectListener, IPhaseLockedLoop phaseLockedLoop)
    {
        //TODO: since we're only going to feed dibits to find next frame, it makes sense to
        //TODO: update the sync lost parameter to 48 bits ....

        //TODO: only enable the phase inversion detectors when we're in a sync-lost state
        mMatcher = new MultiSyncPatternMatcher(syncDetectListener, P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_1.getMessageLength(), 48);
        mBSDSyncDetector = new DMRSoftSyncDetector(DMRSyncPattern.BASE_STATION_DATA,SYNC_MATCH_THRESHOLD, syncDetectListener);
        mMSDSyncDetector = new DMRSoftSyncDetector(DMRSyncPattern.MOBILE_STATION_DATA,SYNC_MATCH_THRESHOLD, syncDetectListener);
        mBSVSyncDetector = new DMRSoftSyncDetector(DMRSyncPattern.MOBILE_STATION_VOICE,SYNC_MATCH_THRESHOLD, syncDetectListener);
        mMSVSyncDetector = new DMRSoftSyncDetector(DMRSyncPattern.BASE_STATION_VOICE,SYNC_MATCH_THRESHOLD, syncDetectListener);
        mTDMATS1D = new DMRSoftSyncDetector(DMRSyncPattern.MOBILE_STATION_REVERSE_CHANNEL,SYNC_MATCH_THRESHOLD, syncDetectListener);

/*
        mInversionDetector90CW = new PLLPhaseInversionDetector(DMRSyncPattern.P25_PHASE1_ERROR_90_CW,
                phaseLockedLoop, DEFAULT_SAMPLE_RATE, FREQUENCY_PHASE_CORRECTION_90_DEGREES);
        mMatcher.add(mInversionDetector90CW);

        mInversionDetector90CCW = new PLLPhaseInversionDetector(FrameSync.P25_PHASE1_ERROR_90_CCW,
                phaseLockedLoop, DEFAULT_SAMPLE_RATE, -FREQUENCY_PHASE_CORRECTION_90_DEGREES);
        mMatcher.add(mInversionDetector90CCW);

        mInversionDetector180 = new PLLPhaseInversionDetector(FrameSync.P25_PHASE1_ERROR_180,
                phaseLockedLoop, DEFAULT_SAMPLE_RATE, FREQUENCY_PHASE_CORRECTION_180_DEGREES);
        mMatcher.add(mInversionDetector180);
*/

        mMatcher.add(mBSDSyncDetector);
        mMatcher.add(mBSVSyncDetector);
        mMatcher.add(mMSVSyncDetector);
        mMatcher.add(mMSDSyncDetector);
        mMatcher.add(mTDMATS1D);

        if(phaseLockedLoop != null)
        {

        }
    }

    /**
     * Calculates the number of bits that match in the current primary detector
     * @return
     */
    public int getPrimarySyncMatchErrorCount()
    {
        return 0; //Long.bitCount(mMatcher.getCurrentValue() ^ FrameSync.DMR_DIRECT_MODE_DATA_TIMESLOT_1.getSync());
    }

    @Override
    public void receive(Dibit dibit)
    {
        mMatcher.receive(dibit.getBit1(), dibit.getBit2());
    }
    public class PLLPhaseInversionDetector extends SyncDetector
    {
        private FrameSync mFrameSync;
        private IPhaseLockedLoop mPhaseLockedLoop;
        private double mSampleRate;
        private double mFrequencyCorrection;
        private double mPllCorrection;

        /**
         * Constructs the PLL phase inversion detector.
         *
         * @param frameSync pattern to monitor for detecting phase inversion errors
         * @param phaseLockedLoop to receive phase correction values
         * @param sampleRate of the incoming sample stream
         * @param frequencyCorrection to apply to the PLL.  Examples:
         *      QPSK +/-90 degree correction: +/-SYMBOL RATE / 4.0
         *      QPSK 180 degree correction: SYMBOL RATE / 2.0
         */
        public PLLPhaseInversionDetector(FrameSync frameSync, IPhaseLockedLoop phaseLockedLoop, double sampleRate,
                                         double frequencyCorrection)
        {
            super(frameSync.getSync());
            mFrameSync = frameSync;
            mPhaseLockedLoop = phaseLockedLoop;
            mFrequencyCorrection = frequencyCorrection;
            setSampleRate(sampleRate);

            setListener(new ISyncDetectListener()
            {
                @Override
                public void syncDetected(int bitErrors)
                {
                    mPhaseLockedLoop.correctInversion(mPllCorrection);
                }

                @Override
                public void syncLost(int bitProcessed)
                {
                    //no-op
                }
            });
        }

        /**
         * Sets or adjusts the sample rate so that the phase inversion correction value can be recalculated.
         * @param sampleRate
         */
        public void setSampleRate(double sampleRate)
        {
            mSampleRate = sampleRate;
            mPllCorrection = 2.0 * Math.PI * mFrequencyCorrection / mSampleRate;
        }
    }

}
