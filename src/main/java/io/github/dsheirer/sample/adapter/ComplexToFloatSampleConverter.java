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
package io.github.dsheirer.sample.adapter;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.Complex;
import io.github.dsheirer.source.mixer.MixerChannel;

import java.util.List;

/**
 * Receives complex samples and send either the left channel or the right
 * channel to the short sample listener
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
