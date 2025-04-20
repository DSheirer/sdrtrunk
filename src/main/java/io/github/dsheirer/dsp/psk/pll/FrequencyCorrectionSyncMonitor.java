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
package io.github.dsheirer.dsp.psk.pll;

import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors decode sync events to adaptively control frequency correction broadcasts.
 */
public class FrequencyCorrectionSyncMonitor implements ISyncDetectListener, IFrequencyErrorProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(FrequencyCorrectionSyncMonitor.class);

    private static final int MAX_SYNC_COUNT = PLLBandwidth.BW_200.getRangeEnd();
    private FeedbackDecoder mFeedbackDecoder;
    private CostasLoop mCostasLoop;
    private int mSyncCount;

    /**
     * Constructs an adaptive monitor to monitor the sync state of a decoder
     *
     * @param costasLoop to receive adaptive gain updates.
     * @param feedbackDecoder to rebroadcast frequency error source events
     */
    public FrequencyCorrectionSyncMonitor(CostasLoop costasLoop, FeedbackDecoder feedbackDecoder)
    {
        mCostasLoop = costasLoop;
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
     * Updates the sync count
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
    }

    /**
     * Resets the monitor
     */
    public void reset()
    {
        mSyncCount = 0;
    }

    @Override
    public void processFrequencyError(long frequencyError)
    {
        mFeedbackDecoder.broadcast(SourceEvent.carrierOffsetMeasurement(-frequencyError));

        //Only rebroadcast as a frequency error measurement if the sync count is more than 2
        if(mSyncCount > 2)
        {
            mFeedbackDecoder.broadcast(SourceEvent.frequencyErrorMeasurement(frequencyError));
        }
    }
}
