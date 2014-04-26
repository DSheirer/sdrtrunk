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
package instrument.tap.stream;

import instrument.tap.Tap;
import instrument.tap.TapType;

public abstract class StreamTap extends Tap
{
	protected float mSampleRateRatio;
	
	public StreamTap( TapType type,
					  String name, 
					  int delay,
					  float sampleRate )
    {
	    super( type, name, delay );
	    
	    mSampleRateRatio = sampleRate;
    }
	
	public float getSampleRateRatio()
	{
		return mSampleRateRatio;
	}
}
