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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.swing.JPanel;
import javax.usb.UsbClaimException;
import javax.usb.UsbException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import source.SourceException;
import source.tuner.TunerClass;
import source.tuner.TunerConfiguration;
import source.tuner.TunerController;
import source.tuner.TunerType;
import source.tuner.fcd.proV1.FCD1TunerController.Block;
import controller.ResourceManager;

public abstract class FCDTunerController extends TunerController
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( FCDTunerController.class );

	public final static byte FCD_INTERFACE = (byte)0x2;
	public final static byte FCD_ENDPOINT_IN = (byte)0x82;
	public final static byte FCD_ENDPOINT_OUT = (byte)0x2;
	
	private Device mDevice;
	private DeviceDescriptor mDeviceDescriptor;
	private DeviceHandle mDeviceHandle;
	
	private FCDConfiguration mConfiguration = new FCDConfiguration();
	
	/**
	 * Generic FCD tuner controller - contains functionality common across both
	 * funcube dongle tuners 
	 * 
	 * @param device
	 * @param descriptor
	 * @param minTunableFrequency
	 * @param maxTunableFrequency
	 */
	public FCDTunerController( Device device, 
							   DeviceDescriptor descriptor,
							   int minTunableFrequency,
							   int maxTunableFrequency )
	{
		super( minTunableFrequency, maxTunableFrequency );

		mDevice = device;
		mDeviceDescriptor = descriptor;
	}

	/**
	 * Initializes the controller by opening the USB device and claiming the
	 * HID interface.
	 * 
	 * Invoke this method after constructing this class to setup the
	 * controller.
	 * 
	 * @throws SourceException if cannot open and claim the USB device
	 */
	public void init() throws SourceException
	{
		mDeviceHandle = new DeviceHandle();
		
		int result = LibUsb.open( mDevice, mDeviceHandle );

		if( result != LibUsb.SUCCESS )
		{
			mDeviceHandle = null;
			
			throw new SourceException( "libusb couldn't open funcube usb "
					+ "device [" + LibUsb.errorName( result ) + "]" );
		}
		
		claimInterface();
	}

	/**
	 * Disposes of resources.  Closes the USB device and interface.
	 */
	public void dispose()
	{
		if( mDeviceHandle != null )
		{
			try
			{
				LibUsb.close( mDeviceHandle );
			}
			catch( Exception e )
			{
				mLog.error( "error while closing device handle", e );
			}
			
			mDeviceHandle = null;
		}
		
		mDeviceDescriptor = null;
		mDevice = null;
	}

	/**
	 * Sample rate of the tuner
	 */
	public abstract int getCurrentSampleRate() throws SourceException;
	
	/**
	 * Tuner class
	 */
	public abstract TunerClass getTunerClass();

	/**
	 * Tuner type
	 */
	public abstract TunerType getTunerType();

	/**
	 * Editor panel (GUI) component.
	 */
	public abstract JPanel getEditor( FCDTuner tuner, 
								ResourceManager resourceManager );

	/**
	 * Applies the settings in the tuner configuration
	 */
	public abstract void apply( TunerConfiguration config ) 
						throws SourceException;

	/**
	 * Returns the currently applied tuner configuration or null if one hasn't 
	 * yet been applied
	 */
	public abstract TunerConfiguration getTunerConfiguration();

	/**
	 * USB address (bus/port)
	 */
	public String getUSBAddress()
	{
		if( mDevice != null )
		{
			StringBuilder sb = new StringBuilder();
			
			sb.append( "Bus:" );
			int bus = LibUsb.getBusNumber( mDevice );
			sb.append( bus );
			
			sb.append( " Port:" );
			int port = LibUsb.getPortNumber( mDevice );
			sb.append( port );
			
			return sb.toString();
		}

		return "UNKNOWN";
	}

	/**
	 * USB Vendor and Product ID 
	 */
	public String getUSBID()
	{
		if( mDeviceDescriptor != null )
		{
			StringBuilder sb = new StringBuilder();
			
			sb.append( String.format( "%04X", 
					(int)( mDeviceDescriptor.idVendor() & 0xFFFF ) ) );

			sb.append( ":" );
			
			sb.append( String.format( "%04X", 
					(int)( mDeviceDescriptor.idProduct() & 0xFFFF ) ) );

			return sb.toString();
		}

		return "UNKNOWN";
	}

	/**
	 * USB Port Speed.  Should be 2.0 for both types of funcube dongles
	 */
	public String getUSBSpeed()
	{
		if( mDevice != null )
		{
			int speed = LibUsb.getDeviceSpeed( mDevice );

			switch( speed )
			{
				case 0:
					return "1.1 LOW";
				case 1:
					return "1.1 FULL";
				case 2:
					return "2.0 HIGH";
				case 3:
					return "3.0 SUPER";
				default:
			}
		}
		
		return "UNKNOWN";
	}
	
	/**
	 * Set fcd interface mode
	 */
	public void setFCDMode( Mode mode ) throws UsbException, UsbClaimException
	{
		ByteBuffer response = null;
		
		switch( mode )
		{
			case APPLICATION:
				response = send( FCDCommand.BL_QUERY );
				break;
			case BOOTLOADER:
				response = send( FCDCommand.APP_RESET );
				break;
			default:
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
			ByteBuffer buffer = send( FCDCommand.APP_GET_FREQUENCY_HZ );
			
			buffer.order( ByteOrder.LITTLE_ENDIAN );
			
			return (int)( buffer.getInt( 2 ) & 0xFFFFFFFF );
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
	 * Claims the USB interface.  Attempts to detach the active kernel driver if
	 * one is currently attached.
	 */
	private void claimInterface() throws SourceException
	{
		if( mDeviceHandle != null )
		{
			int result = LibUsb.kernelDriverActive( mDeviceHandle, FCD_INTERFACE );
			
			if( result == 1 )
			{
				result = LibUsb.detachKernelDriver( mDeviceHandle, 
						FCD_INTERFACE );

				if( result != LibUsb.SUCCESS )
				{
					mLog.error( "failed attempt to detach kernel driver [" + 
					LibUsb.errorName( result ) + "]" );
				}
			}
			
			result = LibUsb.claimInterface( mDeviceHandle, FCD_INTERFACE );
			
			if( result != LibUsb.SUCCESS )
			{
				throw new SourceException( "couldn't claim usb interface [" + 
					LibUsb.errorName( result ) + "]" );
			}
		}
		else
		{
			throw new SourceException( "couldn't claim usb hid interface - no "
					+ "device handle" );
		}
	}

	/**
	 * Performs an interrupt write to the OUT endpoint.
	 * @param buffer - direct allocated buffer.  Must be 64 bytes in length.
	 * 
	 * @throws LibUsbException on error
	 */
	private void write( ByteBuffer buffer ) throws LibUsbException
	{
		if( mDeviceHandle != null )
		{
			IntBuffer transferred = IntBuffer.allocate( 1 );
			
			int result = LibUsb.interruptTransfer( mDeviceHandle,
												   FCD_ENDPOINT_OUT,
												   buffer, 
												   transferred,
												   500l );
			
			if( result != LibUsb.SUCCESS )
			{
				throw new LibUsbException( "error writing byte buffer", 
								result );
			}
//			else if( transferred.get() != buffer.capacity() )
//			{
//				throw new LibUsbException( "transferred bytes [" + 
//						transferred.get( 0 ) + "] is not what was expected [" + 
//						buffer.capacity() + "]", result );
//			}
		}
		else
		{
			throw new LibUsbException( "device handle is null", 
							LibUsb.ERROR_NO_DEVICE );
		}
	}
	
	/**
	 * Performs an interrupt write to the OUT endpoint for the FCD command.
	 * @param command - no-argument command to write
	 * @throws LibUsbException - on error
	 */
	private void write( FCDCommand command ) throws LibUsbException
	{
		ByteBuffer buffer = ByteBuffer.allocateDirect( 64 );

		buffer.put( 0, command.getCommand() );
		buffer.put( 1, (byte)0x00 );

		write( buffer );
	}

	/**
	 * Convenience logger for debugging read/write operations
	 */
	@SuppressWarnings( "unused" )
    private void log( String label, ByteBuffer buffer )
	{
		StringBuilder sb = new StringBuilder();

		sb.append( label );
		
		sb.append( " " );

		sb.append( buffer.get( 0 ) );
		sb.append( " | " );
		
		for( int x = 0; x < 64; x++ )
		{
			sb.append( String.format( "%02X", (int)( buffer.get( x ) & 0xFF ) ) );
			sb.append( " " );
		}
		
		mLog.debug( sb.toString() );
	}
	
	/**
	 * Performs an interrupt write to the OUT endpoint for the FCD command.
	 * @param command - command to write
	 * @param argument - value to write with the command
	 * @throws LibUsbException - on error
	 */
	private void write( FCDCommand command, long argument ) throws LibUsbException
	{
		ByteBuffer buffer = ByteBuffer.allocateDirect( 64 );

		/* The FCD expects little-endian formatted values */
		buffer.order( ByteOrder.LITTLE_ENDIAN );

		buffer.put( 0, command.getCommand() );
		buffer.putLong( 1, argument );

		write( buffer );
	}
	
	/**
	 * Performs an interrupt read against the endpoint
	 * @return buffer read from FCD
	 * @throws LibUsbException on error
	 */
	private ByteBuffer read() throws LibUsbException
	{
		if( mDeviceHandle != null )
		{
			ByteBuffer buffer = ByteBuffer.allocateDirect( 64 );

			IntBuffer transferred = IntBuffer.allocate( 1 );
			

			int result = LibUsb.interruptTransfer( mDeviceHandle,
												   FCD_ENDPOINT_IN,
												   buffer, 
												   transferred,
												   500l );
			
			if( result != LibUsb.SUCCESS )
			{
				throw new LibUsbException( "error reading byte buffer", 
								result );
			}
			else if( transferred.get( 0 ) != buffer.capacity() )
			{
				throw new LibUsbException( "received bytes [" + 
						transferred.get( 0 ) + "] didn't match expected "
						+ "length [" + buffer.capacity() + "]", result );
			}

			return buffer;
		}
		
		throw new LibUsbException( "device handle is null", 
				LibUsb.ERROR_NO_DEVICE );
	}
	
	/**
	 * Sends the FCD command and argument.  Performs a read to complete the
	 * command.
	 * 
	 * @param command - command to send
	 * @param argument - command argument to send
	 * @throws LibUsbException - on error
	 */
	protected void send( FCDCommand command, long argument ) throws LibUsbException
	{
		write( command, argument );
		read();
	}
		
	/**
	 * Sends the no-argument FCD command.  Performs a read to complete the
	 * command.
	 * 
	 * @param command - command to send
	 * @throws LibUsbException - on error
	 */
	protected ByteBuffer send( FCDCommand command ) throws LibUsbException
	{
		write( command );
		return read();
	}

	/**
	 * FCD configuration string parsing  class
	 */
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
		
		/**
		 * Extracts the configuration string from the buffer
		 */
		public void set( ByteBuffer buffer )
		{
			if( buffer.capacity() == 64 )
			{
				byte[] data = new byte[ 64 ];
				
				for( int x = 0; x < 64; x++ )
				{
					data[ x ] = buffer.get( x );
				}

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
