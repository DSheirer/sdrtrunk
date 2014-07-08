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
package source.tuner;

public class TunerChannel implements Comparable<TunerChannel>
{
	private Type mType;
	private long mFrequency;
	private int mBandwidth;
	
	public TunerChannel( Type type, long frequency, int bandwidth )
	{
		mType = type;
		mFrequency = frequency;
		mBandwidth = bandwidth;
	}
	
	public Type getType()
	{
		return mType;
	}
	
	public void setType( Type type )
	{
		mType = type;
	}

	public long getFrequency()
	{
		return mFrequency;
	}
	
	public void setFrequency( long frequency )
	{
		mFrequency = frequency;
	}
	
	public int getBandwidth()
	{
		return mBandwidth;
	}
	
	public void setBandwidth( int bandwidth )
	{
		mBandwidth = bandwidth;
	}
	
	public long getMinFrequency()
	{
		return mFrequency - ( mBandwidth / 2 );
	}
	
	public long getMaxFrequency()
	{
		return mFrequency + ( mBandwidth / 2 );
	}

	/**
	 * Indicates if any part of this tuner channel is contained within the
	 * minimum and maximum frequency values.
	 */
	public boolean isWithin( long minimum, long maximum )
	{
		return ( ( minimum <= getMinFrequency() && getMinFrequency() <= maximum ) ||
				 ( minimum <= getMaxFrequency() && getMaxFrequency() <= maximum ) );
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( "Channel: " );
		sb.append( ( mFrequency - ( mBandwidth / 2 ) ) );
		sb.append( "-" );
		sb.append( ( mFrequency + ( mBandwidth / 2 ) ) );

		return sb.toString();
	}

	@Override
    public int compareTo( TunerChannel otherLock )
    {
		if( mFrequency < otherLock.getFrequency() )
		{
			return -1;
		}
		else
		{
			return 1;
		}
    }
	
	public enum Type
	{
		LOCKED,  //Use for decoding channels, so that they cannot be tuned out of range
		TRAFFIC, //Like locked, but for temporary allocated traffic channels
		TUNABLE; //Tunable -- can be tuned out of range
	}
}
