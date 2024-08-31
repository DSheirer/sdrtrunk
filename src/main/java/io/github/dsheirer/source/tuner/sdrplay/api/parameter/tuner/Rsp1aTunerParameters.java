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

package io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner;

import io.github.dsheirer.source.tuner.sdrplay.api.util.Flag;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_Rsp1aTunerParamsT;
import java.lang.foreign.MemorySegment;

/**
 * RSP-1A Tuner Parameters structure
 */
public class Rsp1aTunerParameters extends TunerParameters
{
    private final MemorySegment mRsp1aTunerParams;

    /**
     * Constructs an instance from the foreign memory segment.
     */
    public Rsp1aTunerParameters(MemorySegment rxChannelParams, MemorySegment rsp1aTunerParams)
    {
        super(rxChannelParams);
        mRsp1aTunerParams = rsp1aTunerParams;
    }

    /**
     * Foreign memory segment for this structure
     */
    private MemorySegment getRsp1aTunerParams()
    {
        return mRsp1aTunerParams;
    }

    /**
     * Indicates if the Bias-T is enabled
     */
    public boolean isBiasT()
    {
        return Flag.evaluate(sdrplay_api_Rsp1aTunerParamsT.biasTEnable(getRsp1aTunerParams()));
    }

    /**
     * Enable or disable the Bias-T
     */
    public void setBiasT(boolean enable)
    {
        sdrplay_api_Rsp1aTunerParamsT.biasTEnable(getRsp1aTunerParams(), Flag.of(enable));
    }
}
