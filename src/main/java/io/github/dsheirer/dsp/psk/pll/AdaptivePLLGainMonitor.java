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
package io.github.dsheirer.dsp.psk.pll;

import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
import io.github.dsheirer.module.decode.dmr.DMRDecoder;
import io.github.dsheirer.module.decode.p25.phase1.P25P1Decoder;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors decode sync events to adaptively control PLL loop gain.
 */
public class AdaptivePLLGainMonitor implements ISyncDetectListener, IFrequencyErrorProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(AdaptivePLLGainMonitor.class);

    private static final int MAX_SYNC_COUNT = 6;
    private CostasLoop mCostasLoop;
    private P25P1Decoder mP25P1Decoder;
    private DMRDecoder mDMRDecoder;
    private PLLGain mPLLGain = PLLGain.LEVEL_1;
    private int mSyncCount;

    /**
     * Constructs an adaptive monitor to monitor the sync state of a decoder and adjust the gain
     * level of the costas loop accordingly.
     *
     * @param costasLoop to receive adaptive gain updates.
     * @param p25P1Decoder to receive frequency error source events
     */
    public AdaptivePLLGainMonitor(CostasLoop costasLoop, P25P1Decoder p25P1Decoder)
    {
        mCostasLoop = costasLoop;
        mCostasLoop.setPLLGain(mPLLGain);
        mCostasLoop.setFrequencyErrorProcessor(this);
        mP25P1Decoder = p25P1Decoder;
    }
    public AdaptivePLLGainMonitor(CostasLoop costasLoop, DMRDecoder p25P1Decoder)
    {
        mCostasLoop = costasLoop;
        mCostasLoop.setPLLGain(mPLLGain);
        mCostasLoop.setFrequencyErrorProcessor(this);
        mDMRDecoder = p25P1Decoder;
    }
    /**
     * Sync detection event.  Updates the running sync count and updates the PLL gain level.
     */
    @Override
    public void syncDetected(int bitErrors)
    {
        mSyncCount++;
        update();
    }

    /**
     * Sync loss event.  Updates the running sync count and updates the PLL gain level.
     */
    @Override
    public void syncLost()
    {
        mSyncCount -= 2;
        update();
    }

    /**
     * Updates the gain state for a sync detect or sync loss
     */
    private void update()
    {
        if(mSyncCount < 0)
        {
            mSyncCount = 0;
            return;
        }

        if(mSyncCount > MAX_SYNC_COUNT)
        {
            mSyncCount = MAX_SYNC_COUNT;
            return;
        }

        PLLGain gain = PLLGain.fromSyncCount(mSyncCount);

        //Only update the costas loop if the gain value changes
        if(gain != mPLLGain)
        {
            mPLLGain = gain;
            mCostasLoop.setPLLGain(mPLLGain);
        }
    }

    /**
     * Resets the monitor and commands the PLL to use the highest gain level.
     */
    public void reset()
    {
        mSyncCount = 0;
        mPLLGain = PLLGain.LEVEL_1;
        mCostasLoop.setPLLGain(mPLLGain);
    }

    @Override
    public void processFrequencyError(long frequencyError)
    {
        //Only rebroadcast the costas loop frequency error if the gain level has progressed above the
        //default gain of 1, meaning that we have received at least 2 sync events
        if(mPLLGain != PLLGain.LEVEL_1)
        {
            if(mP25P1Decoder!=null) { mP25P1Decoder.broadcast(SourceEvent.frequencyErrorMeasurement(frequencyError)); }
            else {
                mDMRDecoder.broadcast(SourceEvent.frequencyErrorMeasurement(frequencyError)); }
        }
    }
}
