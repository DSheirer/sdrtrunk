/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
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
	 * Indicates if any part of this tuner channel overlaps the specified range
	 */
	public boolean overlaps( long minimum, long maximum )
	{
		long channelMin = getMinFrequency();
		long channelMax = getMaxFrequency();
		
		return ( ( minimum <= channelMin && channelMin <= maximum ) ||
				 ( minimum <= channelMax && channelMax <= maximum ) ||
				 ( minimum <= channelMin && channelMax <= maximum ) ||
				 ( channelMin <= minimum && maximum <= channelMax ) );
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( "Channel: " );
		sb.append( getMinFrequency() );
		sb.append( "-" );
		sb.append( getMaxFrequency() );

		return sb.toString();
	}

	@Override
    public int compareTo( TunerChannel otherTunerChannel )
    {
		if( this.equals( otherTunerChannel ) )
		{
			return 0;
		}
		else if( mFrequency < otherTunerChannel.getFrequency() )
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
