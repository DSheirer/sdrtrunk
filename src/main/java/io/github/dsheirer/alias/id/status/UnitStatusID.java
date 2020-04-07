/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */
package io.github.dsheirer.alias.id.status;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;

/**
 * Unit Status
 */
public class UnitStatusID extends AliasID
{
    private int mStatus;

    public UnitStatusID()
    {
    }

    @Override
    public boolean isAudioIdentifier()
    {
        return false;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "status")
    public int getStatus()
    {
        return mStatus;
    }

    public void setStatus(int status)
    {
        mStatus = status;
        updateValueProperty();
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Unit Status: ").append(String.format("%03d", mStatus));
        return sb.toString();
    }

    @Override
    public boolean matches(AliasID id)
    {
        boolean retVal = false;

        if(id instanceof UnitStatusID)
        {
            UnitStatusID userStatusId = (UnitStatusID)id;

            retVal = (mStatus == userStatusId.getStatus());
        }

        return retVal;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public AliasIDType getType()
    {
        return AliasIDType.UNIT_STATUS;
    }
}
