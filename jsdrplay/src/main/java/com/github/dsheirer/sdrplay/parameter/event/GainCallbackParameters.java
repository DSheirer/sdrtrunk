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

package com.github.dsheirer.sdrplay.parameter.event;

import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_GainCbParamT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.MemorySegment;

/**
 * Gain Callback Parameters structure (sdrplay_api_GainCbParamT)
 */
public class GainCallbackParameters
{
    private static final Logger mLog = LoggerFactory.getLogger(GainCallbackParameters.class);
    private int mGainReductionDb;
    private int mLnaGainReductionDb;
    private double mCurrentGain;

    /**
     * Constructs an instance from the foreign memory segment
     */
    public GainCallbackParameters(MemorySegment memorySegment)
    {
        mGainReductionDb = sdrplay_api_GainCbParamT.gRdB$get(memorySegment);
        mLnaGainReductionDb = sdrplay_api_GainCbParamT.lnaGRdB$get(memorySegment);
        mCurrentGain = sdrplay_api_GainCbParamT.currGain$get(memorySegment);
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
