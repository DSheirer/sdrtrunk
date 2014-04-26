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
package alias.mobileID;

import javax.xml.bind.annotation.XmlAttribute;

import alias.AliasID;
import alias.AliasIDType;

/**
 * Mobile ID Number
 */
public class Min extends AliasID
{
	private String mMin;
	
	public Min()
	{
	}

	@XmlAttribute
	public String getMin()
	{
		return mMin;
	}

	public void setMin( String min )
	{
		mMin = min;
	}
	
	public String toString()
	{
		return "MIN: " + mMin;
	}

	@Override
    public boolean matches( AliasID id )
    {
		boolean retVal = false;
		
		if( mMin != null && id instanceof Min )
		{
			Min min = (Min)id;

			//Create a pattern - replace * wildcards with regex single-char wildcard
			String pattern = mMin.replace( "*", ".?" );
			
			retVal = min.getMin().matches( pattern );
		}
		
	    return retVal;
    }

	@Override
    public AliasIDType getType()
    {
	    return AliasIDType.MIN;
    }
}
