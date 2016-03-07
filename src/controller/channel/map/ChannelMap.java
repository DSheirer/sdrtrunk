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
package controller.channel.map;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

@XmlSeeAlso( { ChannelRange.class } )
public class ChannelMap
{
	private String mName;
	private boolean mInvalid = false;
	
	private List<ChannelRange> mRanges = new ArrayList<ChannelRange>();
	
	public ChannelMap()
	{
		this( "New Channel Map" );
	}
	
	public ChannelMap( String name )
	{
		mName = name;
	}
	
	public ChannelMap copyOf()
	{
		ChannelMap map = new ChannelMap( new String( mName ) );
		
		for( ChannelRange range: mRanges )
		{
			map.addRange( range.copyOf() );
		}
		
		return map;
	}

	@XmlTransient
	public boolean isInvalid()
	{
		return mInvalid;
	}

	public String toString()
	{
		return mName + ( mInvalid ? " - Error" : "" );
	}
	
	public List<ChannelRange> getRanges()
	{
		return mRanges;
	}

	@XmlElement( name = "range" )
	public void setRanges( List<ChannelRange> ranges )
	{
		mRanges.clear();
		
		for( ChannelRange range: ranges )
		{
			mRanges.add( range );
		}
		
		validate();
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

	/**
	 * Validates each of the channel ranges for overlap
	 */
	private void validate()
	{
		mInvalid = false;
		
		for( int x = 0; x < mRanges.size(); x++ )
		{
			if( !mRanges.get( x ).isValid() )
			{
				mInvalid = true;
			}
			
			for( int y = x + 1; y <  mRanges.size(); y++ )
			{
				if( mRanges.get( x ).overlaps( mRanges.get( y ) ) )
				{
					mInvalid = true;
				}
			}
		}
	}
	
	
	public void addRange( ChannelRange range )
	{
		mRanges.add( range );
		
		validate();
	}
	
	public void deleteRange( ChannelRange range )
	{
		mRanges.remove( range );
		
		validate();
	}

}
