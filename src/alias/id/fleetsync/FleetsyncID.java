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
package alias.id.fleetsync;

import javax.xml.bind.annotation.XmlAttribute;

import alias.id.AliasID;
import alias.id.AliasIDType;


public class FleetsyncID extends AliasID
{
	private String mID;
	
	public FleetsyncID()
	{
	}

	@XmlAttribute
	public String getIdent()
	{
		return mID;
	}

	public void setIdent( String ident )
	{
		mID = ident;
	}

	@Override
	public boolean isValid()
	{
		return mID != null;
	}

	public String toString()
	{
		return "Fleetsync: " + mID;
	}

	@Override
    public boolean matches( AliasID id )
    {
		boolean retVal = false;
		
		if( mID != null && id instanceof FleetsyncID )
		{
			FleetsyncID tgid = (FleetsyncID)id;

			//Create a pattern - replace * wildcards with regex single-char wildcard
			String pattern = mID.replace( "*", ".?" );
			
			retVal = tgid.getIdent().matches( pattern );
		}
		
	    return retVal;
    }

	@Override
    public AliasIDType getType()
    {
	    return AliasIDType.FLEETSYNC;
    }
}
