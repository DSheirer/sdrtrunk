/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.spectrum;

public enum DFTSize
{
	FFT00512( 512 ),
	FFT01024( 1024 ),
	FFT02048( 2048 ),
	FFT04096( 4096 ),
	FFT08192( 8192 ),
	FFT16384( 16384 ),
	FFT32768( 32768 );
	
	private int mWidth;
	
	private DFTSize( int width )
	{
		mWidth = width;
	}
	
	public int getSize()
	{
		return mWidth;
	}
	
	public String getLabel()
	{
		return String.valueOf( mWidth );
	}
}
