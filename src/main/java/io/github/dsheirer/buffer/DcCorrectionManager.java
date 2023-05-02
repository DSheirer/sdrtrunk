/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.buffer;

import java.util.concurrent.TimeUnit;

/**
 * Provides DC offset correction value and manages calculation processing interval.
 */
public class DcCorrectionManager
{
//    private static Logger mLog = LoggerFactory.getLogger(DcCorrectionManager.class);
//    private DecimalFormat mDecimalFormat = new DecimalFormat("0.00000");

    /**
     * Time (delay) interval for running DC removal calculations
     */
    private static final long DC_PROCESSING_INTERVAL = TimeUnit.SECONDS.toMillis(50);

    /**
     * Number of buffers to process per DC calculation processing interval.
     */
    private static final int DC_CALCULATIONS_PER_INTERVAL = 5;

    /**
     * DC offset processing residual threshold for ending DC processing interval
     */
    private static final float TARGET_DC_OFFSET_REMAINING = 0.0001f;

    /**
     * Number of DC calculations remaining in the current interval
     */
    private int mDcCalculationsRemaining = DC_CALCULATIONS_PER_INTERVAL; //Initial value for coarse correction

    /**
     * Last time DC offset calculation interval ran
     */
    private long mLastDcCalculationTimestamp = 0;

    /**
     * DC offset calculation gain.
     */
    private static final float DC_FILTER_GAIN = 0.05f;

    /**
     * Current DC offset correction value
     */
    private float mAverageDc = 0.0f;

    public float getAverageDc()
    {
        return mAverageDc;
    }

    /**
     * Adjusts the average DC based on the current calculated DC offset present in a sample buffer.
     * @param calculatedDc for the current sample buffer
     */
    public void adjust(float calculatedDc)
    {
        //Calculate the average scaled DC offset so that it can be applied in the native buffer's converted samples
        float residualOffsetToRemove = calculatedDc - mAverageDc;

        mAverageDc += (residualOffsetToRemove * DC_FILTER_GAIN);

        if(Math.abs(residualOffsetToRemove) < TARGET_DC_OFFSET_REMAINING)
        {
            mDcCalculationsRemaining--;
        }

//        mLog.info("DC: " + mDecimalFormat.format(mAverageDc) +
//                " RES:" + mDecimalFormat.format(residualOffsetToRemove) +
//                " REM:" + mDcCalculationsRemaining);
    }

    /**
     * Indicates if a DC offset calculation for a buffer should be performed.
     */
    public boolean shouldCalculateDc()
    {
        if(System.currentTimeMillis() > (mLastDcCalculationTimestamp + DC_PROCESSING_INTERVAL))
        {
            if(mDcCalculationsRemaining > 0)
            {
                return true;
            }
            else
            {
                //Reset the number of buffers to calculate and the timestamp
                mDcCalculationsRemaining = DC_CALCULATIONS_PER_INTERVAL;
                mLastDcCalculationTimestamp = System.currentTimeMillis();
            }
        }

        return false;
    }
}
