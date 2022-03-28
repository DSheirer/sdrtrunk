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

package io.github.dsheirer.spectrum;

import io.github.dsheirer.dsp.filter.smoothing.SmoothingFilter.SmoothingType;

public interface SpectralDisplayAdjuster
{
	/**
	 * Gets the averaging value indicating the number of FFT results that are
	 * averaged to produce each FFT results output.
	 * 
	 * @return averaging value ( 2 - 50 )
	 */
	public int getAveraging();
	public void setAveraging( int averaging );

	/**
	 * Sets the smoothing filter averaging window width.
	 * 
	 * Valid values are odd in the range 3 - 29
	 */
	public int getSmoothing();
	public void setSmoothing( int smoothing );

	/**
	 * Sets the type of smoothing filter to use.
	 */
	public SmoothingType getSmoothingType();
	public void setSmoothingType( SmoothingType type );
	
}
