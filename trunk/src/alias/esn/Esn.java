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
package alias.esn;

import javax.xml.bind.annotation.XmlAttribute;

import alias.AliasID;
import alias.AliasIDType;

public class Esn extends AliasID
{
	private String mEsn;
	
	public Esn()
	{
	}

	@XmlAttribute
	public String getEsn()
	{
		return mEsn;
	}

	public void setEsn( String esn )
	{
		mEsn = esn;
	}
	
	public String toString()
	{
		return "ESN: " + mEsn;
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

	@Override
    public AliasIDType getType()
    {
	    return AliasIDType.ESN;
    }
}
