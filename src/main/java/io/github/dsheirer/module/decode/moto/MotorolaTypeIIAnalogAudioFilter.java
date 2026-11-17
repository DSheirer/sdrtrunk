/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.moto;

import io.github.dsheirer.audio.AbstractAudioFilter;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.window.WindowType;

/**
 * De-emphasis and 3 kHz low-pass for analog voice on a Motorola Type II traffic channel.
 *
 * The de-emphasis is a single pole with unity gain at low frequency, so the level of the voice fundamentals is not
 * changed and only the treble is reduced.
 */
public class MotorolaTypeIIAnalogAudioFilter extends AbstractAudioFilter
{
    private static final double SAMPLE_RATE = 8000.0;
    private static final int LOW_PASS_PASS_HZ = 3000;
    private static final int LOW_PASS_STOP_HZ = 3400;

    private final float mAlpha;
    private float mPrevious;
    private final IRealFilter mLowPassFilter;

    /**
     * Constructs an instance.
     * @param deemphasis to apply, other than none.
     */
    public MotorolaTypeIIAnalogAudioFilter(AnalogDeemphasis deemphasis)
    {
        double timeConstant = deemphasis.getTimeConstantMicroseconds() / 1.0E6;
        mAlpha = timeConstant > 0 ? (float)Math.exp(-1.0 / (SAMPLE_RATE * timeConstant)) : 0.0f;
        mLowPassFilter = FilterFactory.getRealFilter(FilterFactory.getLowPass(SAMPLE_RATE, LOW_PASS_PASS_HZ,
            LOW_PASS_STOP_HZ, 60, WindowType.HAMMING, true));
    }

    @Override
    public float[] filter(float[] audio)
    {
        for(int x = 0; x < audio.length; x++)
        {
            mPrevious = audio[x] + mAlpha * (mPrevious - audio[x]);
            audio[x] = mPrevious;
        }

        return mLowPassFilter.filter(audio);
    }
}
