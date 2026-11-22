/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.alias.id.talkgroup;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.alias.id.INXDNTypeD;
import io.github.dsheirer.preference.identifier.talkgroup.NXDNTalkgroupFormatter;
import io.github.dsheirer.protocol.Protocol;

/**
 * NXDN Type-D talkgroup.  The value is the over-the-air 16-bit identifier that combines the home repeater and talkgroup
 * so that it matches the value of an NXDN Type-D talkgroup identifier.
 */
public class NXDNTypeDTalkgroup extends Talkgroup implements INXDNTypeD
{
    public NXDNTypeDTalkgroup()
    {
        //No-arg JAXB constructor
        setProtocol(Protocol.NXDN);
    }

    /**
     * Constructs an instance
     * @param homeRepeater for the talkgroup
     * @param talkgroup value, without the home repeater
     */
    public NXDNTypeDTalkgroup(int homeRepeater, int talkgroup)
    {
        super(Protocol.NXDN, NXDNTalkgroupFormatter.toTypeDValue(homeRepeater, talkgroup));
    }

    @Override
    public AliasIDType getType()
    {
        return AliasIDType.NXDN_TYPE_D_TALKGROUP;
    }

    @JsonIgnore
    @Override
    public int getHomeRepeater()
    {
        return NXDNTalkgroupFormatter.getTypeDHomeRepeater(getValue());
    }

    @Override
    public void setHomeRepeater(int homeRepeater)
    {
        setValue(NXDNTalkgroupFormatter.toTypeDValue(homeRepeater, getIdentifier()));
    }

    @JsonIgnore
    @Override
    public int getIdentifier()
    {
        return NXDNTalkgroupFormatter.getTypeDIdentifier(getValue());
    }

    @Override
    public void setIdentifier(int identifier)
    {
        setValue(NXDNTalkgroupFormatter.toTypeDValue(getHomeRepeater(), identifier));
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Type-D Talkgroup:").append(NXDNTalkgroupFormatter.toTypeD(getHomeRepeater(), getIdentifier(), true));
        sb.append(" Protocol:").append(getProtocol());

        if(!isValid())
        {
            sb.append(" **NOT VALID**");
        }

        return sb.toString();
    }
}
