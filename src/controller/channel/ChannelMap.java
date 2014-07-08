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
package controller.channel;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;

@XmlSeeAlso( { ChannelRange.class } )
public class ChannelMap
{
	private String mName;
	
	private ArrayList<ChannelRange> mRanges = new ArrayList<ChannelRange>();
	
	public ChannelMap()
	{
		this( "New Channel Map" );
	}
	
	public ChannelMap( String name )
	{
		mName = name;
	}

	public String toString()
	{
		return mName;
	}
	/**
	 * Creates a copy of this channel map
	 */
	public ChannelMap copyOf()
	{
		ChannelMap map = new ChannelMap( mName );
		
		for( ChannelRange range: mRanges )
		{
			map.addRange( range.copyOf() );
		}
		
		return map;
	}
	
	public int getInvalidRange()
	{
		for( int x = 0; x < mRanges.size(); x++ )
		{
			if( !mRanges.get( x ).isValid() )
			{
				return x;
			}
		}
		
		return -1;
	}
	
	public ArrayList<ChannelRange> getRanges()
	{
		return mRanges;
	}

	@XmlElement( name = "range" )
	public void setRanges( ArrayList<ChannelRange> ranges )
	{
		mRanges = ranges;
	}
	
	@XmlAttribute
	public String getName()
	{
		return mName;
	}
	
	public void setName( String name )
	{
		mName = name;
	}
	
	public long getFrequency( int channelNumber )
	{
		for( ChannelRange range: mRanges )
		{
			if( range.hasChannel( channelNumber ) )
			{
				return range.getFrequency( channelNumber );
			}
		}
		
		return 0;
	}
	
	public void addRange( ChannelRange range )
	{
		mRanges.add( range );
	}
	
	public void deleteRange( ChannelRange range )
	{
		mRanges.remove( range );
	}

}
