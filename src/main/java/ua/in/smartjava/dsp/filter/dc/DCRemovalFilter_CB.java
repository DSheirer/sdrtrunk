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
package ua.in.smartjava.dsp.filter.dc;

import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.sample.complex.ComplexBuffer;

public class DCRemovalFilter_CB implements Listener<ComplexBuffer>
{
	private Listener<ComplexBuffer> mListener;
	private DCRemovalFilter mIFilter;
	private DCRemovalFilter mQFilter;
	
	public DCRemovalFilter_CB( float ratio )
	{
		mIFilter = new DCRemovalFilter( ratio );
		mQFilter = new DCRemovalFilter( ratio );
	}
	
	public void dispose()
	{
		mListener = null;
	}
	
	public float[] filter( float[] samples )
	{
		for( int x = 0; x < samples.length; x += 2 )
		{
			samples[ x ] = mIFilter.filter( samples[ x ] );
			samples[ x + 1 ] = mQFilter.filter( samples[ x + 1 ] );
		}

		return samples;
	}
	
	public ComplexBuffer filter( ComplexBuffer buffer )
	{
		filter( buffer.getSamples() );
		
		return buffer;
	}
	
	@Override
	public void receive( ComplexBuffer buffer )
	{
		if( mListener != null )
		{
			mListener.receive( filter( buffer ) );
		}
	}
	
	public void setListener( Listener<ComplexBuffer> listener )
	{
		mListener = listener;
	}
}
