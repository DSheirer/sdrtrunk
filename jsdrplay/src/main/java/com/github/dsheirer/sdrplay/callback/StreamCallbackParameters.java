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

package com.github.dsheirer.sdrplay.callback;

import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_StreamCbParamsT;
import com.github.dsheirer.sdrplay.util.Flag;

import java.lang.foreign.MemorySegment;

/**
 * Stream Callback Parameters structure (sdrplay_api_StreamCbParamsT)
 */
public class StreamCallbackParameters
{
    private int mFirstSampleNumber;
    private boolean mGainReductionChanged;
    private boolean mRfFrequencyChanged;
    private boolean mSampleRateChanged;
    private long mNumberSamples;

    /**
     * Constructs an instance from the foreign memory segment
     */
    public StreamCallbackParameters(MemorySegment memorySegment)
    {
        mFirstSampleNumber = sdrplay_api_StreamCbParamsT.firstSampleNum$get(memorySegment);
        mGainReductionChanged = Flag.evaluate(sdrplay_api_StreamCbParamsT.grChanged$get(memorySegment));
        mRfFrequencyChanged = Flag.evaluate(sdrplay_api_StreamCbParamsT.rfChanged$get(memorySegment));
        mSampleRateChanged = Flag.evaluate(sdrplay_api_StreamCbParamsT.fsChanged$get(memorySegment));
        mNumberSamples = sdrplay_api_StreamCbParamsT.numSamples$get(memorySegment);
    }

    /**
     * First sample number
     */
    public int getFirstSampleNumber()
    {
        return mFirstSampleNumber;
    }

    /**
     * Indicates if gain reduction value has changed
     */
    public boolean isGainReductionChanged()
    {
        return mGainReductionChanged;
    }

    /**
     * Indicates if RF center frequency has changed
     */
    public boolean isRfFrequencyChanged()
    {
        return mRfFrequencyChanged;
    }

    /**
     * Indicates if sample rate has changed
     */
    public boolean isSampleRateChanged()
    {
        return mSampleRateChanged;
    }

    /**
     * Number of samples
     */
    public long getNumberSamples()
    {
        return mNumberSamples;
    }
}
