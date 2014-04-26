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
package bits;

public enum SyncPattern
{
	/* Revs (0xAA) and Sync (0x7650) = 01010 0111011001010000 */
	FLEETSYNC1( new boolean[] 
	{ 
		false, true, false, true, false,     //end of revs
		false, true, true, true,             //0111 0x7 
		false, true, true, false,            //0110 0x6
		false, true, false, true,            //0101 0x5
		false, false, false, false           //0000 0x0
	} ),

	/* Revs (0x0A) and Sync (0x23EB) = 10101 0010001111101011 */
	FLEETSYNC2( new boolean[] 
	{ 
		false, true, false, true, false,	//End of bit revs
		false, false, true, false,          //0010 0x2 
		false, false, true, true,           //0011 0x3
		true, true, true, false,            //1110 0xE
		true, false, true, true             //1011 0xB
	} ),

	/**
	 * Note: we use a truncated portion of the NRZ-I encoded sync pattern.
	 * The truncated version seems to work well enough for detecting the burst, 
	 * while limiting falsing.
	 * 
	 *       Sync: .... .... 0000 0111 0000 1001 0010 1010 0100 0100 0110 1111
	 * Rev & Sync: ...1 0101 0000 0111 0000 1001 0010 1010 0100 0100 0110 1111
	 *      NRZ-I: .... 1111 1000 0100 1000 1101 1011 1111 0110 0110 0101 1000
	 *       This: .... .... 1000 0100 1000 1101 1011 1111 .... .... .... ....
	 */
	/* Sync (0x07092A446F) = 0000011100001001001010100100010001101111 */
	MDC1200( new boolean[]
	{
		false, false, false, false,//0000 0x0
		false, true, true, true,   //0111 0x7
		false, false, false, false,//0000 0x0
		true, false, false, true,  //1001 0x9
		false, false, true, false, //0010 0x2
		true, false, true, false,  //1010 0xA
		false, true, false, false, //0100 0x4
		false, true, false, false, //0100 0x4
		false, true, true, false,  //0110 0x6
		true, true, true, true     //1111 0xF
	} ),

	/* Revs (0xA) and Sync(0xA4D7) = 1010 1100010011010111 */
	MPT1327_CONTROL( new boolean[]
	{
		true, false, true, false,   //Includes 4 of 16 rev bits
//		false, true, false, true,   //Includes 4 of 16 rev bits
		true, true, false, false,   //1100 0xA
		false, true, false, false,  //0100 0x4
		true, true, false, true,    //1101 0xD
		false, true, true, true     //0111 0x7
	}),

	/* Revs (0xA) and Sync(0x3B28) = 1010 0011101100101000 */
	MPT1327_TRAFFIC( new boolean[]
	{
		true, false, true, false,   //1010 Includes 4 of 16 rev bits
//	false, true, false, true,   //1010 Includes 4 of 16 rev bits
		false, false, true, true,   //0011 0x3
		true, false, true, true,    //1011 0xB
		false, false, true, false,  //0010 0x2
		true, false, false, false   //1000 0x8
	}),
	
	/* Sync (0x158) = 1101011000 */
	PASSPORT( new boolean[] 
	{
		true, 						//0001 0x1
		false, true, false, true, 	//0101 0x5
		true, false, false, false	//1000 0x8
	} ),
	
	LTR_STANDARD_OSW( new boolean[] 
	{
		true,false,true,false,true,true,false,false,false
	} ),
	
	LTR_STANDARD_ISW( new boolean[] 
	{
		false, true, false, true,false, false, true, true, true
	} );
	
	boolean[] mBits;
	
	private SyncPattern( boolean[] bits )
	{
		mBits = bits;
	}
	
	public boolean[] getPattern()
	{
		return mBits;
	}
}
