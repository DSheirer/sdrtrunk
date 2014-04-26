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

import javax.swing.JPanel;
import javax.usb.UsbClaimException;
import javax.usb.UsbException;

import log.Log;
import source.SourceException;
import source.tuner.TunerClass;
import source.tuner.TunerConfiguration;
import source.tuner.TunerType;
import source.tuner.fcd.FCDCommand;
import source.tuner.fcd.FCDTuner;
import source.tuner.fcd.FCDTunerController;
import source.tuner.usb.USBTunerDevice;
import controller.ResourceManager;

public class FCD2TunerController extends FCDTunerController
{
	public static final int sMINIMUM_TUNABLE_FREQUENCY = 150000;
	public static final int sMAXIMUM_TUNABLE_FREQUENCY = 2050000000;
	public static final int sSAMPLE_RATE = 192000;
	
	private FCD2TunerConfiguration mTunerConfiguration;
	private FCD2TunerEditorPanel mEditor;
	
	public FCD2TunerController( USBTunerDevice device ) 
			throws SourceException 
	{
		super( device,
			   sMINIMUM_TUNABLE_FREQUENCY, 
			   sMAXIMUM_TUNABLE_FREQUENCY );

		mFrequencyController.setBandwidth( sSAMPLE_RATE );

		try
		{
			open();
			
			setFCDMode( Mode.APPLICATION );
		}
		catch( Exception e )
		{
			throw new SourceException( "FCDTunerController error " +
					"during construction", e );
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

	public JPanel getEditor( FCDTuner tuner, ResourceManager resourceManager )
	{
		if( mEditor == null )
		{
			mEditor = new FCD2TunerEditorPanel( tuner, resourceManager );
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
            catch ( UsbClaimException e )
            {
            	throw new SourceException( "Exception while setting LNA Gain", e );
            }
            catch ( UsbException e )
            {
            	throw new SourceException( "Exception while setting LNA Gain", e );
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
            catch ( UsbClaimException e )
            {
            	throw new SourceException( "Exception while setting mixer gain", e );
            }
            catch ( UsbException e )
            {
            	throw new SourceException( "Exception while setting mixer gain", e );
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
	        dcCorrection = (int)send( FCDCommand.APP_GET_DC_CORRECTION );
        }
        catch ( UsbClaimException e )
        {
        	Log.error( "FCTTunerController - couldn't claim funcube HID to"
        			+ " get dc correction value" );
        }
        catch ( UsbException e )
        {
        	Log.error( "FCTTunerController - error getting dc correction "
        			+ "value - " + e.getLocalizedMessage() );
	        
        }
		
		return dcCorrection;
	}
	
	public void setDCCorrection( int value )
	{
		try
        {
	        send( FCDCommand.APP_SET_DC_CORRECTION, value );
        }
        catch ( UsbClaimException e )
        {
	        Log.error( "FCDTunerController - error claiming usb hid while "
	        		+ "trying to set dc correction to [" + value + "]" );
        }
        catch ( UsbException e )
        {
	        Log.error( "FCDTunerController - error while "
	        		+ "trying to set dc correction to [" + value + "] - " +
	        		e.getLocalizedMessage() );
        }
	}
	
	public int getIQCorrection()
	{
		int iqCorrection = -999;
		
		try
        {
	        iqCorrection = (int)send( FCDCommand.APP_GET_IQ_CORRECTION );
        }
        catch ( UsbClaimException e )
        {
        	Log.error( "FCTTunerController - couldn't claim funcube HID to"
        			+ " get iq correction value" );
        }
        catch ( UsbException e )
        {
        	Log.error( "FCTTunerController - error getting iq correction "
        			+ "value - " + e.getLocalizedMessage() );
	        
        }
		
		return iqCorrection;
	}
	
	public void setIQCorrection( int value )
	{
		try
        {
	        send( FCDCommand.APP_SET_IQ_CORRECTION, value );
        }
        catch ( UsbClaimException e )
        {
	        Log.error( "FCDTunerController - error claiming usb hid while "
	        		+ "trying to set iq correction to [" + value + "]" );
        }
        catch ( UsbException e )
        {
	        Log.error( "FCDTunerController - error while "
	        		+ "trying to set iq correction to [" + value + "] - " +
	        		e.getLocalizedMessage() );
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

	public enum TunerRFFilter
	{
		TRF_0_4,
		TRF_4_8,
		TRF_8_16,
		TRF_16_32,
		TRF_32_75,
		TRF_75_125,
		TRF_125_250,
		TRF_145,
		TRF_410_875,
		TRF_435,
		TRF_875_2000,
		UNKNOWN;
		
		public static TunerRFFilter get( int value )
		{
			TunerRFFilter retVal = UNKNOWN;
			
			if( 0 <= value && value <= 10 )
			{
				retVal = values()[ value ];
			}
			
			return retVal;
		}
	}

	public enum TunerIFFilter
	{
		TIF_200KHZ,
		TIF_300KHZ,
		TIF_600KHZ,
		TIF_1536KHZ,
		TIF_5MHZ,
		TIF_6MHZ,
		TIF_7MHZ,
		TIF_8MHZ,
		UNKNOWN;
		
		public static TunerIFFilter get( int value )
		{
			TunerIFFilter retVal = UNKNOWN;
			
			if( 0 <= value && value <= 7 )
			{
				retVal = values()[ value ];
			}
			
			return retVal;
		}
	}
	
    public enum LNAGain 
    {
    	LNA_GAIN_MINUS_5_0( 0, "-5.0 dB" ),
    	LNA_GAIN_MINUS_2_5( 1, "-2.5 dB" ),
    	LNA_GAIN_PLUS_0_0( 4, "0.0 dB" ),
    	LNA_GAIN_PLUS_2_5( 5, "2.5 dB" ),
    	LNA_GAIN_PLUS_5_0( 6, "5.0 dB" ),
    	LNA_GAIN_PLUS_7_5( 7, "7.5 dB" ),
    	LNA_GAIN_PLUS_10_0( 8, "10.0 dB" ),
    	LNA_GAIN_PLUS_12_5( 9, "12.5 dB" ),
    	LNA_GAIN_PLUS_15_0( 10, "15.0 dB" ),
    	LNA_GAIN_PLUS_17_5( 11, "17.5 dB" ),
    	LNA_GAIN_PLUS_20_0( 12, "20.0 dB" ),
    	LNA_GAIN_PLUS_25_0( 13, "25.0 dB" ),
    	LNA_GAIN_PLUS_30_0( 14, "30.0 dB" );
    	
    	private int mSetting;
    	private String mLabel;
    	
    	private LNAGain( int setting, String label )
    	{
    		mSetting = setting;
    		mLabel = label;
    	}
    	
    	public int getSetting()
    	{
    		return mSetting;
    	}
    	
    	public String getLabel()
    	{
    		return mLabel;
    	}
    	
    	public String toString()
    	{
    		return getLabel();
    	}
    }
}
