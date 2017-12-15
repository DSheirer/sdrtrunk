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
package ua.in.smartjava.sample.complex;

import java.util.ArrayList;
import java.util.List;

import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.sample.Provider;

public class ComplexSampleAssembler implements Listener<Complex>,
											   Provider<List<Complex>>
{
	private ArrayList<Complex> mBuffer;
	private int mBufferSize;
	private Listener<List<Complex>> mListener;
	
	public ComplexSampleAssembler( int bufferSize )
	{
		mBufferSize = bufferSize;
		mBuffer = new ArrayList<>();
	}
	
	public void dispose()
	{
		mBuffer.clear();
		mListener = null;
	}

	@Override
    public void receive( Complex sample )
    {
		mBuffer.add( sample );
		
		if( mBuffer.size() >= mBufferSize )
		{
			if( mListener != null )
			{
				mListener.receive( mBuffer );
			}
			
			mBuffer = new ArrayList<>();
		}
    }

	@Override
    public void setListener( Listener<List<Complex>> listener )
    {
		mListener = listener;
    }

	@Override
    public void removeListener( Listener<List<Complex>> listener )
    {
		mListener = null;
    }
}
