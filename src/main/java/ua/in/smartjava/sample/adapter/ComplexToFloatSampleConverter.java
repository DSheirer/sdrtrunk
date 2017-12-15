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
package ua.in.smartjava.sample.adapter;

import java.util.List;

import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.sample.complex.Complex;
import ua.in.smartjava.source.mixer.MixerChannel;

/**
 * Receives complex samples and send either the left ua.in.smartjava.channel or the right
 * ua.in.smartjava.channel to the short ua.in.smartjava.sample listener
 */
public class ComplexToFloatSampleConverter implements Listener<List<Complex>>
{
	private MixerChannel mChannel;
	private Listener<Float> mListener;
	
	public ComplexToFloatSampleConverter( Listener<Float> listener, 
										  MixerChannel channel )
	{
		mListener = listener;
		mChannel = channel;
	}

	@Override
    public void receive( List<Complex> samples )
    {
		for( Complex sample: samples )
		{
			mListener.receive( mChannel == MixerChannel.LEFT ? 
										   sample.left() : 
										   sample.right() );
		}
    }
	
	public Listener<Float> getListener()
	{
		return mListener;
	}
}
