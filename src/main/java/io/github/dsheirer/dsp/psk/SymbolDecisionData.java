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


public class SymbolDecisionData
{
    private float[] mSamplesInphase;
    private float[] mSamplesQuadrature;
    private float mSamplesPerSymbol;
    private int mSampleIndex;
    private float mSampleIndexOffset;

    /**
     * Symbol decision data - samples and timing decision for a single symbol decision instant.
     *
     * @param samplesInphase
     * @param samplesQuadrature
     * @param samplesPerSymbol
     * @param sampleIndex
     * @param sampleIndexOffset
     */
    public SymbolDecisionData(float[] samplesInphase, float[] samplesQuadrature, float samplesPerSymbol,
                              int sampleIndex, float sampleIndexOffset)
    {
        mSamplesInphase = samplesInphase;
        mSamplesQuadrature = samplesQuadrature;
        mSamplesPerSymbol = samplesPerSymbol;
        mSampleIndex = sampleIndex;
        mSampleIndexOffset = sampleIndexOffset;
    }

    public float[] getInphaseSamples()
    {
        return mSamplesInphase;
    }

    public float[] getQuadratureSamples()
    {
        return mSamplesQuadrature;
    }

    public float getSamplesPerSymbol()
    {
        return mSamplesPerSymbol;
    }

    public int getSampleIndex()
    {
        return mSampleIndex;
    }

    public float getSampleIndexOffset()
    {
        return mSampleIndexOffset;
    }
}
