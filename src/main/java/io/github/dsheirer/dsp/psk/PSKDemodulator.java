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
package io.github.dsheirer.dsp.psk;

import io.github.dsheirer.dsp.psk.pll.IPhaseLockedLoop;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ComplexBuffer;
import io.github.dsheirer.sample.complex.Complex;
import io.github.dsheirer.sample.complex.ComplexSampleListener;

public abstract class PSKDemodulator<T> implements ComplexSampleListener
{
    private InterpolatingSampleBuffer mInterpolatingSampleBuffer;
    private IPhaseLockedLoop mPLL;
    private Complex mReceivedSample = new Complex(0, 0);
    private Listener<T> mSymbolListener;

    /**
     * Abstract Phase Shift Keyed (PSK) demodulator
     * @param interpolatingSampleBuffer to store complex samples and provide access to interpolated samples
     * @param phaseLockedLoop to track and remove frequency offset in the incoming samples
     */
    public PSKDemodulator(InterpolatingSampleBuffer interpolatingSampleBuffer, IPhaseLockedLoop phaseLockedLoop)
    {
        mInterpolatingSampleBuffer = interpolatingSampleBuffer;
        mPLL = phaseLockedLoop;
    }

    /**
     * Registers the listener to receive symbol decisions from this demodulator
     */
    public void setSymbolListener(Listener<T> listener)
    {
        mSymbolListener = listener;
    }

    /**
     * Broadcasts the symbol decision to the registered symbol listener
     * @param symbol
     */
    protected void broadcast(T symbol)
    {
        if(mSymbolListener != null)
        {
            mSymbolListener.receive(symbol);
        }
    }

    /**
     * Interpolating sample buffer for receiving the incoming complex sample stream and providing access to
     * indexed and interpolated samples
     */
    protected InterpolatingSampleBuffer getInterpolatingSampleBuffer()
    {
        return mInterpolatingSampleBuffer;
    }

    /**
     * Phase Locked Loop (PLL)
     */
    protected IPhaseLockedLoop getPLL()
    {
        return mPLL;
    }

    /**
     * Processes a (filtered) buffer containing complex samples for decoding
     * @param reusableComplexBuffer with complex samples
     */
    public void receive(ComplexBuffer reusableComplexBuffer)
    {
        float[] samples = reusableComplexBuffer.getSamples();

        for(int x = 0; x < samples.length; x += 2)
        {
            receive(samples[x], samples[x + 1]);
        }

        }

    /**
     * Processes a complex sample for decoding.  Once sufficient samples are buffered, a symbol decision is made.
     * @param inphase value for the sample
     * @param quadrature value for the sample
     */
    @Override
    public void receive(float inphase, float quadrature)
    {
        //Update current sample with values
        mReceivedSample.setValues(inphase, quadrature);

        //Mix current sample with costas loop to remove any rotation that is present from a mis-tuned carrier frequency
        mReceivedSample.multiply(mPLL.incrementAndGetCurrentVector());

        //Store the sample in the interpolating buffer
        mInterpolatingSampleBuffer.receive(mReceivedSample);

        //Calculate the symbol once we've stored enough samples
        if(mInterpolatingSampleBuffer.hasSymbol())
        {
            calculateSymbol();
        }
    }

    protected abstract void calculateSymbol();

    /**
     * Constrains value to the range of ( -maximum <> maximum )
     */
    public static float clip(float value, float maximum)
    {
        if(value > maximum)
        {
            return maximum;
        }
        else if(value < -maximum)
        {
            return -maximum;
        }

        return value;
    }
}
