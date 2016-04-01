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
package source.tuner.fcd;

import java.util.concurrent.RejectedExecutionException;

import sample.Listener;
import sample.adapter.ShortAdapter;
import sample.complex.ComplexBuffer;
import source.SourceException;
import source.tuner.MixerTuner;
import source.tuner.MixerTunerDataLine;
import source.tuner.TunerChannel;
import source.tuner.TunerChannelSource;
import source.tuner.TunerClass;
import source.tuner.TunerEvent;
import source.tuner.TunerType;

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
	 * Releases the tuned channel so that the tuner controller can tune to
	 * other frequencies as needed.
	 */
	@Override
    public void releaseChannel( TunerChannelSource source )
    {
		if( source != null )
		{
			/* Unregister for receiving samples */
			removeListener( (Listener<ComplexBuffer>)source );

			/* Tell the controller to release the channel and cleanup */
			/* This will release the channel as a frequency change listener */
			getController().releaseChannel( source );
		}
    }
}
