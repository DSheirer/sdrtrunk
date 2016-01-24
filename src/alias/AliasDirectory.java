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

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

@XmlSeeAlso( { AliasList.class} )
@XmlRootElement( name = "alias_directory" )
public class AliasDirectory
{
	private ArrayList<AliasList> mAliasList = new ArrayList<AliasList>();
	
	public AliasDirectory()
	{
	}
	
	public void refresh()
	{
		for( AliasList list: mAliasList )
		{
			list.update();
		}
	}
	
	public void clearAliasLists()
	{
		mAliasList.clear();
	}

	@XmlElement( name = "alias_list" )
	public ArrayList<AliasList> getAliasList()
	{
		return mAliasList;
	}
	
	public void setAliasList( ArrayList<AliasList> lists )
	{
		mAliasList = lists;
		
		if( mAliasList == null )
		{
			mAliasList = new ArrayList<AliasList>();
		}
	}
	
	public void addAliasList( AliasList list )
	{
		mAliasList.add( list );
	}
	
	public void removeAliasList( AliasList list )
	{
		mAliasList.remove( list );
	}
	
	/**
	 * Gets the named alias list, or returns an empty alias list
	 */
	public AliasList getAliasList( String name )
	{
		AliasList retVal = new AliasList();
		
		for( AliasList list: mAliasList )
		{
			if( list.getName().equalsIgnoreCase( name ) )
			{
				return list;
			}
		}
		
		return retVal;
	}
}
