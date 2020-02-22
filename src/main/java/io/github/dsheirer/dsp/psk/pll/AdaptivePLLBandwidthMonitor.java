/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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
package io.github.dsheirer.dsp.psk.pll;

import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors decode sync events to adaptively control PLL loop gain.
 */
public class AdaptivePLLBandwidthMonitor implements ISyncDetectListener, IFrequencyErrorProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(AdaptivePLLBandwidthMonitor.class);

    private static final int MAX_SYNC_COUNT = PLLBandwidth.BW_200.getRangeEnd();
    private CostasLoop mCostasLoop;
    private FeedbackDecoder mFeedbackDecoder;
    private PLLBandwidth mPLLBandwidth = PLLBandwidth.BW_400;
    private int mSyncCount;

    /**
     * Constructs an adaptive monitor to monitor the sync state of a decoder and adjust the gain
     * level of the costas loop accordingly.
     *
     * @param costasLoop to receive adaptive gain updates.
     * @param feedbackDecoder to rebroadcast frequency error source events
     */
    public AdaptivePLLBandwidthMonitor(CostasLoop costasLoop, FeedbackDecoder feedbackDecoder)
    {
        mCostasLoop = costasLoop;
        mCostasLoop.setPLLBandwidth(mPLLBandwidth);
        mCostasLoop.setFrequencyErrorProcessor(this);
        mFeedbackDecoder = feedbackDecoder;
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
    public void syncLost(int bitsProcessed)
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

        PLLBandwidth gain = PLLBandwidth.fromSyncCount(mSyncCount);

        //Only update the costas loop if the gain value changes
        if(gain != mPLLBandwidth)
        {
            mPLLBandwidth = gain;
            mCostasLoop.setPLLBandwidth(mPLLBandwidth);
        }
    }

    /**
     * Resets the monitor and commands the PLL to use the highest gain level.
     */
    public void reset()
    {
        mSyncCount = 0;
        mPLLBandwidth = PLLBandwidth.BW_400;
        mCostasLoop.setPLLBandwidth(mPLLBandwidth);
    }

    @Override
    public void processFrequencyError(long frequencyError)
    {
        //Only rebroadcast the costas loop frequency error if the gain level has progressed above
        //a bandwidth of 300, meaning that we have received at least 4 sync events
        if(mPLLBandwidth != PLLBandwidth.BW_400 && mPLLBandwidth != PLLBandwidth.BW_300)
        {
            mFeedbackDecoder.broadcast(SourceEvent.frequencyErrorMeasurement(frequencyError));
        }
    }
}
