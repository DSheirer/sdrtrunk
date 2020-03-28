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
package io.github.dsheirer.alias.id.legacy.uniqueID;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;

public class UniqueID extends AliasID
{
    private int mUid;

    public UniqueID()
    {
    }

    @Override
    public boolean isAudioIdentifier()
    {
        return false;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "uid")
    public int getUid()
    {
        return mUid;
    }

    public void setUid(int uid)
    {
        this.mUid = uid;
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    public String toString()
    {
        return "UniqueID: " + mUid + " **INVALID - USE TALKGROUP INSTEAD";
    }

    @Override
    public boolean matches(AliasID id)
    {
        boolean retVal = false;

        if(id instanceof UniqueID)
        {
            UniqueID uid = (UniqueID)id;

            retVal = mUid == uid.getUid();
        }

        return retVal;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public AliasIDType getType()
    {
        return AliasIDType.LTR_NET_UID;
    }
}
