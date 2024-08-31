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

package io.github.dsheirer.source.tuner.sdrplay.api.parameter.event;

import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_GainCbParamT;
import java.lang.foreign.MemorySegment;

/**
 * Gain Callback Parameters structure (sdrplay_api_GainCbParamT)
 */
public class GainCallbackParameters
{
    private final int mGainReductionDb;
    private final int mLnaGainReductionDb;
    private final double mCurrentGain;

    /**
     * Constructs an instance from the foreign memory segment
     */
    public GainCallbackParameters(MemorySegment gainCbParam)
    {
        mGainReductionDb = sdrplay_api_GainCbParamT.gRdB(gainCbParam);
        mLnaGainReductionDb = sdrplay_api_GainCbParamT.lnaGRdB(gainCbParam);
        mCurrentGain = sdrplay_api_GainCbParamT.currGain(gainCbParam);
    }

    /**
     * Current gain reduction value
     */
    public int getGainReductionDb()
    {
        return mGainReductionDb;
    }

    /**
     * Current LNA state setting
     */
    public int getLnaGainReductionDb()
    {
        return mLnaGainReductionDb;
    }

    /**
     * Current gain value
     */
    public double getCurrentGain()
    {
        return mCurrentGain;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("GR:").append(getGainReductionDb());
        sb.append(" LNA:").append(getLnaGainReductionDb());
        sb.append(" CURRENT GAIN:").append(getCurrentGain());
        return sb.toString();
    }
}
