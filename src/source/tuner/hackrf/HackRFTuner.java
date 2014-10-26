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
package source.tuner.hackrf;

import java.util.concurrent.RejectedExecutionException;

import javax.swing.JPanel;
import javax.usb.UsbException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.complex.ComplexBuffer;
import source.SourceException;
import source.tuner.Tuner;
import source.tuner.TunerChannel;
import source.tuner.TunerChannelSource;
import source.tuner.TunerClass;
import source.tuner.TunerConfiguration;
import source.tuner.TunerType;
import source.tuner.hackrf.HackRFTunerController.BoardID;
import source.tuner.hackrf.HackRFTunerController.Serial;
import controller.ResourceManager;
import controller.ThreadPoolManager;

public class HackRFTuner extends Tuner
{
	private final static Logger mLog = LoggerFactory.getLogger( HackRFTuner.class );

	private HackRFTunerController mController;
	
	public HackRFTuner( HackRFTunerController controller ) throws SourceException
	{
		super( "HackRF" );
		
		mController = controller;
		
		/* Register for frequency/sample rate changes */
		mController.addListener( this );
		
		try
		{
			BoardID id = mController.getBoardID();
			super.setName( id.getLabel() );
			
			String version = mController.getFirmwareVersion();
			mLog.info( "HackRF firmware: " + version );
			
			Serial serial = mController.getSerial();
			mLog.info( "HackRF Part ID: " + serial.getPartID() );
			mLog.info( "HackRF Serial Number: " + serial.getSerialNumber() );
		}
		catch( UsbException e )
		{
			mLog.error( "couldn't read HackRF board id", e  );
		}
	}

	public void dispose()
	{
		//TODO: dispose of something here
	}
	
	public HackRFTunerController getController()
	{
		return mController;
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
    public JPanel getEditor( ResourceManager resourceManager )
    {
	    return new HackRFTunerEditorPanel( this, resourceManager );
    }

	@Override
    public void apply( TunerConfiguration config ) throws SourceException
    {
		mController.apply( config );
    }

	@Override
    public int getSampleRate()
    {
	    return mController.getSampleRate();
    }

	@Override
    public long getFrequency() throws SourceException
    {
	    return mController.getFrequency();
    }

	@Override
    public TunerChannelSource getChannel( ThreadPoolManager threadPoolManager,
		TunerChannel channel ) throws RejectedExecutionException, SourceException
    {
	    return mController.getChannel( threadPoolManager, this, channel );
    }

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
    public String getUniqueID()
    {
		try
		{
			return mController.getSerial().getSerialNumber();
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
		mController.addListener( listener );
	}
	
	@Override
	public void removeListener( Listener<ComplexBuffer> listener )
	{
		mController.removeListener( listener );
	}
}
