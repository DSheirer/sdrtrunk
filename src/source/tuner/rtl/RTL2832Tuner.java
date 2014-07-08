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
package source.tuner.rtl;

import java.util.concurrent.RejectedExecutionException;

import javax.swing.JPanel;
import javax.usb.UsbException;

import log.Log;
import sample.Listener;
import source.SourceException;
import source.tuner.FrequencyChangeListener;
import source.tuner.Tuner;
import source.tuner.TunerChannel;
import source.tuner.TunerChannelSource;
import source.tuner.TunerClass;
import source.tuner.TunerConfiguration;
import source.tuner.TunerType;
import source.tuner.rtl.RTL2832TunerController.SampleRate;
import controller.ResourceManager;
import controller.ThreadPoolManager;

public class RTL2832Tuner extends Tuner
{
	private TunerClass mTunerClass;
	protected RTL2832TunerController mController;

	public RTL2832Tuner( TunerClass tunerClass, 
						 RTL2832TunerController controller ) 
								 			throws SourceException
	{
		super( tunerClass.getVendorDeviceLabel() + "/" + 
			   controller.getTunerType().getLabel() + " #" +
			   controller.getUniqueID() );
		
		mTunerClass = tunerClass;
		mController = controller;
		mController.addListener( (FrequencyChangeListener)this );
	}
	
	public RTL2832TunerController getController()
	{
		return mController;
	}

	@Override
    public String getUniqueID()
    {
	    return mController.getUniqueID();
    }

	@Override
    public TunerClass getTunerClass()
    {
	    return mTunerClass;
    }

	@Override
    public TunerType getTunerType()
    {
	    return mController.getTunerType();
    }
	
	@Override
    public JPanel getEditor( ResourceManager resourceManager )
    {
	    return mController.getEditor( resourceManager );
    }

	@Override
    public void apply( TunerConfiguration config ) throws SourceException
    {
		mController.apply( config );
    }

	@Override
    public int getSampleRate()
    {
		try
		{
		    return mController.getCurrentSampleRate();
		}
		catch( SourceException e )
		{
			Log.error( "RTL2832 Tuner - couldn't get sample rate - " + 
						e.getLocalizedMessage() );
		}
		
		return 0;
    }
	
	public void setSampleRate( SampleRate sampleRate ) throws SourceException
	{
		try
		{
			mController.setSampleRate( sampleRate );
		}
		catch( UsbException e )
		{
			throw new SourceException( "RTL2832 Tuner - error setting "
					+ "sample rate", e );
		}
	}

	@Override
    public long getFrequency() throws SourceException
    {
	    return mController.getFrequency();
    }
	
	public void setFrequency( int frequency ) throws SourceException
	{
		mController.setFrequency( frequency );
	}

	@Override
    public TunerChannelSource getChannel( ThreadPoolManager threadPoolManager,
    									  TunerChannel channel )
    									    throws RejectedExecutionException,
    									    	   SourceException
	{
		return mController.getChannel( threadPoolManager, this, channel );
    }
	/**
	 * Releases the tuned channel so that the tuner controller can tune to
	 * other frequencies as needed.
	 */
	@Override
    public void releaseChannel( TunerChannelSource source )
    {
		/* Unregister for receiving samples */
		removeListener( (Listener<Float[]>)source );
		
		/* Tell the controller to release the channel and cleanup */
		if( source != null )
		{
			mController.releaseChannel( source );
		}
    }

	@Override
	public void addListener( Listener<Float[]> listener )
	{
		mController.addListener( listener );
	}
	
	@Override
	public void removeListener( Listener<Float[]> listener )
	{
		mController.removeListener( listener );
	}
}
