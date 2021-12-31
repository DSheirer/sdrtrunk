/*
 * *****************************************************************************
 * Copyright (C) 2014-2021 Dennis Sheirer
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

package io.github.dsheirer.dsp.filter.decimate;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.Window;
import io.github.dsheirer.dsp.filter.halfband.real.RealHalfBandDecimationFilter;

/**
 * Constructs the decimation filter.
 */
public class RealDecimateX2Filter extends RealHalfBandDecimationFilter
{
    private static final int DECIMATE_BY_2_FILTER_LENGTH = 63;
    private static final Window.WindowType DECIMATE_BY_2_WINDOW_TYPE = Window.WindowType.HAMMING;

    /**
     * Creates a half band filter with inherent decimation by two.
     */
    public RealDecimateX2Filter()
    {
        super(FilterFactory.getHalfBand(DECIMATE_BY_2_FILTER_LENGTH, DECIMATE_BY_2_WINDOW_TYPE));
    }

    /**
     * Validates that the samples length is an integer multiple of the specified validation multiple argument.
     * @param samples to validate
     * @param validationMultiple to validate against
     */
    protected static void validate(float[] samples, int validationMultiple)
    {
        if(samples.length % validationMultiple != 0)
        {
            throw new IllegalArgumentException("Sample buffer length [" + samples.length +
                    "] must be an integer multiple of " + validationMultiple);
        }
    }
}
