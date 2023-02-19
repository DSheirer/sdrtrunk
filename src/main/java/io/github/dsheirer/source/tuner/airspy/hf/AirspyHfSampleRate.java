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

package io.github.dsheirer.source.tuner.airspy.hf;

import java.text.DecimalFormat;

/**
 * Airspy HF sample rate.
 */
public class AirspyHfSampleRate
{
    private static final DecimalFormat KILOHERTZ_FORMATTER = new DecimalFormat("0.0");
    private int mIndex;
    private int mSampleRate;
    private boolean mLowIf;

    /**
     * Constructs an instance
     * @param sampleRate in Hertz
     * @param lowIf true if Low IF (LIF) or (default) false if Zero IF (ZIF)
     */
    public AirspyHfSampleRate(int index, int sampleRate, boolean lowIf)
    {
        mIndex = index;
        mSampleRate = sampleRate;
        mLowIf = lowIf;
    }

    /**
     * Index for the sample rate in the tuner's sample rate structure.
     */
    public short getIndex()
    {
        return (short)mIndex;
    }

    /**
     * Value of sample rate
     * @return rate in Hertz
     */
    public int getSampleRate()
    {
        return mSampleRate;
    }

    /**
     * Indicates if the sample rate uses Low IF (LIF) versus Zero IF (ZIF)
     * @return true if LIF
     */
    public boolean isLowIf()
    {
        return mLowIf;
    }

    @Override
    public String toString()
    {
        return KILOHERTZ_FORMATTER.format(mSampleRate / 1E3) + " kHz " + (isLowIf() ? "(Low IF)" : "(Zero IF)");
    }
}
