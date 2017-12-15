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
package ua.in.smartjava.source.tuner.hackrf;

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
import ua.in.smartjava.source.tuner.hackrf.HackRFTunerController.BoardID;

public class HackRFTuner extends Tuner
{
	private final static Logger mLog = LoggerFactory.getLogger( HackRFTuner.class );

	public HackRFTuner( HackRFTunerController controller ) throws SourceException
	{
		super( "HackRF", controller );
	}
	
	public HackRFTunerController getController()
	{
		return (HackRFTunerController)getTunerController();
	}

	public void dispose()
	{
		//TODO: dispose of something here
	}
	
	@Override
    public TunerClass getTunerClass()
    {
	    return TunerClass.HACKRF_ONE;
    }

	@Override
    public TunerType getTunerType()
    {
	    return TunerType.HACKRF;
    }

	@Override
	public double getSampleSize()
	{
		return 11.0;
	}

	@Override
    public TunerChannelSource getChannel( TunerChannel channel ) 
    		throws RejectedExecutionException, SourceException
    {
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
    public String getUniqueID()
    {
		try
		{
			return getController().getSerial().getSerialNumber();
		}
		catch( Exception e )
		{
			mLog.error( "error gettting serial number", e );
		}
		
		return BoardID.HACKRF_ONE.getLabel();
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
