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

import source.Source.SampleType;
import source.mixer.SampleAdapter;

public class RTL2832SampleAdapter extends SampleAdapter
{
	private final static float[] mLOOKUP_VALUES;

	/**
	 * Creates a static lookup table that converts the 8-bit valued range 
	 * from 0 - 255 into scaled float values of -128 to 0 to 127
	 */
	static
	{
		mLOOKUP_VALUES = new float[ 256 ];
		
		float scale = (float)( 1.0f / 127.0f );
		
		for( int x = 0; x < 256; x++ )
		{
			mLOOKUP_VALUES[ x ] = (float)( x - 128 ) * scale;
		}
		
		mLOOKUP_VALUES[ 128 ] = 0.0000001f;
	}

	@Override
    public Float[] convert( byte[] samples )
    {
		Float[] convertedSamples = new Float[ samples.length ];
		int pointer = 0;
		
		for( byte sample: samples )
		{
			/* Convert byte value into float from the lookup table */
			convertedSamples[ pointer++ ] = mLOOKUP_VALUES[ ( sample & 0xFF ) ];
		}
		
	    return convertedSamples;
    }
}
