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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.LibUsbException;

import sample.Listener;
import sample.complex.ComplexBuffer;
import settings.SettingsManager;
import source.SourceException;
import source.tuner.Tuner;
import source.tuner.TunerChannel;
import source.tuner.TunerChannelSource;
import source.tuner.TunerClass;
import source.tuner.TunerConfiguration;
import source.tuner.TunerType;
import source.tuner.frequency.IFrequencyChangeListener;
import source.tuner.rtl.RTL2832TunerController.SampleRate;
import controller.ThreadPoolManager;

public class RTL2832Tuner extends Tuner
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( RTL2832Tuner.class );

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
		mController.addListener( this );
	}
	
	public void dispose()
	{
		//TODO: dispose of something here
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
    public JPanel getEditor( SettingsManager settingsManager )
    {
	    return mController.getEditor( settingsManager );
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
			mLog.error( "RTL2832 Tuner - couldn't get sample rate", e );
		}
		
		return 0;
    }
	
	public void setSampleRate( SampleRate sampleRate ) throws SourceException
	{
		try
		{
			mController.setSampleRate( sampleRate );
		}
		catch( LibUsbException e )
		{
			throw new SourceException( "RTL2832 Tuner - error setting "
					+ "sample rate", e );
		}
	}
	
	@Override
	public double getSampleSize()
	{
		//Note: although sample size is 8, we set it to 11 to align with the
		//actual noise floor.
		return 11.0;
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
		TunerChannelSource source = null;
		
		try
		{
			source = mController.getChannel( threadPoolManager, this, channel );
		}
		catch( Exception e )
		{
			mLog.error( "couldn't provide source channel", e );
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
		/* Unregister for receiving samples */
		removeListener( (Listener<ComplexBuffer>)source );
		
		/* Tell the controller to release the channel and cleanup */
		if( source != null )
		{
			mController.releaseChannel( source );
		}
    }

	@Override
	public void addListener( Listener<ComplexBuffer> listener )
	{
		mController.addListener( listener );
	}
	
	@Override
	public void removeListener( Listener<ComplexBuffer> listener )
	{
		mController.removeListener( listener );
	}
}
