/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.dsp.psk;

import io.github.dsheirer.sample.complex.Complex;
import io.github.dsheirer.sample.complex.ComplexSampleListener;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterpolatingSampleBufferInstrumented extends InterpolatingSampleBuffer
{
    private final static Logger mLog = LoggerFactory.getLogger(InterpolatingSampleBufferInstrumented.class);
    private SymbolDecisionData mSymbolDecisionData;
    private ComplexSampleListener mSampleListener;
    private int mBufferLength;

    public InterpolatingSampleBufferInstrumented(float samplesPerSymbol, float symbolTimingGain)
    {
        super(samplesPerSymbol, symbolTimingGain);
        mBufferLength = (int) FastMath.ceil(samplesPerSymbol);
        mSymbolDecisionData = new SymbolDecisionData(mBufferLength);
    }

    public void receive(Complex sample)
    {
        super.receive(sample);
        mSymbolDecisionData.receive(sample);

        if(mSampleListener != null)
        {
            mSampleListener.receive(sample.inphase(), sample.quadrature());
        }
    }

    /**
     * Contents of the interpolating buffer and the current buffer index and symbol decision offset.  This data can
     * be used to support an external eye-diagram chart.
     * @return symbol decision data.
     */
    public SymbolDecisionData getSymbolDecisionData()
    {
        for(int x = mDelayLinePointer; x < mDelayLinePointer + mBufferLength; x++)
        {
            mSymbolDecisionData.receive(mDelayLineInphase[x], mDelayLineQuadrature[x]);
        }

        mSymbolDecisionData.setSamplingPoint(getSamplingPoint());
        return mSymbolDecisionData;
    }

    /**
     * Sets the listener to receive samples being sent to this buffer.  Note: these samples have
     * already been corrected by the PLL, so this provides an ideal tap point for PLL corrected samples.
     * @param listener to receive samples.
     */
    public void setSampleListener(ComplexSampleListener listener)
    {
        mSampleListener = listener;
    }
}
