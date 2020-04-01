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
package io.github.dsheirer.alias.id.lojack;


import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.module.decode.lj1200.LJ1200Message;

public class LoJackFunctionAndID extends AliasID
{
    private LJ1200Message.Function mFunction = LJ1200Message.Function.F0_UNKNOWN;
    private String mID = null;

    public LoJackFunctionAndID()
    {
    }

    @Override
    public boolean isAudioIdentifier()
    {
        return false;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "id")
    public String getID()
    {
        return mID;
    }

    public void setID(String id)
    {
        mID = id;
        updateValueProperty();
    }

    @JacksonXmlProperty(isAttribute = true, localName = "function")
    public LJ1200Message.Function getFunction()
    {
        return mFunction;
    }

    public void setFunction(LJ1200Message.Function function)
    {
        mFunction = function;
        updateValueProperty();
    }

    @Override
    public boolean isValid()
    {
        return mFunction != null && mID != null;
    }

    public String toString()
    {
        return "LoJack FUNCTION: " + mFunction.getLabel() +
            " ID:" + (mID == null ? "" : mID) + (isValid() ? "" : " **NOT VALID**");
    }

    /**
     * Indicates if the function and id combination match this alias id
     */
    public boolean matches(LJ1200Message.Function function, String id)
    {
        return mFunction == function &&
            mID != null &&
            id != null &&
            id.matches(mID.replace("*", ".?"));
    }

    @Override
    public boolean matches(AliasID id)
    {
        boolean retVal = false;

        if(mID != null && id instanceof LoJackFunctionAndID)
        {
            LoJackFunctionAndID otherLojack = (LoJackFunctionAndID)id;

            if(otherLojack.getFunction() == mFunction)
            {
                //Create a pattern - replace * wildcards with regex single-char wildcard
                String pattern = mID.replace("*", ".?");

                retVal = otherLojack.getID().matches(pattern);
            }
        }

        return retVal;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
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
            + ((mFunction == null) ? 0 : mFunction.hashCode());
        result = prime * result + ((mID == null) ? 0 : mID.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
        {
            return true;
        }
        if(obj == null)
        {
            return false;
        }
        if(getClass() != obj.getClass())
        {
            return false;
        }
        LoJackFunctionAndID other = (LoJackFunctionAndID)obj;
        if(mFunction != other.mFunction)
        {
            return false;
        }
        if(mID == null)
        {
            if(other.mID != null)
            {
                return false;
            }
        }
        else if(!mID.equals(other.mID))
        {
            return false;
        }
        return true;
    }
}
