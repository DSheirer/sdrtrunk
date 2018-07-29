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

public class SoftSyncDetector implements ISyncProcessor
{
    private ISyncDetectListener mListener;
    private long mPattern;
    private int mThreshold;

    public SoftSyncDetector(ISyncDetectListener listener, long pattern, int threshold)
    {
        this(pattern, threshold);

        mListener = listener;
    }

    public SoftSyncDetector(long pattern, int threshold)
    {
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

        if((difference == 0 || Long.bitCount(difference) <= mThreshold) &&
            mListener != null)
        {
            mListener.syncDetected();
            return true;
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
