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
package io.github.dsheirer.dsp.filter.channelizer;

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;

/**
 * Factory for selecting the optimal implementation of the channel DFT for this hardware, as determined by the
 * Calibration Manager.
 */
public class ChannelDFTFactory
{
    /**
     * Creates the optimal channel DFT implementation.
     * @param channelCount as the size of the transform
     * @return channel DFT
     */
    public static ChannelDFT getChannelDFT(int channelCount)
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.CHANNEL_DFT);

        switch(implementation)
        {
            case VECTOR_SIMD_PREFERRED:
                return new VectorChannelDFT(channelCount);
            case SCALAR:
            default:
                return new ScalarChannelDFT(channelCount);
        }
    }
}
