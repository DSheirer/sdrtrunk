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
package source.tuner.fcd;

import javax.swing.JPanel;
import javax.usb.UsbClaimException;
import javax.usb.UsbConfiguration;
import javax.usb.UsbDevice;
import javax.usb.UsbEndpoint;
import javax.usb.UsbException;
import javax.usb.UsbInterface;
import javax.usb.UsbInterfacePolicy;
import javax.usb.UsbPipe;

import log.Log;
import source.SourceException;
import source.tuner.TunerClass;
import source.tuner.TunerConfiguration;
import source.tuner.TunerController;
import source.tuner.TunerType;
import source.tuner.fcd.proV1.FCD1TunerController.Block;
import source.tuner.usb.USBTunerDevice;
import controller.ResourceManager;

public abstract class FCDTunerController extends TunerController
{
	public final static int sINT_NULL_VALUE = -1;
	public final static long sLONG_NULL_VALUE = -1l;
	public final static double sDOUBLE_NULL_VALUE = -1.0D;
	
	public final static byte sFCD_INTERFACE = (byte)0x2;
	public final static byte sFCD_ENDPOINT_IN = (byte)0x82;
	public final static byte sFCD_ENDPOINT_OUT = (byte)0x2;
	
	public final static boolean sFCD_FORCE_CLAIM_HID_INTERFACE = true;
	
	private UsbDevice mFuncube;
	private UsbConfiguration mUsbConfiguration;
	private UsbInterface mFuncubeHID;
	private UsbPipe mPipeIn;
	private UsbPipe mPipeOut;
	private FCDConfiguration mConfiguration = new FCDConfiguration();

	public FCDTunerController( USBTunerDevice device, 
							   int minTunableFrequency,
							   int maxTunableFrequency ) throws SourceException
	{
		super( minTunableFrequency, maxTunableFrequency );

		mFuncube = device.getDevice();
		mUsbConfiguration = mFuncube.getActiveUsbConfiguration();
		mFuncubeHID = mUsbConfiguration.getUsbInterface( sFCD_INTERFACE );
	}
	
	public abstract int getCurrentSampleRate() throws SourceException;
	
	/**
	 * Returns the tuner class.  Since this is a generic container for both
	 * types of funcube, we let the controller self identify the class and type
	 * 
	 * @return - tuner class
	 */
	public abstract TunerClass getTunerClass();

	/**
	 * Returns the tuner type.  Since this is a generic container for both
	 * types of funcube, we let the controller self identify the class and type
	 * 
	 * @return - tuner type
	 */
	public abstract TunerType getTunerType();

	/**
	 * FCD editor panel
	 */
	public abstract JPanel getEditor( FCDTuner tuner, 
				ResourceManager resourceManager );

	/**
	 * Applies the settings in the tuner configuration
	 */
	public abstract void apply( TunerConfiguration config ) 
						throws SourceException;

	/**
	 * Returns the applied tuner configuration or null if one hasn't yet been
	 * applied
	 */
	public abstract TunerConfiguration getTunerConfiguration();

	/**
	 * USB address
	 */
	public String getAddress()
	{
	    return mFuncube.toString();
	}
	
	/**
	 * Set fcd interface mode
	 */
	public void setFCDMode( Mode mode ) throws UsbException, UsbClaimException
	{
		byte[] response = null;
		
		switch( mode )
		{
			case APPLICATION:
				response = send( FCDCommand.BL_QUERY.getRequestTemplate(), 
								 FCDCommand.BL_QUERY.getResponseTemplate() );
				break;
			case BOOTLOADER:
				response = send( FCDCommand.APP_RESET.getRequestTemplate(), 
								 FCDCommand.APP_RESET.getResponseTemplate() );
				break;
			case ERROR:
			case UNKNOWN:
				break;
		}
		
		if( response != null )
		{
			mConfiguration.set( response );
		}
		else
		{
			mConfiguration.setModeUnknown();
		}
	}

	/**
	 * Sets the actual (uncorrected) device frequency
	 */
	public void setTunedFrequency( long frequency ) throws SourceException
	{
		try
		{
			send( FCDCommand.APP_SET_FREQUENCY_HZ, frequency );
		}
		catch( Exception e )
		{
			throw new SourceException( "Couldn't set FCD Local " +
					"Oscillator Frequency [" + frequency + "]", e );
		}
	}

	/**
	 * Gets the actual (uncorrected) device frequency
	 */
	public long getTunedFrequency() throws SourceException
	{
		try
		{
			return send( FCDCommand.APP_GET_FREQUENCY_HZ );
		}
		catch( Exception e )
		{
			throw new SourceException( "FCDTunerController - "
					+ "couldn't get LO frequency", e );
		}
	}
	
	/**
	 * Returns the FCD device configuration
	 */
	public FCDConfiguration getConfiguration()
	{
		return mConfiguration;
	}

	/**
	 * Claims the FCD USB HID interface.  If another application currently has
	 * the interface claimed, the sFCD_FORCE_CLAIM_HID_INTERFACE setting
	 * will dictate if the interface is forcibly claimed from the other 
	 * application
	 */
	private boolean claim() throws UsbException, UsbClaimException
	{
		boolean claimed = false;
		
		if( !mFuncubeHID.isClaimed() )
		{
			mFuncubeHID.claim( new UsbInterfacePolicy() 
			{
				@Override
                public boolean forceClaim( UsbInterface arg0 )
                {
                    return sFCD_FORCE_CLAIM_HID_INTERFACE;
                }
			} );
			
			claimed = true;
		}
		else
		{
			Log.info( "Attempted to claim the Funcube HID interface, but it's in use by another program" );
		}
		
		return claimed;
	}
	
	/**
	 * Releases the claimed USB (FCD) interface
	 */
	private void release()
	{
		mPipeIn = null;
		mPipeOut = null;
		
		try
        {
	        mFuncubeHID.release();
        }
        catch ( Exception e )
        {
	        Log.error( "Exception thrown while releasing the funcube HID interface" + e.getLocalizedMessage() );
        }
	}

	/**
	 * Convenience wrapper for commands that don't take arguments 
	 */
	public long send( FCDCommand command ) throws UsbException, UsbClaimException
	{
		return send( command, sLONG_NULL_VALUE );
	}

	/**
	 * Sends the command, optionally placing the argument value parsed into 
	 * little endian bytes in the request array.  Returns any integer value that
	 * part of the response, or sINT_NULL_VALUE (-1) if it fails.
	 * 
	 * Note: we use a long return value to avoid any issues with the MSB of a
	 * 32-bit response value being misinterpreted as the integer sign bit.
	 * 
	 * @param command
	 * @param commandArgument - command argument.  Use -1 for no-argument commands
	 * @return - response long value, or -1 if fails
	 * @throws UsbException
	 * @throws UsbClaimException
	 */
	public long send( FCDCommand command, long commandArgument ) throws UsbException,
															UsbClaimException
	{
		long retVal = sLONG_NULL_VALUE;
		
		byte[] request = command.getRequestTemplate();

		if( commandArgument != sLONG_NULL_VALUE )
		{
			//Parse the value argument into the request byte indices, according to command length
			for( int x = 1; x < command.getArrayLength(); x++ )
			{
				//Little endian
				request[ x ] = (byte)( 0xFF & 
						Long.rotateRight( commandArgument, ( x - 1 ) * 8 ) ); 
			}
		}

		byte[] response = send( request, command.getResponseTemplate() );

		if( response != null )
		{
			retVal = parseResponse( command, response );
		}
		
		return retVal;
	}
	
	/**
	 * Sends the request byte array to the device and returns the response array 
	 * filled with the response bytes from the device, or returns null if 
	 * something failed along the way
	 */
	private byte[] send( byte[] request, byte[] response ) 
			throws UsbException, UsbClaimException
	{
		byte[] retVal = null;
		
		if( mPipeIn != null && mPipeOut != null )
		{
			mPipeOut.syncSubmit( request );

			mPipeIn.syncSubmit( response );
			
			retVal = response;
		}
		
		return retVal;
	}
	
	/**
	 * Parses a multi-byte integer response value from a byte array that is
	 * 2 or more bytes long
	 */
	private long parseResponse( FCDCommand command, byte[] response )
	{
		long retVal = 0;

		//Parse the value argument into the request byte indices, according to command length
		for( int x = 2; x < command.getArrayLength(); x++ )
		{
			retVal += Long.rotateLeft( ( response[ x ] & 0xFF ), ( x - 2 ) * 8 );
		}

		return retVal;
	}
	
	protected void open() throws UsbException, UsbClaimException
	{
		if( claim() )
		{
			//Get the USB endpoints
			UsbEndpoint endpointIn = 
					mFuncubeHID.getUsbEndpoint( sFCD_ENDPOINT_IN );

			UsbEndpoint endpointOut = 
					mFuncubeHID.getUsbEndpoint( sFCD_ENDPOINT_OUT );

			//Setup the pipes
			if( endpointIn != null && endpointOut != null )
			{
				mPipeOut = endpointOut.getUsbPipe();
				mPipeOut.open();

				mPipeIn = endpointIn.getUsbPipe();
				mPipeIn.open();
				
				setFCDMode( Mode.APPLICATION );
			}
			else
			{
				Log.error( "FCD Pro Tuner Controller - couldn't gain access " +
						"to the USB in/out endpoints or pipes." );
			}
		}
		else
		{
			Log.error( "FCD Pro Tuner Controller - couldn't claim the USB " +
				"interface.  Check to see if another program is using it." );
		}
	}
	
    protected void close()
	{
		release();
	}

	public class FCDConfiguration
	{
		private String mConfig;
		private Mode mMode;

		public FCDConfiguration()
		{
			mConfig = null;
			mMode = Mode.UNKNOWN;
		}
		
		private void setModeUnknown()
		{
			mConfig = null;
			mMode = Mode.UNKNOWN;
		}
		
		public void set( byte[] data )
		{
			if( FCDCommand.checkResponse( FCDCommand.BL_QUERY, data ) || 
					FCDCommand.checkResponse( FCDCommand.APP_RESET, data ) )
			{
				mConfig = new String( data );
				mMode = Mode.getMode( mConfig );
			}
			else
			{
				mConfig = null;
				mMode = Mode.ERROR;
			}
		}
		
		public Mode getMode()
		{
			return mMode;
		}
		
        public FCDModel getModel()
		{
			FCDModel retVal = FCDModel.FUNCUBE_UNKNOWN;
			
			switch( mMode )
			{
				case APPLICATION:
					retVal = FCDModel.getFCD( mConfig.substring( 15, 22 ) );
					break;
				case BOOTLOADER:
				case UNKNOWN:
				case ERROR:
					break;
			}
			
			return retVal;
		}
		
		public Block getBandBlocking()
		{
			Block retVal = Block.UNKNOWN;
			
			switch( mMode )
			{
				case APPLICATION:
					retVal = Block.getBlock( mConfig.substring( 23, 33 ).trim() );
					break;
				case BOOTLOADER:
				case UNKNOWN:
				case ERROR:
				break;
			}
			
			return retVal;
		}

		public String getFirmware()
		{
			String retVal = null;
			
			switch( mMode )
			{
				case APPLICATION:
					retVal = mConfig.substring( 9, 14 );
					break;
				case BOOTLOADER:
				case UNKNOWN:
				case ERROR:
					break;
			}

			return retVal;
		}

		public String toString()
		{
			return getModel().getLabel();
		}
	}
	
	public enum Mode 
	{ 
		APPLICATION,
		BOOTLOADER,
		ERROR,
		UNKNOWN;

		public static Mode getMode( String config )
		{
			Mode retVal = UNKNOWN;

			if( config == null )
			{
				retVal = ERROR;
			}
			else
			{
				if( config.length() >= 8 )
				{
					String mode = config.substring( 2, 8 ).trim();

					if( mode.equalsIgnoreCase( "FCDAPP" ) )
					{
						retVal = APPLICATION;
					}
					else if( mode.equalsIgnoreCase( "FCDBL" ) )
					{
						retVal = BOOTLOADER;
					}
				}
			}
			
			return retVal;
		}
	}
}
