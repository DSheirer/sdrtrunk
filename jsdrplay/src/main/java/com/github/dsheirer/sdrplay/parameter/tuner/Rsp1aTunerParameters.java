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

package com.github.dsheirer.sdrplay.parameter.tuner;

import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_Rsp1aTunerParamsT;
import com.github.dsheirer.sdrplay.util.Flag;

import java.lang.foreign.MemorySegment;

/**
 * RSP-1A Tuner Parameters structure
 */
public class Rsp1aTunerParameters extends TunerParameters
{
    private MemorySegment mRsp1aMemorySegment;

    /**
     * Constructs an instance from the foreign memory segment.
     */
    public Rsp1aTunerParameters(MemorySegment tunerParametersMemorySegment, MemorySegment rsp1aMemorySegment)
    {
        super(tunerParametersMemorySegment);
        mRsp1aMemorySegment = rsp1aMemorySegment;
    }

    /**
     * Foreign memory segment for this structure
     */
    private MemorySegment getRsp1aMemorySegment()
    {
        return mRsp1aMemorySegment;
    }

    /**
     * Indicates if the Bias-T is enabled
     */
    public boolean isBiasT()
    {
        return Flag.evaluate(sdrplay_api_Rsp1aTunerParamsT.biasTEnable$get(getRsp1aMemorySegment()));
    }

    /**
     * Enable or disable the Bias-T
     */
    public void setBiasT(boolean enable)
    {
        sdrplay_api_Rsp1aTunerParamsT.biasTEnable$set(getRsp1aMemorySegment(), Flag.of(enable));
    }
}
