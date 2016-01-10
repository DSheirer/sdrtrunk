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
package source.tuner.fcd.proplusV2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;

import settings.SettingsManager;
import source.SourceException;
import source.tuner.TunerClass;
import source.tuner.TunerConfiguration;
import source.tuner.TunerType;
import source.tuner.fcd.FCDCommand;
import source.tuner.fcd.FCDTuner;
import source.tuner.fcd.FCDTunerController;

public class FCD2TunerController extends FCDTunerController
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( FCD2TunerController.class );

	public static final int sMINIMUM_TUNABLE_FREQUENCY = 150000;
	public static final int sMAXIMUM_TUNABLE_FREQUENCY = 2050000000;
	public static final int sSAMPLE_RATE = 192000;
	
	private FCD2TunerConfiguration mTunerConfiguration;
	private FCD2TunerEditorPanel mEditor;
	
	public FCD2TunerController( Device device, DeviceDescriptor descriptor ) 
	{
		super( device,
			   descriptor,
			   sMINIMUM_TUNABLE_FREQUENCY, 
			   sMAXIMUM_TUNABLE_FREQUENCY );
	}

	public void init() throws SourceException
	{
		super.init();

		mFrequencyController.setSampleRate( sSAMPLE_RATE );

		try
		{
			setFCDMode( Mode.APPLICATION );
		}
		catch( Exception e )
		{
			throw new SourceException( "error setting Mode to APPLICATION", e );
		}
	}
	
	public int getCurrentSampleRate()
	{
		return sSAMPLE_RATE;
	}

	@Override
    public TunerClass getTunerClass()
    {
	    return TunerClass.FUNCUBE_DONGLE_PRO_PLUS;
    }

	@Override
    public TunerType getTunerType()
    {
	    return TunerType.FUNCUBE_DONGLE_PRO_PLUS;
    }

	public JPanel getEditor( FCDTuner tuner, SettingsManager settingsManager )
	{
		if( mEditor == null )
		{
			mEditor = new FCD2TunerEditorPanel( tuner, settingsManager );
		}
		
		return mEditor;
	}
	
	@Override
    public void apply( TunerConfiguration config ) throws SourceException
    {
		if( config instanceof FCD2TunerConfiguration )
		{
			FCD2TunerConfiguration plusConfig = 
								(FCD2TunerConfiguration)config;

			setFrequencyCorrection( plusConfig.getFrequencyCorrection() );

			try
            {
                if( plusConfig.getGainLNA() )
    			{
                	send( FCDCommand.APP_SET_LNA_GAIN, 1 );

    			}
    			else
    			{
    				send( FCDCommand.APP_SET_LNA_GAIN, 0 );
    			}
            }
            catch ( Exception e )
            {
            	throw new SourceException( "error while setting LNA Gain", e );
            }

            try
            {
                if( plusConfig.getGainMixer() )
    			{
                	send( FCDCommand.APP_SET_MIXER_GAIN, 1 );
                    
    			}
    			else
    			{
    				send( FCDCommand.APP_SET_MIXER_GAIN, 0 );
    			}
            }
            catch ( Exception e )
            {
            	throw new SourceException( "error while setting Mixer Gain", e );
            }
            
            mTunerConfiguration = plusConfig;
		}
    }
	
	@Override
    public TunerConfiguration getTunerConfiguration()
    {
	    return mTunerConfiguration;
    }

	public int getDCCorrection()
	{
		int dcCorrection = -999;
		
		try
        {
			ByteBuffer buffer = send( FCDCommand.APP_GET_DC_CORRECTION );
			
			buffer.order( ByteOrder.LITTLE_ENDIAN );
			
			return buffer.getInt( 2 );
        }
        catch ( Exception e )
        {
        	mLog.error( "error getting dc correction value", e );
        }
		
		return dcCorrection;
	}
	
	public void setDCCorrection( int value )
	{
		try
        {
			send( FCDCommand.APP_SET_DC_CORRECTION, value );
        }
        catch ( Exception e )
        {
        	mLog.error( "error setting dc correction to [" + value + "]", e );
        }
	}
	
	public int getIQCorrection()
	{
		int iqCorrection = -999;
		
		try
        {
			ByteBuffer buffer = send( FCDCommand.APP_GET_IQ_CORRECTION );
			
			buffer.order( ByteOrder.LITTLE_ENDIAN );
			
			return buffer.getInt( 2 );
        }
        catch ( Exception e )
        {
        	mLog.error( "error reading IQ correction value", e );
        }
	        
		return iqCorrection;
	}
	
	public void setIQCorrection( int value )
	{
		try
        {
	        send( FCDCommand.APP_SET_IQ_CORRECTION, value );
        }
        catch ( Exception e )
        {
        	mLog.error( "error setting IQ correction to [" + value + "]", e );
        }
	}
	
	public enum Block 
	{ 
		CELLULAR_BAND_BLOCKED( "Blocked" ),
		NO_BAND_BLOCK( "Unblocked" ),
		UNKNOWN( "Unknown" );
		
		private String mLabel;
		
		private Block( String label )
		{
		    mLabel = label;
		}
		
		public String getLabel()
		{
		    return mLabel;
		}

		public static Block getBlock( String block )
		{
			Block retVal = UNKNOWN;

			if( block.equalsIgnoreCase( "No blk" ) )
			{
				retVal = NO_BAND_BLOCK;
			}
			else if( block.equalsIgnoreCase( "Cell blk" ) )
			{
				retVal = CELLULAR_BAND_BLOCKED;
			}
			
			return retVal;
		}
	}
}
