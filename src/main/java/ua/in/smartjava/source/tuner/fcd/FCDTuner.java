/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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
package ua.in.smartjava.source.tuner.fcd;

import java.util.concurrent.RejectedExecutionException;

import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.sample.adapter.ShortAdapter;
import ua.in.smartjava.sample.complex.ComplexBuffer;
import ua.in.smartjava.source.SourceException;
import ua.in.smartjava.source.tuner.MixerTuner;
import ua.in.smartjava.source.tuner.MixerTunerDataLine;
import ua.in.smartjava.source.tuner.TunerChannel;
import ua.in.smartjava.source.tuner.TunerChannelSource;
import ua.in.smartjava.source.tuner.TunerClass;
import ua.in.smartjava.source.tuner.TunerEvent;
import ua.in.smartjava.source.tuner.TunerType;

public class FCDTuner extends MixerTuner
{
	public FCDTuner( MixerTunerDataLine mixerTDL,
					 FCDTunerController controller )
	{
		super( controller.getConfiguration().toString(),
			   controller,
			   mixerTDL, 
			   new ShortAdapter() );
	}
	
	public void dispose()
	{
		//TODO: release the mixer tuner data line as well
		
		getController().dispose();
	}
	
	public FCDTunerController getController()
	{
		return (FCDTunerController)getTunerController();
	}
	
	@Override
    public TunerClass getTunerClass()
    {
	    return getController().getTunerClass();
    }

	@Override
    public TunerType getTunerType()
    {
	    return getController().getTunerType();
    }

	@Override
    public String getUniqueID()
    {
	    return getController().getUSBAddress();
    }

	@Override
	public double getSampleSize()
	{
		return 16.0;
	}

	@Override
    public TunerChannelSource getChannel( TunerChannel tunerChannel )
    		throws RejectedExecutionException, SourceException
    {
		TunerChannelSource source = getController().getChannel( this, tunerChannel );

		if( source != null )
		{
			broadcast( new TunerEvent( this, TunerEvent.Event.CHANNEL_COUNT ) );
		}
		
		return source;
    }

	/**
	 * Releases the tuned ua.in.smartjava.channel so that the tuner ua.in.smartjava.controller can tune to
	 * other frequencies as needed.
	 */
	@Override
    public void releaseChannel( TunerChannelSource source )
    {
		if( source != null )
		{
			/* Unregister for receiving samples */
			removeListener( (Listener<ComplexBuffer>)source );

			/* Tell the ua.in.smartjava.controller to release the ua.in.smartjava.channel and cleanup */
			/* This will release the ua.in.smartjava.channel as a frequency change listener */
			getController().releaseChannel( source );
		}
    }
}
