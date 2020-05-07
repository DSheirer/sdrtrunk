/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.bits;

import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
import org.apache.commons.lang3.Validate;

public class SoftSyncDetector implements ISyncProcessor
{
    private ISyncDetectListener mListener;
    private long mPattern;
    private int mThreshold;
    private int mBitErrorCount;

    public SoftSyncDetector(long pattern, int threshold, ISyncDetectListener listener)
    {
        Validate.notNull(listener, "Sync detect listener cannot be null");
        mListener = listener;
        mPattern = pattern;
        mThreshold = threshold;
    }

    public SoftSyncDetector(long pattern, int threshold) {
        mPattern = pattern;
        mThreshold = threshold;
    }
    public void dispose()
    {
        mListener = null;
    }

    @Override
    public boolean checkSync(long value)
    {
        long difference = value ^ mPattern;

        if(difference == 0)
        {
            mListener.syncDetected(0);
            return true;
        }
        else
        {
            mBitErrorCount = Long.bitCount(difference);

            if(mBitErrorCount <= mThreshold)
            {
                mListener.syncDetected(mBitErrorCount);
                return true;
            }
        }

        return false;
    }

    public void setThreshold(int threshold)
    {
        mThreshold = threshold;
    }

    public void setListener(ISyncDetectListener listener)
    {
        mListener = listener;
    }
}
