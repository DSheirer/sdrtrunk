/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25.message.tsbk.motorola;

public enum MotorolaOpcode
{
	PATCH_GROUP_ADD( "ADD PATCH GROUP", "ADD PATCH SUPER GROUP", 0x00 ),
	PATCH_GROUP_DELETE( "DELETE PATCH GROUP", "DELETE PATCH SUPER GROUP", 0x01 ),
	PATCH_GROUP_CHANNEL_GRANT( "PATCH VOICE CHANNEL_NUMBER GRANT", "PATCH VOICE CHANNEL_NUMBER GRANT", 0x02 ),
	PATCH_GROUP_CHANNEL_GRANT_UPDATE( "PATCH VOICE CHANNEL_NUMBER UPDATE", "PATCH VOICE CHANNEL_NUMBER GRANT UPDATE", 0x03 ),
	OP04( "MOT OP04:UNKNOWN", "Opcode 0x04 Unknown", 0x04 ),
	TRAFFIC_CHANNEL_ID( "TRAFFIC CHAN STATION ID", "TRAFFIC CHAN STATION ID", 0x05 ),
	OP06( "MOT OP06:UNKNOWN    ", "Opcode 0x06 Unknown", 0x06 ),
	OP07( "MOT OP07:UNKNOWN    ", "Opcode 0x07 Unknown", 0x07 ),
	OP08( "MOT OP08:UNKNOWN    ", "Opcode 0x08 Unknown", 0x08 ),
	SYSTEM_LOAD( "SYSTEM LOADING", "SYSTEM LOAD", 0x09 ),
	OP0A( "OP0A:UNKNOWN    ", "Opcode 0x0A Unknown", 0x0A ),
	CONTROL_CHANNEL_ID( "CONTROL CHAN BASE STATION ID", "CONTROL CHAN BASE STATION ID", 0x0B ),
	OP0C( "MOT OP0C:UNKNOWN    ", "Opcode 0x0C Unknown", 0x0C ),
	OP0D( "MOT OP0D:UNKNOWN    ", "Opcode 0x0D Unknown", 0x0D ),
	CCH_PLANNED_SHUTDOWN( "PLANNED CONTROL CHANNEL_NUMBER SHUTDOWN ", "PLANNED CONTROL CHANNEL_NUMBER SHUTDOWN", 0x0E ),
	OP0F( "MOT OP0F:UNKNOWN    ", "Opcode 0x0F Unknown", 0x0F ),
	OP10( "MOT OP10:UNKNOWN    ", "Opcode 0x10 Unknown", 0x10 ),
	OP11( "MOT OP11:UNKNOWN    ", "Opcode 0x11 Unknown", 0x11 ),
	OP12( "MOT OP12:UNKNOWN    ", "Opcode 0x12 Unknown", 0x12 ),
	OP13( "MOT OP13:UNKNOWN    ", "Opcode 0x13 Unknown", 0x13 ),
	OP14( "MOT OP14:UNKNOWN    ", "Opcode 0x14 Unknown", 0x14 ),
	OP15( "MOT OP15:UNKNOWN    ", "Opcode 0x15 Unknown", 0x15 ),
	OP16( "MOT OP16:UNKNOWN    ", "Opcode 0x16 Unknown", 0x16 ),
	OP17( "MOT OP17:UNKNOWN    ", "Opcode 0x17 Unknown", 0x17 ),
	OP18( "MOT OP18:UNKNOWN    ", "Opcode 0x18 Unknown", 0x18 ),
	OP19( "MOT OP19:UNKNOWN    ", "Opcode 0x19 Unknown", 0x19 ),
	OP1A( "MOT OP1A:UNKNOWN    ", "Opcode 0x1A Unknown", 0x1A ),
	OP1B( "MOT OP1B:UNKNOWN    ", "Opcode 0x1B Unknown", 0x1B ),
	OP1C( "MOT OP1C:UNKNOWN    ", "Opcode 0x1C Unknown", 0x1C ),
	OP1D( "MOT OP1D:UNKNOWN    ", "Opcode 0x1D Unknown", 0x1D ),
	OP1E( "MOT OP1E:UNKNOWN    ", "Opcode 0x1E Unknown", 0x1E ),
	OP1F( "MOT OP1F:UNKNOWN    ", "Opcode 0x1F Unknown", 0x1F ),
	OP20( "MOT OP20:UNKNOWN    ", "Opcode 0x20 Unknown", 0x20 ),
	OP21( "MOT OP21:UNKNOWN    ", "Opcode 0x21 Unknown", 0x21 ),
	OP22( "MOT OP22:UNKNOWN    ", "Opcode 0x22 Unknown", 0x22 ),
	OP23( "MOT OP23:UNKNOWN    ", "Opcode 0x23 Unknown", 0x23 ),
	OP24( "MOT OP24:UNKNOWN    ", "Opcode 0x24 Unknown", 0x24 ),
	OP25( "MOT OP25:UNKNOWN    ", "Opcode 0x25 Unknown", 0x25 ),
	OP26( "MOT OP26:UNKNOWN    ", "Opcode 0x26 Unknown", 0x26 ),
	OP27( "MOT OP27:UNKNOWN    ", "Opcode 0x27 Unknown", 0x27 ),
	OP28( "MOT OP28:UNKNOWN    ", "Opcode 0x28 Unknown", 0x28 ),
	OP29( "MOT OP29:UNKNOWN    ", "Opcode 0x29 Unknown", 0x29 ),
	OP2A( "MOT OP2A:UNKNOWN    ", "Opcode 0x2A Unknown", 0x2A ),
	OP2B( "MOT OP2B:UNKNOWN    ", "Opcode 0x2B Unknown", 0x2B ),
	OP2C( "MOT OP2C:UNKNOWN    ", "Opcode 0x2C Unknown", 0x2C ),
	OP2D( "MOT OP2D:UNKNOWN    ", "Opcode 0x2D Unknown", 0x2D ),
	OP2E( "MOT OP2E:UNKNOWN    ", "Opcode 0x2E Unknown", 0x2E ),
	OP2F( "MOT OP2F:UNKNOWN    ", "Opcode 0x2F Unknown", 0x2F ),
	OP30( "MOT OP30:UNKNOWN    ", "Opcode 0x30 Unknown", 0x30 ),
	OP31( "MOT OP31:UNKNOWN    ", "Opcode 0x31 Unknown", 0x31 ),
	OP32( "MOT OP32:UNKNOWN    ", "Opcode 0x32 Unknown", 0x32 ),
	OP33( "MOT OP33:UNKNOWN    ", "Opcode 0x33 Unknown", 0x33 ),
	OP34( "MOT OP34:UNKNOWN    ", "Opcode 0x34 Unknown", 0x34 ),
	OP35( "MOT OP35:UNKNOWN    ", "Opcode 0x35 Unknown", 0x35 ),
	OP36( "MOT OP36:UNKNOWN    ", "Opcode 0x36 Unknown", 0x36 ),
	OP37( "MOT OP37:UNKNOWN    ", "Opcode 0x37 Unknown", 0x37 ),
	OP38( "MOT OP38:UNKNOWN    ", "Opcode 0x38 Unknown", 0x38 ),
	OP39( "MOT OP39:UNKNOWN    ", "Opcode 0x39 Unknown", 0x39 ),
	OP3A( "MOT OP3A:UNKNOWN    ", "Opcode 0x3A Unknown", 0x3A ),
	OP3B( "MOT OP3B:UNKNOWN    ", "Opcode 0x3B Unknown", 0x3B ),
	OP3C( "MOT OP3C:UNKNOWN    ", "Opcode 0x3C Unknown", 0x3C ),
	OP3D( "MOT OP3D:UNKNOWN    ", "Opcode 0x3D Unknown", 0x3D ),
	OP3E( "MOT OP3E:UNKNOWN    ", "Opcode 0x3E Unknown", 0x3E ),
	OP3F( "MOT OP3F:UNKNOWN    ", "Opcode 0x3F Unknown", 0x3F ),
	UNKNOWN( "UNKNOWN OPCODE ", "Unknown", -1 );

	private String mLabel;
	private String mDescription;
	private int mCode;
	
	private MotorolaOpcode( String label, String description, int code )
	{
		mLabel = label;
		mDescription = description;
		mCode = code;
	}
	
	public String getLabel()
	{
		return mLabel;
	}
	
	public String toString()
	{
		return getLabel();
	}
	
	public String getDescription()
	{
		return mDescription;
	}
	
	public int getCode()
	{
		return mCode;
	}
	
	public static MotorolaOpcode fromValue( int value )
	{
		if( 0 <= value && value <= 0x3F )
		{
			return MotorolaOpcode.values()[ value ];
		}
		
		return UNKNOWN;
	}
}
