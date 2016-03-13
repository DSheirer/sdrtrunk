/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2015 Dennis Sheirer
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
package source.tuner.airspy;

import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.complex.ComplexBuffer;
import source.SourceException;
import source.tuner.Tuner;
import source.tuner.TunerChannel;
import source.tuner.TunerChannelSource;
import source.tuner.TunerClass;
import source.tuner.TunerEvent;
import source.tuner.TunerType;
import source.tuner.airspy.AirspyTunerController.BoardID;
import source.tuner.configuration.TunerConfiguration;

public class AirspyTuner extends Tuner
{
	private final static Logger mLog = LoggerFactory.getLogger( AirspyTuner.class );

	public AirspyTuner( AirspyTunerController controller )
	{
		super( "Airspy", controller );
	}
	
	public AirspyTunerController getController()
	{
		return (AirspyTunerController)getTunerController();
	}

	@Override
	public void apply( TunerConfiguration config ) throws SourceException
	{
		getController().apply( config );
	}

	@Override
    public String getUniqueID()
    {
		try
		{
			return getController().getDeviceInfo().getSerialNumber();
		}
		catch( Exception e )
		{
			mLog.error( "error gettting serial number", e );
		}
		
		return BoardID.AIRSPY.getLabel();
    }

	@Override
	public TunerClass getTunerClass()
	{
		return TunerClass.AIRSPY;
	}

	@Override
	public TunerType getTunerType()
	{
		return TunerType.AIRSPY_R820T;
	}

	@Override
	public int getSampleRate()
	{
		int rate = 0;
		
		try
		{
			rate = getController().getCurrentSampleRate();
		} 
		catch ( SourceException e )
		{
			mLog.error( "Error while getting sample rate from controller", e );
		}
		
		return rate;
	}
	
	@Override
	public double getSampleSize()
	{
		return 13.0;
	}

	@Override
	public long getFrequency() throws SourceException
	{
		return getController().getFrequency();
	}

	@Override
    public TunerChannelSource getChannel( TunerChannel channel ) 
    		throws RejectedExecutionException, SourceException
    {
		//TODO: this channel has a decimated sample rate of:
		// 10.0 MSps = 10,000,000 / 208 = 48076.923
		//  5.0 MSps =  5,000,000 / 104 = 48076.923
		//  2.5 MSps =  2,500,000 /  52 = 48076.923
		//Consider implementing a fractional resampler to get a correct 48 kHz
		//output sample rate
		
		TunerChannelSource source = getController().getChannel( this, channel );

		if( source != null )
		{
			broadcast( new TunerEvent( this, TunerEvent.Event.CHANNEL_COUNT ) );
		}
		
		return source;
    }
	
	

	@Override
	public int getChannelCount()
	{
		return getController().getChannelCount();
	}

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
