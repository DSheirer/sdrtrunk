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
package io.github.dsheirer.dsp.afsk;

public class AFSKSampleBufferInstrumented extends AFSKSampleBuffer
{
    public boolean mLastSymbol;

    /**
     * Buffer to store complex sample data and produce interpolated samples.
     *
     * @param samplesPerSymbol
     * @param symbolTimingGain
     */
    public AFSKSampleBufferInstrumented(int samplesPerSymbol, float symbolTimingGain)
    {
        super(samplesPerSymbol, symbolTimingGain);
    }

    /**
     * Sample delay buffer containing boolean (ie positive or negative) samples.
     */
    public boolean[] getDelayLine()
    {
        return mDelayLine;
    }

    /**
     * Current delay buffer pointer which points to where the next sample will be stored and/or the oldest sample in
     * the buffer.
     */
    public int getDelayLinePointer()
    {
        return mDelayLinePointer;
    }

    public int getDelayLineSecondPointer()
    {
        return mDelayLinePointer + mTwiceSamplesPerSymbol;
    }

    /**
     * Current symbol timing gain value
     * @return
     */
    public float getSymbolTimingGain()
    {
        return mSymbolTimingGain;
    }

    public float getSamplesPerSymbol()
    {
        return mSamplesPerSymbol;
    }

    @Override
    public boolean getSymbol()
    {
        mLastSymbol = super.getSymbol();
        return mLastSymbol;
    }

    public boolean getLastSymbol()
    {
        return mLastSymbol;
    }

    public int getSymbolStart()
    {
        return mSymbolStart;
    }

    public int getSymbolEnd()
    {
        return mSymbolEnd;
    }
}
