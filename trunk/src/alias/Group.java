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

import javax.xml.bind.annotation.XmlAttribute;

public class Group
{
	private String mName;
	private ArrayList<Alias> mAliases = new ArrayList<Alias>();
	
	public Group()
	{
	}
	
	public String toString()
	{
		return "Group: " + mName;
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
	
	public ArrayList<Alias> getAlias()
	{
		return mAliases;
	}
	
	public void setAlias( ArrayList<Alias> aliases )
	{
		mAliases = aliases;
	}

	public void addAlias( Alias alias )
	{
		mAliases.add( alias );
	}
	
	public void removeAlias( Alias alias )
	{
		mAliases.remove( alias );
	}
	
	public boolean contians( Alias alias )
	{
		return mAliases.contains( alias );
	}
}
