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
package source.tuner.configuration;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import source.tuner.TunerType;

/**
 * Abstract class to hold a named configuration for a specific type of tuner
 */
@XmlType( name = "tuner_configuration" )
public abstract class TunerConfiguration
{
	protected String mName;
	protected String mUniqueID;
	protected boolean mAssigned;

	/**
	 * Default constructor to support JAXB
	 */
	public TunerConfiguration()
	{
	}
	
	/**
	 * Normal constructor
	 */
	public TunerConfiguration( String uniqueID, String name )
	{
		mUniqueID = uniqueID;
		mName = name;
	}
	
	public String toString()
	{
		return mName;
	}

	@XmlAttribute( name = "name" )
	public String getName()
	{
		return mName;
	}
	
	public void setName( String name )
	{
		mName = name;
	}

	@XmlAttribute( name = "unique_id" )
	public String getUniqueID()
	{
		return mUniqueID;
	}
	
	public void setUniqueID( String id )
	{
		mUniqueID = id;;
	}
	
	@XmlAttribute( name = "assigned" )
	public boolean isAssigned()
	{
		return mAssigned;
	}
	
	public void setAssigned( boolean assigned )
	{
		mAssigned = assigned;
	}
	
	@XmlAttribute( name = "tuner_type" )
	public abstract TunerType getTunerType();
}
