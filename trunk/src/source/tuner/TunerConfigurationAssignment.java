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

import javax.xml.bind.annotation.XmlAttribute;

public class TunerConfigurationAssignment
{
	private TunerType mTunerType;
	private String mUniqueID;
	private String mTunerConfigurationName;
	
	public TunerConfigurationAssignment()
	{
	}

	@XmlAttribute( name = "type" )
	public TunerType getTunerType()
	{
		return mTunerType;
	}

	public void setTunerType( TunerType tunerType )
	{
		mTunerType = tunerType;
	}

	@XmlAttribute( name = "id" )
	public String getUniqueID()
	{
		return mUniqueID;
	}

	public void setUniqueID( String uniqueID )
	{
		mUniqueID = uniqueID;
	}

	@XmlAttribute( name = "configuration_name" )
	public String getTunerConfigurationName()
	{
		return mTunerConfigurationName;
	}

	public void setTunerConfigurationName( String tunerConfigurationName )
	{
		mTunerConfigurationName = tunerConfigurationName;
	}
}
