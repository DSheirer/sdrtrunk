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
import source.SourceException;
import source.mixer.ShortAdapter;
import source.tuner.FrequencyChangeListener;
import source.tuner.MixerTuner;
import source.tuner.MixerTunerDataLine;
import source.tuner.TunerChannel;
import source.tuner.TunerChannelSource;
import source.tuner.TunerClass;
import source.tuner.TunerConfiguration;
import source.tuner.TunerType;
import controller.ResourceManager;
import controller.ThreadPoolManager;

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
		mController.addListener( (FrequencyChangeListener)this );
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
	    return mController.getAddress();
    }

	@Override
	public JPanel getEditor( ResourceManager resourceManager )
	{
		return mController.getEditor( this, resourceManager );
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
    public long getFrequency() throws SourceException
    {
		return mController.getFrequency();
    }

	@Override
    public TunerChannelSource getChannel( ThreadPoolManager threadPoolManager,
    	TunerChannel tunerChannel )	throws RejectedExecutionException, 
    									   SourceException
    {
		return mController.getChannel( threadPoolManager, this, tunerChannel );
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
			removeListener( (Listener<Float[]>)source );

			/* Tell the controller to release the channel and cleanup */
			/* This will release the channel as a frequency change listener */
			mController.releaseChannel( source );
		}
    }
}
