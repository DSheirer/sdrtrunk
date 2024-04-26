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
package io.github.dsheirer.module.decode.p25.phase2;

import io.github.dsheirer.bits.MultiSyncPatternMatcher;
import io.github.dsheirer.bits.SoftSyncDetector;
import io.github.dsheirer.bits.SyncDetector;
import io.github.dsheirer.dsp.psk.pll.IPhaseLockedLoop;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.FrameSync;
import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
import io.github.dsheirer.sample.Listener;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P25P2SyncDetector implements Listener<Dibit>
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P2SyncDetector.class);

    /* Determines the threshold for sync pattern soft matching */
    private static final int SYNC_MATCH_THRESHOLD = 4;

    /* Costas Loop phase lock error correction values.  A phase lock error of
     * 90 degrees requires a correction of 1/4 of the symbol rate (1500Hz).  An
     * error of 180 degrees requires a correction of 1/2 of the symbol rate */
    public static final double DEFAULT_SAMPLE_RATE = 50000.0;
    public static final double DEFAULT_SYMBOL_RATE = 6000;

    public static final double FREQUENCY_PHASE_CORRECTION_90_DEGREES = DEFAULT_SYMBOL_RATE / 4.0;
    public static final double FREQUENCY_PHASE_CORRECTION_180_DEGREES = DEFAULT_SYMBOL_RATE / 2.0;

    private MultiSyncPatternMatcher mMatcher;
    private SoftSyncDetector mPrimarySyncDetector;

    private PLLPhaseInversionDetector mInversionDetector90CW;
    private PLLPhaseInversionDetector mInversionDetector90CCW;
    private PLLPhaseInversionDetector mInversionDetector180;

    public P25P2SyncDetector(ISyncDetectListener syncDetectListener, IPhaseLockedLoop phaseLockedLoop)
    {
        //TODO: since we're only going to feed dibits to find next frame, it makes sense to
        //TODO: update the sync lost parameter to 48 bits ....

        //TODO: only enable the phase inversion detectors when we're in a sync-lost state
        mMatcher = new MultiSyncPatternMatcher(syncDetectListener, 1440, 40);
        mPrimarySyncDetector = new SoftSyncDetector(FrameSync.P25_PHASE2_NORMAL.getSync(), SYNC_MATCH_THRESHOLD, syncDetectListener);
        mMatcher.add(mPrimarySyncDetector);

        if(phaseLockedLoop != null)
        {
//            //Add additional sync pattern detectors to detect when we get 90/180 degree out of phase sync pattern
//            //detections so that we can apply correction to the phase locked loop
//            mInversionDetector90CW = new PLLPhaseInversionDetector(FrameSync.P25_PHASE2_ERROR_90_CW,
//                phaseLockedLoop, DEFAULT_SAMPLE_RATE, FREQUENCY_PHASE_CORRECTION_90_DEGREES);
//            mMatcher.add(mInversionDetector90CW);
//
//            mInversionDetector90CCW = new PLLPhaseInversionDetector(FrameSync.P25_PHASE2_ERROR_90_CCW,
//                phaseLockedLoop, DEFAULT_SAMPLE_RATE, -FREQUENCY_PHASE_CORRECTION_90_DEGREES);
//            mMatcher.add(mInversionDetector90CCW);
//
//            mInversionDetector180 = new PLLPhaseInversionDetector(FrameSync.P25_PHASE2_ERROR_180,
//                phaseLockedLoop, DEFAULT_SAMPLE_RATE, FREQUENCY_PHASE_CORRECTION_180_DEGREES);
//            mMatcher.add(mInversionDetector180);
        }
    }

    /**
     * Calculates the number of bits that match in the current primary detector
     * @return
     */
    public int getPrimarySyncMatchErrorCount()
    {
        return Long.bitCount(mMatcher.getCurrentValue() ^ FrameSync.P25_PHASE2_NORMAL.getSync());
    }

    @Override
    public void receive(Dibit dibit)
    {
        mMatcher.receive(dibit.getBit1(), dibit.getBit2());
    }

    /**
     * Updates the incoming sample stream sample rate to allow the PLL phase inversion detectors to
     * recalculate their internal phase correction values.
     *
     * @param sampleRate of the incoming sample stream
     */
    public void setSampleRate(double sampleRate)
    {
//        mInversionDetector180.setSampleRate(sampleRate);
//        mInversionDetector90CW.setSampleRate(sampleRate);
//        mInversionDetector90CCW.setSampleRate(sampleRate);
    }

    /**
     * Sync pattern detector to listen for costas loop phase lock errors and apply a phase correction to the costas
     * loop so that we don't miss any messages.
     *
     * When the costas loop locks with a +/- 90 degree or 180 degree phase error, the slicer will incorrectly apply
     * the symbol pattern rotated left or right by the phase error.  However, we can detect these rotated sync patterns
     * and apply immediate phase correction so that message processing can continue.
     */
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
                public void syncLost(int bitsProcessed)
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
            mPllCorrection = 2.0 * FastMath.PI * mFrequencyCorrection / mSampleRate;
        }
    }
}
