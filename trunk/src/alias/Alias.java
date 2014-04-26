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
package alias;

import java.awt.Color;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAttribute;

import audio.AudioType;

public class Alias
{
	private String mName;
	private int mColor;
	private String mIconName;
	private ArrayList<AliasID> mAliasIDs = new ArrayList<AliasID>();
	
	public Alias()
	{
	}
	
	public String toString()
	{
		return "Alias: " + mName;
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

	@XmlAttribute
	public int getColor()
	{
		return mColor;
	}

	public void setColor( int color )
	{
		mColor = color;
	}

	public Color getMapColor()
	{
		return new Color( mColor );
	}
	
	@XmlAttribute
	public String getIconName()
	{
		return mIconName;
	}
	
	public void setIconName( String iconName )
	{
		mIconName = iconName;
	}
	
	public ArrayList<AliasID> getId()
	{
		return mAliasIDs;
	}
	
	public void setId( ArrayList<AliasID> id )
	{
		mAliasIDs = id;
	}
	
	public void addAliasID( AliasID id )
	{
		mAliasIDs.add( id );
	}
	
	public void removeAliasID( AliasID id )
	{
		mAliasIDs.remove( id );
	}
	
	public boolean hasAudioType()
	{
		for( AliasID id: mAliasIDs )
		{
			if( id.hasAudioType() )
			{
				return true;
			}
		}
		
		return false;
	}
	
	public AudioType getAudioType()
	{
		for( AliasID id: mAliasIDs )
		{
			if( id.hasAudioType() )
			{
				return id.getAudioType();
			}
		}
		
		return AudioType.NORMAL;
	}
}
