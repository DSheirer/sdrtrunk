/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsherer.sdrplay.test.listener;

/**
 * Logs the cumulative/running sample count
 */
public class SampleCountLogger implements ISampleCountListener
{
    private final String mLabel;
    private final int mSampleLogInterval;

    public SampleCountLogger(String label, int sampleLoggingInterval)
    {
        mLabel = label;
        mSampleLogInterval = sampleLoggingInterval;
    }

    @Override
    public void sampleCount(long sampleCount)
    {

//        mEmitSampleLogCount++;
//
//        if(mEmitSampleLogCount % mSampleLogInterval == 0)
//        {
//            mEmitSampleLogCount = 0;
//            mLog.info(mLabel + " - Samples Received: " + mSampleCount);
//        }

    }
}
