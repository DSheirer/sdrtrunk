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

import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import source.tuner.airspy.AirspyTunerController.BoardID;
import controller.ThreadPoolManager;

public class AirspyTuner extends Tuner
{
	private final static Logger mLog = LoggerFactory.getLogger( AirspyTuner.class );

	AirspyTunerController mController;
	
	public AirspyTuner( AirspyTunerController controller )
	{
		super( "Airspy" );
		
		mController = controller;
		
		/* Register for frequency/sample rate changes so that we can rebroadcast
		 * to any registered listeners */
		mController.addListener( this );
	}
	
	@Override
	public JPanel getEditor( SettingsManager settingsManager )
	{
		return new AirspyTunerEditorPanel( this, settingsManager );
	}
	
	public AirspyTunerController getController()
	{
		return mController;
	}

	@Override
	public void apply( TunerConfiguration config ) throws SourceException
	{
		mController.apply( config );
	}

	@Override
    public String getUniqueID()
    {
		try
		{
			return mController.getDeviceInfo().getSerialNumber();
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
			rate = mController.getCurrentSampleRate();
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
		return mController.getFrequency();
	}

	@Override
    public TunerChannelSource getChannel( ThreadPoolManager threadPoolManager,
		TunerChannel channel ) throws RejectedExecutionException, SourceException
    {
		//TODO: this channel has a decimated sample rate of:
		// 10.0 MSps = 10,000,000 / 208 = 48076.923
		//  5.0 MSps =  5,000,000 / 104 = 48076.923
		//  2.5 MSps =  2,500,000 /  52 = 48076.923
		//Consider implementing a fractional resampler to get a correct 48 kHz
		//output sample rate
		
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
