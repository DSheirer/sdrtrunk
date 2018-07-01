/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.dsp.filter.fir.remez;

import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;

public class PolyphaseChannelizerDesigner extends FIRFilterSpecification
{
    /**
     * Constructs a Polyphase TunerChannelizer Remez filter specification.
     *
     * Constructor is package private -- use the PolyphaseChannelizerFilterFactory to create a filter
     *
     * @param order of the filter
     * @param gridDensity to use when calculating the coefficients
     */
    PolyphaseChannelizerDesigner(int order, int gridDensity)
    {
        super(FIRLinearPhaseFilterType.TYPE_1_ODD_LENGTH_EVEN_ORDER_SYMMETRICAL, order, gridDensity);
    }
}
