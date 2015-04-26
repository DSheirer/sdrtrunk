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

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-Sync pattern matcher.  Accepts multiple sync detector objects for 
 * comparision against the incoming bit stream.
 * 
 * Note: works for sync patterns up to 63 bits (integer size - 1 ) long.  
 * If a 64 bit or larger sync pattern is applied, then the wrapping feature
 * of the Long.rotate methods will wrap the MSB around to the LSB and
 * corrupt the matching value.
 *
 */
public class MultiSyncPatternMatcher
{
	private List<ISyncProcessor> mSyncProcessors = new ArrayList<ISyncProcessor>();
	
	private long mBits = 0;
	private long mMask = 0;
	
	public MultiSyncPatternMatcher( int syncSize )
	{
		assert( syncSize < 64 );
		
		//Setup a bit mask of all ones the length of the sync pattern
		mMask = (long)( ( Math.pow( 2, syncSize ) ) - 1 );
	}
	
	/**
	 * Processes two bits before checking sync processors for a match.
	 */
	public void receive( boolean bit1, boolean bit2 )
	{
		mBits = Long.rotateLeft( mBits, 1 );
		
		mBits &= mMask;
		
		if( bit1 )
		{
			mBits += 1;
		}

		mBits = Long.rotateLeft( mBits, 1 );
		
		mBits &= mMask;
		
		if( bit2 )
		{
			mBits += 1;
		}

		for( ISyncProcessor processor: mSyncProcessors )
		{
			processor.checkSync( mBits );
		}
	}
	
	/**
	 * Processes one bit before checking sync processors for a match.
	 */
	public void receive( boolean bit )
	{
		mBits = Long.rotateLeft( mBits, 1 );
		
		mBits &= mMask;
		
		if( bit )
		{
			mBits += 1;
		}

		for( ISyncProcessor processor: mSyncProcessors )
		{
			processor.checkSync( mBits );
		}
	}

	/**
	 * Adds a sync processor to receive the bit stream.
	 */
	public void add( ISyncProcessor processor )
	{
		mSyncProcessors.add( processor );
	}
}
