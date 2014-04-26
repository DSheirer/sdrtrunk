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


/**
 * Sync pattern matcher.
 * 
 * Note: works for sync patterns up to 63 bits (integer size - 1 ) long.  
 * If a 64 bit or larger sync pattern is applied, then the wrapping feature
 * of the Integer.rotate methods will wrap the MSB around to the LSB and
 * corrupt the matching value.
 *
 */
public class SyncPatternMatcher
{
	private long mBits = 0;
	private long mMask = 0;
	private long mSync = 0;
	
	public SyncPatternMatcher( boolean[] syncPattern )
	{
		//Setup a bit mask of all ones the length of the sync pattern
		mMask = (long)( ( Math.pow( 2, syncPattern.length ) ) - 1 );
		
		//Convert the sync bits into a long value for comparison
		for( int x = 0; x < syncPattern.length; x++ )
		{
			if( syncPattern[ x ] )
			{
    			mSync += 1l << ( syncPattern.length - 1 - x );
			}
		}
	}
	
	public void receive( boolean bit )
	{
		//Left shift the previous value
		mBits = Long.rotateLeft( mBits, 1 );
		
		//Apply the mask to erase the previous MSB
		mBits &= mMask;
		
		//Add in the new bit
		if( bit )
		{
			mBits += 1;
		}
	}

	/**
	 * Indicates if the most recently received bit sequence matches the 
	 * sync pattern
	 */
	public boolean matches()
	{
		return ( mBits == mSync );
	}
}
