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


public class AdditiveFloatTap extends FloatTap
{
	private float mValue;
	
	public AdditiveFloatTap( String name, 
						 int delay, 
						 float sampleRateRatio,
						 float addValue )
    {
	    super( name, delay, sampleRateRatio );
	    
	    mValue = addValue;
    }

	@Override
    public void receive( float sample )
    {
		super.receive( sample + mValue );
    }
}
