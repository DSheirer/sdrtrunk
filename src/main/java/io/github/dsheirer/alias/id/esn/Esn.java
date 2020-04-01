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
package io.github.dsheirer.alias.id.esn;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;

public class Esn extends AliasID
{
	private String mEsn;
	
	public Esn()
	{
	}

	@Override
	public boolean isAudioIdentifier()
	{
		return false;
	}

	@JacksonXmlProperty(isAttribute = true, localName = "esn")
	public String getEsn()
	{
		return mEsn;
	}

	@Override
	public boolean isValid()
	{
		return mEsn != null;
	}

	public void setEsn(String esn )
	{
		mEsn = esn;
		updateValueProperty();
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("ESN: ").append(mEsn != null ? mEsn : "(empty)");
		if(!isValid())
		{
			sb.append(" **NOT VALID**");
		}
		return sb.toString();
	}

	@Override
    public boolean matches( AliasID id )
    {
		boolean retVal = false;
		
		if( mEsn != null && id instanceof Esn )
		{
			Esn esn = (Esn)id;

			//Create a pattern - replace * wildcards with regex single-char wildcard
			String pattern = mEsn.replace( "*", ".?" );
			
			retVal = esn.getEsn().matches( pattern );
		}
		
	    return retVal;
    }

	@JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
	@Override
    public AliasIDType getType()
    {
	    return AliasIDType.ESN;
    }
}
