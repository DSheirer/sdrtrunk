/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2015 Dennis Sheirer
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
package alias.id.lojack;

import javax.xml.bind.annotation.XmlAttribute;

import module.decode.lj1200.LJ1200Message;
import module.decode.lj1200.LJ1200Message.Function;
import alias.id.AliasID;
import alias.id.AliasIDType;

public class LoJackFunctionAndID extends AliasID
{
	private LJ1200Message.Function mFunction = Function.F0_UNKNOWN;
	private String mID = "*****";
	
	public LoJackFunctionAndID()
	{
	}

	@XmlAttribute( name="id" )
	public String getID()
	{
		return mID;
	}

	public void setID( String id )
	{
		mID = id;
	}

	@XmlAttribute( name="function" )
	public Function getFunction()
	{
		return mFunction;
	}

	public void setFunction( Function function )
	{
		mFunction = function;
	}

	@Override
	public boolean isValid()
	{
		return mFunction != null && mID != null;
	}

	public String toString()
	{
		return "LoJack FUNC: " + mFunction.getLabel() + " ID:" + ( mID == null ? "" : mID );
	}

	/**
	 * Indicates if the function and id combination match this alias id
	 */
	public boolean matches( Function function, String id )
	{
		return mFunction == function &&
			   mID != null && 
			   id != null &&
			   id.matches( mID.replace( "*", ".?" ) );
	}

	@Override
    public boolean matches( AliasID id )
    {
		boolean retVal = false;
		
		if( mID != null && id instanceof LoJackFunctionAndID )
		{
			LoJackFunctionAndID otherLojack = (LoJackFunctionAndID)id;

			if( otherLojack.getFunction() == mFunction )
			{
				//Create a pattern - replace * wildcards with regex single-char wildcard
				String pattern = mID.replace( "*", ".?" );
				
				retVal = otherLojack.getID().matches( pattern );
			}
		}
		
	    return retVal;
    }

	@Override
    public AliasIDType getType()
    {
	    return AliasIDType.LOJACK;
    }
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ( ( mFunction == null ) ? 0 : mFunction.hashCode() );
		result = prime * result + ( ( mID == null ) ? 0 : mID.hashCode() );
		return result;
	}

	@Override
	public boolean equals( Object obj )
	{
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		LoJackFunctionAndID other = (LoJackFunctionAndID) obj;
		if ( mFunction != other.mFunction )
			return false;
		if ( mID == null )
		{
			if ( other.mID != null )
				return false;
		} else if ( !mID.equals( other.mID ) )
			return false;
		return true;
	}
}
