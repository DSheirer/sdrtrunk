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
package source.tuner.hackrf;

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
import source.tuner.configuration.TunerConfiguration;
import source.tuner.hackrf.HackRFTunerController.BoardID;

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
		
		/* Tell the controller to release the channel and cleanup */
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
