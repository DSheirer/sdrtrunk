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
package source.tuner.fcd;

import java.util.concurrent.RejectedExecutionException;

import javax.swing.JPanel;

import sample.Listener;
import sample.adapter.ShortAdapter;
import sample.complex.ComplexBuffer;
import settings.SettingsManager;
import source.SourceException;
import source.tuner.MixerTuner;
import source.tuner.MixerTunerDataLine;
import source.tuner.TunerEvent;
import source.tuner.TunerChannel;
import source.tuner.TunerChannelSource;
import source.tuner.TunerClass;
import source.tuner.TunerType;
import source.tuner.configuration.TunerConfiguration;

public class FCDTuner extends MixerTuner
{
	private FCDTunerController mController;
	
	public FCDTuner( MixerTunerDataLine mixerTDL,
					 FCDTunerController controller )
	{
		super( controller.getConfiguration().toString(), 
			   mixerTDL, 
			   new ShortAdapter() );
		
		mController = controller;
		mController.addListener( this );
	}
	
	public void dispose()
	{
		//TODO: release the mixer tuner data line as well
		
		mController.dispose();
	}
	
	public FCDTunerController getController()
	{
		return mController;
	}
	
	@Override
    public TunerClass getTunerClass()
    {
	    return mController.getTunerClass();
    }

	@Override
    public TunerType getTunerType()
    {
	    return mController.getTunerType();
    }

	@Override
    public String getUniqueID()
    {
	    return mController.getUSBAddress();
    }

	@Override
    public void apply( TunerConfiguration config )throws SourceException
    {
	    mController.apply( config );
    }
	
	@Override
    public int getSampleRate()
    {
	    return (int)mMixerTunerType.getAudioFormat().getSampleRate();
    }
	
	@Override
	public double getSampleSize()
	{
		return 16.0;
	}

	@Override
    public long getFrequency() throws SourceException
    {
		return mController.getFrequency();
    }

	@Override
    public TunerChannelSource getChannel( TunerChannel tunerChannel )
    		throws RejectedExecutionException, SourceException
    {
		TunerChannelSource source = mController.getChannel( this, tunerChannel );

		if( source != null )
		{
			broadcast( new TunerEvent( this, TunerEvent.Event.CHANNEL_COUNT ) );
		}
		
		return source;
    }

	@Override
	public int getChannelCount()
	{
		return mController.getChannelCount();
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
			mController.releaseChannel( source );
		}
    }
}
