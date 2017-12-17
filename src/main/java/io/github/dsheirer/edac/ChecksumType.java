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
package io.github.dsheirer.edac;

public enum ChecksumType
{
	FLEETSYNC_ANI( new short[]
	{
			0x0000, //Unknown Always One
			0x0000, //Status Msg Flag
			0x14C3, //Status Message 6  001010011000011
			0x6908, //Status Message 5  110100100001000
			0x3484, //Status Message 4  011010010000100
			0x1A42, //Status Message 3  001101001000010
			0x0D21, //Status Message 2  000110100100001
			0x729A, //Status Message 1  111001010011010
			0x394D, //Status Message 0  011100101001101
			0x0000, //Type 7
			0x0000, //Type 6
			0x0000, //Type 5
			0x0000, //Type 4
			0x0000, //Type 3
			0x0000, //Type 2
			0x0000, //Type 1
			0x0000, //Type 0
			0x0000, //Fleet From 7
			0x0000, //Fleet From 6
			0x0000, //Fleet From 5
			0x0000, //Fleet From 4
			0x402E, //Fleet From 3      100000000101110
			0x2017, //Fleet From 2      010000000010111
			0x6401, //Fleet From 1      110010000000001
			0x460A, //Fleet From 0      100011000001010
			0x0000, //Ident From 11
			0x0000, //Ident From 10
			0x0000, //Ident From 9
			0x0000, //Ident From 8
			0x0CB1, //Ident From 7      000110010110001
			0x7252, //Ident From 6      111001001010010
			0x3929, //Ident From 5      011100100101001
			0x689E, //Ident From 4      110100010011110
			0x344F, //Ident From 3      011010001001111
			0x6E2D, //Ident From 2      110111000101101
			0x431C, //Ident From 1      100001100011100
			0x218E, //Ident From 0      010000110001110
			0x0000, //Ident To 11
			0x0000, //Ident To 10
			0x0000, //Ident To 9
			0x0000, //Ident To 8
			0x6685, //Ident To 7        110011010000101
			0x4748, //Ident To 6        100011101001000
			0x23A4, //Ident To 5        010001110100100
			0x11D2, //Ident To 4        001000111010010
			0x08E9, //Ident To 3        000100011101001
			0x707E, //Ident To 2        111000001111110
			0x383F, //Ident To 1        011100000111111
			0x6815, //Ident To 0        110100000010101
	}),
	
	LTR( new short[] 
	{	0x0038, //Area
		0x001C, //Channel 4
		0x000E, //Channel 3
		0x0046, //Channel 2
		0x0023, //Channel 1
		0x0051, //Channel 0
		0x0068, //Home 4
		0x0075, //Home 3
		0x007A, //Home 2
		0x003D, //Home 1
		0x001F, //Home 0
		0x004F, //Group 7
		0x0026, //Group 6
		0x0052, //Group 5
		0x0029, //Group 4
		0x0015, //Group 3
		0x000B, //Group 2
		0x0045, //Group 1
		0x0062, //Group 0
		0x0031, //Free 4
		0x0019, //Free 3
		0x000D, //Free 2
		0x0007, //Free 1
		0x0043  //Free 0 
	} ), 
	
	PASSPORT( new short[] 
	{
//	        0x6e,
//	        0xbf, 
//	        0xd6,
//	        0xe3,
//	        0xf8, 
//	        0x7c,
//	        0x3e,
//	        0x97,
//	        0xc2,
//	        0xe9,
//	        0x75,
//	        0x3b,
//	        0x94,
//	        0x4a,
//	        0xad,
//	        0x57,
//	        0xa2,
//	        0xd9,
//	        0x6d,
//	        0x37,
//	        0x92,
//	        0xc1,
//	        0x61,
//	        0x31,        
//	        0x19,
//	        0x0d,
//	        0x07,
//	        0x8a,
//	        0xcd,
//	        0x67,
//	        0xba,
//	        0xd5,
//	        0x6b,   // 0111110
//	        0xbc,   // 1111100
//	        0x5e,   // 1101001
//	        0xa7,   // 1000011
//	        0xda,   // 0010111
//	        0xe5,   // 0101110
//	        0x73,   // 1011100
//	        0xb0,   // 0101001
//	        0x58,   // 1010010
//	        0x2c,   // 0110101 Bit 43
//	        0x16,   // 1101010
//	        0x83,   // 1000101 Bit 45
//	        0xc8,   // 0011011 Bit 46
//	        0x64,   // 0110110
//	        0x32,   // 1101100
//	        0x91,   // 1001001
//	        0x49,
//	        0x25,
//	        0x13,
	} ),
	
	MULTINET( new short[] 
	{
//        0x96, // 0010110
//        0xAC,   // 0101100
//        0xd8,   // 1011000
//        0x21,   // 0100001
//        0x42,   // 1000010
//        0x95,   // 0010101
//        0xaa,   // 0101010
//        0xd4,   // 1010100
//        0x39,   // 0111001
//        0x72,   // 1110010
//        0xf5,   // 1110101 Bit 10
//        0x7b,   // 1111011
//        0xe7,   // 1100111
//        0x5f,   // 1011111
//        0xaf,   // 0101111
//        0xde,   // 1011110
//        0x2d,   // 0101101
//        0x5a,   // 1011010
//        0xa5,   // 0100101
//        0xca,   // 1001010
//        0x05,   // 0000101
//        0x0a,   // 0001010
//        0x14,   // 0010100
//        0x28,   // 0101000
//        0x50,   // 1010000
//        0xb1,   // 0110001
//        0xe2,   // 1100010
//        0x55,   // 1010101
//        0xbb,   // 0111011
//        0xf6,   // 1110110
//        0x7d,   // 1111101
//        0xeb,   // 1101011
//        0x47,   // 1000111
//        0x9f,   // 0011111 Bit 33
//        0xbe,   // 0111110
//        0xfc,   // 1111100
//        0x69,   // 1101001
//        0xc3,   // 1000011
//        0x17,   // 0010111
//        0x2e,   // 0101110
//        0x5c,   // 1011100
//        0xA9,   // 0101001
//        0xD2,   // 1010010
//        0x35,   // 0110101 Bit 43
//        0x6A,   // 1101010
//        0xC5,   // 1000101 Bit 45
//        0x1B,   // 0011011 Bit 46
//        0x36,   // 0110110
//        0x6C,   // 1101100
//        0xC9    // 1001001
	});
	
	private short[] mValues;
	
	ChecksumType( short[] values )
	{
		mValues = values;
	}
	
	public short[] getValues()
	{
		return mValues;
	}

}
