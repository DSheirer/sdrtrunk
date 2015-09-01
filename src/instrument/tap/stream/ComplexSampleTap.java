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

import instrument.tap.TapListener;
import instrument.tap.TapType;
import sample.Listener;
import sample.complex.Complex;
import sample.complex.ComplexSampleListener;

public class ComplexSampleTap extends StreamTap implements ComplexSampleListener
{
	private ComplexSampleListener mListener;
	
	public ComplexSampleTap( String name, 
					   int delay, 
					   float sampleRateRatio )
    {
	    super( TapType.STREAM_COMPLEX_SAMPLE, name, delay, sampleRateRatio );
    }
	
	public ComplexSampleTap( TapType tapType, String name, int delay, float sampleRateRatio )
	{
	    super( tapType, name, delay, sampleRateRatio );
	}

	@Override
    public void receive( float inphase, float quadrature )
    {
		if( mListener != null )
		{
			mListener.receive( inphase, quadrature );
		}
		
		for( TapListener listener: mListeners )
		{
			listener.receive( new Complex( inphase, quadrature ) );
		}
    }

    public void setListener( ComplexSampleListener listener )
    {
		mListener = listener;
    }

    public void removeListener( ComplexSampleListener listener )
    {
		mListener = null;
    }
}
