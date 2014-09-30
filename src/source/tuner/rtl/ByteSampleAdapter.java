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
package source.tuner.rtl;

import source.mixer.SampleAdapter;

public class ByteSampleAdapter extends SampleAdapter
{
	private final static float[] LOOKUP_VALUES;

	/**
	 * Creates a static lookup table that converts the 8-bit valued range 
	 * from 0 - 255 into scaled float values of -32768.0 to 0 to 32512.0
	 */
	static
	{
		LOOKUP_VALUES = new float[ 256 ];
		
		for( int x = 0; x < 256; x++ )
		{
			LOOKUP_VALUES[ x ] = (float)( x - 128 ) * 256.0f;
		}
		
		LOOKUP_VALUES[ 128 ] = 0.0000001f;
	}

	@Override
    public Float[] convert( byte[] samples )
    {
		Float[] convertedSamples = new Float[ samples.length ];
		int pointer = 0;
		
		for( byte sample: samples )
		{
			/* Convert byte value into float from the lookup table */
			convertedSamples[ pointer++ ] = LOOKUP_VALUES[ ( sample & 0xFF ) ];
		}
		
	    return convertedSamples;
    }
}
