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
package sample.complex;

@Deprecated
public class RTLComplexSample extends ComplexSample
{
    private static final long serialVersionUID = 1L;

	private static float[] mValues;

	/**
	 * Creates a lookup table that converts the 8-bit valued range from 0 - 255
	 * into scaled float values of -128 to 0 to 127
	 */
	static
	{
		mValues = new float[ 256 ];
		
		float scale = (float)( 1.0f / 127.0f );
		
		for( int x = 0; x < 256; x++ )
		{
			mValues[ x ] = (float)( x - 128 ) * scale;
		}
		
		mValues[ 128 ] = 0.0000001f;
	}

	/**
	 * Constructs a complex sample from the sample format produced by the 
	 * RTL-2832 chip, where the byte values run from 0 - 255, but represent
	 * the value -127 to 127
	 * 
	 * @param left
	 * @param right
	 */
	public RTLComplexSample( byte left, byte right )
    {
	    super( mValues[ left & 0xFF ], mValues[ right & 0xFF ] );
    }
}
