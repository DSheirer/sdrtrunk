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
package record.config;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import record.RecorderType;
import controller.config.Configuration;

@XmlRootElement( name = "record_configuration" )
public class RecordConfiguration extends Configuration
{
	private List<RecorderType> mRecorders = new ArrayList<>();
	
	public RecordConfiguration()
	{
	}

	@XmlElement( name = "recorder" )
	public List<RecorderType> getRecorders()
	{
		return mRecorders;
	}
	
	public void setRecorders( List<RecorderType> recorders )
	{
		mRecorders = recorders;
	}
	
	public void addRecorder( RecorderType recorder )
	{
		mRecorders.add( recorder );
	}

	public void clearRecorders()
	{
		mRecorders.clear();
	}
}
