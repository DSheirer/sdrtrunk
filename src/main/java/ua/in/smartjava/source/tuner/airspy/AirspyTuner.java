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
package ua.in.smartjava.source.tuner.airspy;

import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.sample.complex.ComplexBuffer;
import ua.in.smartjava.source.SourceException;
import ua.in.smartjava.source.tuner.Tuner;
import ua.in.smartjava.source.tuner.TunerChannel;
import ua.in.smartjava.source.tuner.TunerChannelSource;
import ua.in.smartjava.source.tuner.TunerClass;
import ua.in.smartjava.source.tuner.TunerEvent;
import ua.in.smartjava.source.tuner.TunerType;
import ua.in.smartjava.source.tuner.airspy.AirspyTunerController.BoardID;

public class AirspyTuner extends Tuner
{
	private final static Logger mLog = LoggerFactory.getLogger( AirspyTuner.class );

	public AirspyTuner( AirspyTunerController controller )
	{
		super( "Airspy " + controller.getDeviceInfo().getSerialNumber(), controller );
	}
	
	public AirspyTunerController getController()
	{
		return (AirspyTunerController)getTunerController();
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
			mLog.error( "error getting serial number", e );
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
	public double getSampleSize()
	{
		return 13.0;
	}

	@Override
    public TunerChannelSource getChannel( TunerChannel channel ) 
    		throws RejectedExecutionException, SourceException
    {
		//TODO: this ua.in.smartjava.channel has a decimated ua.in.smartjava.sample rate of:
		// 10.0 MSps = 10,000,000 / 208 = 48076.923
		//  5.0 MSps =  5,000,000 / 104 = 48076.923
		//  2.5 MSps =  2,500,000 /  52 = 48076.923
		//Consider implementing a fractional resampler to get a correct 48 kHz
		//output ua.in.smartjava.sample rate
		
		TunerChannelSource source = getController().getChannel( this, channel );

		if( source != null )
		{
			broadcast( new TunerEvent( this, TunerEvent.Event.CHANNEL_COUNT ) );
		}
		
		return source;
    }
	
	@Override
    public void releaseChannel( TunerChannelSource source )
    {
		/* Unregister for receiving samples */
		removeListener( (Listener<ComplexBuffer>)source );
		
		/* Tell the ua.in.smartjava.controller to release the ua.in.smartjava.channel and cleanup */
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
