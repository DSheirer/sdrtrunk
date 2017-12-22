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
package io.github.dsheirer.source.tuner.fcd;

public enum FCDCommand
{
	/**
	 * Note: the javaxUSB and usb4Java libraries are slightly different than
	 * the signal9 HID libraries used in C, so you will see variations in the
	 * byte array length and indexes, when comparing this setup to the qthid
	 * source code.
	 */
	//Bootloader Mode Commands
	BL_QUERY( (byte) 0x01, 64 ),               //01 Decimal
	BL_RESET( (byte) 0x08, 2 ),               //08
	BL_ERASE( (byte) 0x19, 2 ),               //24
	BL_SET_BYTE_ADDR( (byte) 0x1A, 6 ),       //25
	BL_GET_BYTE_ADDR_RANGE( (byte) 0x1B, 6 ), //26
	BL_WRITE_FLASH_BLOCK( (byte) 0x1C, 52 ),   //27
	BL_READ_FLASH_BLOCK( (byte) 0x1D, 48 ),    //28

	//Application Mode Commands
	APP_SET_FREQUENCY_KHZ( (byte)0x64, 5 ),   //100
	APP_SET_FREQUENCY_HZ( (byte)0x65, 6 ),    //101
	APP_GET_FREQUENCY_HZ( (byte)0x66, 6 ),    //102
	APP_GET_IF_RSSI( (byte) 0x68, 3 ),        //104
	APP_GET_PLL_LOCKED( (byte) 0x69, 3 ),     //105  
	APP_SET_DC_CORRECTION( (byte) 0x6A, 5 ),  //106
	APP_GET_DC_CORRECTION( (byte) 0x6B, 6 ),  //107
	APP_SET_IQ_CORRECTION( (byte) 0x6C, 5 ),  //108
	APP_GET_IQ_CORRECTION( (byte) 0x6D, 6 ),  //109
	APP_SET_LNA_GAIN( (byte)0x6E, 2 ),        //110
	APP_SET_LNA_ENHANCE( (byte)0x6F, 2 ),     //111
	APP_SET_BAND( (byte)0x70, 2 ),            //112
	APP_SET_RF_FILTER( (byte)0x71, 2 ),       //113
	APP_SET_MIXER_GAIN( (byte)0x72, 2 ),      //114
	APP_SET_BIAS_CURRENT( (byte)0x73, 2 ),    //115
	APP_SET_MIXER_FILTER( (byte)0x74, 2 ),    //116
	APP_SET_IF_GAIN1( (byte)0x75, 2 ),        //117
	APP_SET_IF_GAIN_MODE( (byte)0x76, 2 ),    //118
	APP_SET_IF_RC_FILTER( (byte)0x77, 2 ),    //119
	APP_SET_IF_GAIN2( (byte)0x78, 2 ),        //120
	APP_SET_IF_GAIN3( (byte)0x79, 2 ),        //121
	APP_SET_IF_FILTER( (byte)0x7A, 2 ),       //122
	APP_SET_IF_GAIN4( (byte)0x7B, 2 ),        //123
	APP_SET_IF_GAIN5( (byte)0x7C, 2 ),        //124
	APP_SET_IF_GAIN6( (byte)0x7D, 2 ),        //125
	APP_SET_BIAS_TEE( (byte)0x7E, 2 ),        //126
	
	APP_GET_LNA_GAIN( (byte)0x96, 3 ),        //150
	APP_GET_LNA_ENHANCE( (byte)0x97, 3 ),     //151
	APP_GET_BAND( (byte)0x98, 3 ),            //152
	APP_GET_RF_FILTER( (byte)0x99, 3 ),       //153
	APP_GET_MIXER_GAIN( (byte)0x9A, 3 ),      //154
	APP_GET_BIAS_CURRENT( (byte)0x9B, 3 ),    //155
	APP_GET_MIXER_FILTER( (byte)0x9C, 3 ),    //156
	APP_GET_IF_GAIN1( (byte)0x9D, 3 ),        //157
	APP_GET_IF_GAIN_MODE( (byte)0x9E, 3 ),    //158
	APP_GET_IF_RC_FILTER( (byte)0x9F, 3 ),    //159
	APP_GET_IF_GAIN2( (byte)0xA0, 3 ),        //160
	APP_GET_IF_GAIN3( (byte)0xA1, 3 ),        //161
	APP_GET_IF_FILTER( (byte)0xA2, 3 ),       //162
	APP_GET_IF_GAIN4( (byte)0xA3, 3 ),        //163
	APP_GET_IF_GAIN5( (byte)0xA4, 3 ),        //164
	APP_GET_IF_GAIN6( (byte)0xA5, 3 ),        //165
	APP_GET_BIAS_TEE( (byte)0xA6, 3 ),        //166

	APP_SEND_I2C_BYTE( (byte)0xC8, 2 ),       //200
	APP_RECV_I2C_BYTE( (byte)0xC9, 3 ),       //201

	APP_RESET( (byte)0xFF, 1 );               //255
	
	/**
	 * Single-byte value to use for the command
	 */
	private byte mCommandByte;

	/**
	 * Required array length
	 */
	private int mArrayLength;
	
	private FCDCommand( byte value, int responseLength )
	{
		mCommandByte = value;
		mArrayLength = responseLength;
	}

	/**
	 * Returns a byte array of the specified length with the command byte value
	 * set in index position zero.  Fill in additional command arguments prior
	 * to sending to the device
	 */
	public byte[] getRequestTemplate()
	{
		byte[] retVal = new byte[ 64 ];

		retVal[ 0 ] = mCommandByte;
		retVal[ 1 ] = (byte)0x0;
		
		return retVal;
	}

	/**
	 * Returns an empty byte array of the correct length to receive the response 
	 * from the associated command 
	 */
	public byte[] getResponseTemplate()
	{
		return new byte[ 64 ];
	}

	/**
	 * Byte value of the command
	 */
	public byte getCommand()
	{
		return mCommandByte;
	}

	/**
	 * Total array length required to send the command or receive the response.
	 * 
	 * Index 0: command is echoed back
	 * Index 1: 0-fail, 1-success
	 */
	public int getArrayLength()
	{
		return mArrayLength;
	}

	/**
	 * Checks the response array to verify the pass/fail value that is placed
	 * in array index position 1, in responses from the device
	 */
	public static boolean checkResponse( FCDCommand command, byte[] response )
	{
		boolean valid = false;
		
		if( command != null && response != null )
		{
			valid = response[ 0 ] == command.getCommand() && 
					response[ 1 ] == 1;
		}
		
		return valid;
	}
}
