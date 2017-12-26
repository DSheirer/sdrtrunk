package io.github.dsheirer.source.tuner.airspy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class AirspyDeviceInformation
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( AirspyDeviceInformation.class );

	private AirspyTunerController.BoardID mBoardID = AirspyTunerController.BoardID.UNKNOWN;
	private String mFirmwareVersion = "Unknown";
	private String mPartNumber = "Unknown";
	private String mSerialNumber = "Unknown";

	/**
	 * Airspy Device Information - provides parsing and access to device
	 * information components.
	 */
	public AirspyDeviceInformation()
	{
	}
	
	public void setBoardID( int boardID )
	{
		mBoardID = AirspyTunerController.BoardID.fromValue( boardID );
	}
	
	public AirspyTunerController.BoardID getBoardID()
	{
		return mBoardID;
	}
	
	public void setVersion( byte[] data )
	{
		if( data != null && data.length > 0 )
		{
			/* Truncate the version string if we have a line break - ASCII 10 */
			for( int x = 0; x < data.length; x++ )
			{
				if( data[ x ] == (byte)10 )
				{
					mFirmwareVersion = new String( Arrays.copyOf( data, x ) );
					mFirmwareVersion = mFirmwareVersion.replace( "AirSpy NOS ", "" );
					return;
				}
			}
			
			mFirmwareVersion = new String( data );
			mFirmwareVersion = mFirmwareVersion.replace( "AirSpy NOS ", "" );
		}
		else
		{
			mLog.error( "Error setting airspy version byte data - null:" + 
				( data == null ? " TRUE" : " FALSE length:" + data.length ) );
		}
	}
	
	public String getVersion()
	{
		return mFirmwareVersion;
	}
	
	public void setPartAndSerialNumber( byte[] data )
	{
		if( data != null && data.length == 24 )
		{
			//Note: values are byte-reversed (big-endian) 32-bit chunks
			StringBuilder part = new StringBuilder();
			
			part.append( format( data[ 3 ] ) );
			part.append( format( data[ 2 ] ) );
			part.append( format( data[ 1 ] ) );
			part.append( format( data[ 0 ] ) );
			part.append( format( data[ 7 ] ) );
			part.append( format( data[ 6 ] ) );
			part.append( format( data[ 5 ] ) );
			part.append( format( data[ 4 ] ) );

			mPartNumber = part.toString();
			
			StringBuilder serial = new StringBuilder();

			//Note: current airspy library only exposes 64-bits of serial number
			//address space, but appears to have room for 128-bits.  We won't
			//add the first 64 bits if it's zeros
			if( !( data[ 8 ] == 0 && data[ 9 ] == 0 && data[ 10 ] == 0 && data[ 11 ] == 0 ) )
			{
				serial.append( format( data[ 11 ] ) );
				serial.append( format( data[ 10 ] ) );
				serial.append( format( data[ 9 ] ) );
				serial.append( format( data[ 8 ] ) );
				serial.append( "-" );
			}

			if( !( data[ 12 ] == 0 && data[ 13 ] == 0 && data[ 14 ] == 0 && data[ 15 ] == 0 ) )
			{
				serial.append( format( data[ 15 ] ) );
				serial.append( format( data[ 14 ] ) );
				serial.append( format( data[ 13 ] ) );
				serial.append( format( data[ 12 ] ) );
				serial.append( "-" );
			}
			
			serial.append( format( data[ 19 ] ) );
			serial.append( format( data[ 18 ] ) );
			serial.append( format( data[ 17 ] ) );
			serial.append( format( data[ 16 ] ) );
			serial.append( "-" );
			serial.append( format( data[ 23 ] ) );
			serial.append( format( data[ 22 ] ) );
			serial.append( format( data[ 21 ] ) );
			serial.append( format( data[ 20 ] ) );

			mSerialNumber = serial.toString();
		}
		else
		{
			mLog.error( "Error setting airspy part and serial byte data - null:" + 
					( data == null ? " TRUE" : " FALSE length:" + data.length ) );
		}
	}
	
	private String format( byte value )
	{
		return String.format( "%02X", value );
	}
	
	public String getSerialNumber()
	{
		return mSerialNumber;
	}
	
	public String getPartNumber()
	{
		return mPartNumber;
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append( "Airspy Device Information\n" );
		sb.append( "Board: " );
		sb.append( getBoardID().getLabel() );
		sb.append( "\nPart Number: " );
		sb.append( getPartNumber() );
		sb.append( "\nSerial Number: " );
		sb.append( getSerialNumber() );
		sb.append( "\nFirmware: " );
		sb.append( getVersion() );
		
		return sb.toString();
	}
}
