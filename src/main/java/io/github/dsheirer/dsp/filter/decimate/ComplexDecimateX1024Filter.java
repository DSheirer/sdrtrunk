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
import io.github.dsheirer.dsp.filter.halfband.complex.ComplexHalfBandDecimationFilter;

/**
 * Decimate by 1024 filter for complex valued sample buffers.
 */
public class ComplexDecimateX1024Filter extends ComplexDecimateX512Filter
{
    private static final int VALIDATION_LENGTH = 1024 * 2;
    private static final int DECIMATE_BY_1024_FILTER_LENGTH = 11;
    private static final Window.WindowType DECIMATE_BY_1024_WINDOW_TYPE = Window.WindowType.BLACKMAN;
    private ComplexHalfBandDecimationFilter mFilter;

    /**
     * Constructs the decimation filter.
     */
    public ComplexDecimateX1024Filter()
    {
        mFilter = new ComplexHalfBandDecimationFilter(FilterFactory.getHalfBand(DECIMATE_BY_1024_FILTER_LENGTH,
                DECIMATE_BY_1024_WINDOW_TYPE));
    }

    @Override
    public float[] decimateComplex(float[] samples)
    {
        validate(samples, VALIDATION_LENGTH);

        //Decimate by this filter, then by the parent decimation filter
        return super.decimateComplex(mFilter.decimateComplex(samples));
    }
}
