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
package io.github.dsheirer.source.tuner.rtl;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexBuffer;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerChannel;
import io.github.dsheirer.source.tuner.TunerChannelSource;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.TunerEvent;
import io.github.dsheirer.source.tuner.TunerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.LibUsbException;

import java.util.concurrent.RejectedExecutionException;

public class RTL2832Tuner extends Tuner
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( RTL2832Tuner.class );

	private TunerClass mTunerClass;

	public RTL2832Tuner( TunerClass tunerClass, 
						 RTL2832TunerController controller ) 
								 			throws SourceException
	{
		super( tunerClass.getVendorDeviceLabel() + "/" + 
			   controller.getTunerType().getLabel() + " " +
			   controller.getUniqueID(), controller );
		
		mTunerClass = tunerClass;
	}
	
	public void dispose()
	{
		//TODO: dispose of something here
	}
	
	public RTL2832TunerController getController()
	{
		return (RTL2832TunerController)getTunerController();
	}

	@Override
    public String getUniqueID()
    {
	    return getController().getUniqueID();
    }

	@Override
    public TunerClass getTunerClass()
    {
	    return mTunerClass;
    }

	@Override
    public TunerType getTunerType()
    {
	    return getController().getTunerType();
    }
	
	public void setSampleRate( RTL2832TunerController.SampleRate sampleRate ) throws SourceException
	{
		try
		{
			getController().setSampleRate( sampleRate );
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

	public void setFrequency( int frequency ) throws SourceException
	{
		getController().setFrequency( frequency );
	}

	@Override
    public TunerChannelSource getChannel(TunerChannel channel )
    									    throws RejectedExecutionException,
    									    	   SourceException
	{
		TunerChannelSource source = null;
		
		try
		{
			source = getController().getChannel( this, channel );

			if( source != null )
			{
				broadcast( new TunerEvent( this, TunerEvent.Event.CHANNEL_COUNT ) );
			}
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
			getController().releaseChannel( source );
		}
    }

	@Override
	public void addListener( Listener<ComplexBuffer> listener )
	{
		getController().addListener( listener );
	}
	
	@Override
	public void removeListener( Listener<ComplexBuffer> listener )
	{
		getController().removeListener( listener );
	}
}
