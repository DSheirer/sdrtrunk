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

import io.github.dsheirer.sample.complex.Complex;

public class InterpolatingSampleBufferInstrumented extends InterpolatingSampleBuffer
{
    private SymbolDecisionData mSymbolDecisionData;

    public InterpolatingSampleBufferInstrumented(float samplesPerSymbol, float symbolTimingGain)
    {
        super(samplesPerSymbol, symbolTimingGain);
        mSymbolDecisionData = new SymbolDecisionData((int)samplesPerSymbol);
    }

    public void receive(Complex sample)
    {
        super.receive(sample);
        mSymbolDecisionData.receive(sample);
    }

    /**
     * Contents of the interpolating buffer and the current buffer index and symbol decision offset.  This data can
     * be used to support an external eye-diagram chart.
     * @return symbol decision data.
     */
    public SymbolDecisionData getSymbolDecisionData()
    {
        mSymbolDecisionData.setSamplingPoint(getSamplingPoint());
        return mSymbolDecisionData;
    }

}
