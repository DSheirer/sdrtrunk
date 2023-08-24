/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.dsp.psk.dqpsk;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;

/**
 * DQPSK demodulator base class
 */
public abstract class DQPSKDemodulator implements Listener<ComplexSamples>
{
    private int mSymbolRate;
    private float mSampleRate;
    protected float mSamplesPerSymbol;
    protected DQPSKSoftSymbolListener mSoftSymbolListener;

//TODO: make subclass implementations of this base class for SCALAR, VECTOR64, VECTOR128, etc.

    /**
     * Constructor
     * @param symbolRate symbols per second
     */
    public DQPSKDemodulator(int symbolRate)
    {
        mSymbolRate = symbolRate;
    }

    /**
     * Registers the listener to receive demodulated soft symbol stream.
     * @param symbolListener to receive soft symbol stream.
     */
    public void setListener(DQPSKSoftSymbolListener symbolListener)
    {
        mSoftSymbolListener = symbolListener;
    }

    /**
     * Sets the sample rate
     * @param sampleRate of the incoming sample stream
     */
    public void setSampleRate(double sampleRate)
    {
        mSampleRate = (float)sampleRate;
        mSamplesPerSymbol = mSampleRate / mSymbolRate;
        if(mSoftSymbolListener != null)
        {
            mSoftSymbolListener.setSamplesPerSymbol(mSamplesPerSymbol);
        }
    }
}
