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

import java.awt.image.IndexColorModel;


public class WaterfallColorModel
{
	public static IndexColorModel getDefaultColorModel()
	{
		int bitDepth = 8;
		int indexSize = 256;
		
		byte[] red = new byte[ indexSize ];
		byte[] green = new byte[ indexSize ];
		byte[] blue = new byte[ indexSize ];

		//Background noise
		for( int x = 0; x < 16; x++ ) //Blue
		{
			red[ x ] = (byte)0;

			green[ x ] = (byte)0;

			blue[ x ] = (byte)127;
		}

		for( int x = 16; x < 32; x++ ) //Blue
		{
			red[ x ] = (byte)0;

			green[ x ] = (byte)0;

			blue[ x ] = (byte)191;
		}

		int r = 0;
		int g = 0;
		int b = 191;

		for( int x = 32; x < 60; x++ )
		{
			red[ x ] = (byte)r;
			r += 9;

			green[ x ] = (byte)g;
			g += 9;

			blue[ x ] = (byte)b;
			b -= 6;
		}

		r = 255;
		g = 255;
		b = 0;
		
		for( int x = 60; x < 188; x++ ) //Yellow
		{
			red[ x ] = (byte)r;

			green[ x ] = (byte)g;
			g -= 2;

			blue[ x ] = (byte)b;
		}

		for( int x = 188; x < 256; x++ ) //Red
		{
			red[ x ] = (byte)255;

			green[ x ] = (byte)0;

			blue[ x ] = (byte)0;
		}
		
		return new IndexColorModel( bitDepth, indexSize, red, green, blue );
	}
}
