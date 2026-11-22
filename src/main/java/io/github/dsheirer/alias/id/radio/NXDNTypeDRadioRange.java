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

package io.github.dsheirer.alias.id.radio;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.alias.id.INXDNTypeDRange;
import io.github.dsheirer.preference.identifier.talkgroup.NXDNTalkgroupFormatter;
import io.github.dsheirer.protocol.Protocol;

/**
 * NXDN Type-D radio range for a single home repeater.  The min and max values are the over-the-air 16-bit identifiers
 * that combine the home repeater and radio.
 */
public class NXDNTypeDRadioRange extends RadioRange implements INXDNTypeDRange
{
    public NXDNTypeDRadioRange()
    {
        //No-arg JAXB constructor
        setProtocol(Protocol.NXDN);
    }

    /**
     * Constructs an instance
     * @param homeRepeater for the range
     * @param minRadio value, without the home repeater
     * @param maxRadio value, without the home repeater
     */
    public NXDNTypeDRadioRange(int homeRepeater, int minRadio, int maxRadio)
    {
        super(Protocol.NXDN, NXDNTalkgroupFormatter.toTypeDValue(homeRepeater, minRadio),
                NXDNTalkgroupFormatter.toTypeDValue(homeRepeater, maxRadio));
    }

    @Override
    public AliasIDType getType()
    {
        return AliasIDType.NXDN_TYPE_D_RADIO_ID_RANGE;
    }

    @JsonIgnore
    @Override
    public int getHomeRepeater()
    {
        return NXDNTalkgroupFormatter.getTypeDHomeRepeater(getMinRadio());
    }

    @Override
    public void setHomeRepeater(int homeRepeater)
    {
        int min = getMinIdentifier();
        int max = getMaxIdentifier();
        setMinRadio(NXDNTalkgroupFormatter.toTypeDValue(homeRepeater, min));
        setMaxRadio(NXDNTalkgroupFormatter.toTypeDValue(homeRepeater, max));
    }

    @JsonIgnore
    @Override
    public int getMinIdentifier()
    {
        return NXDNTalkgroupFormatter.getTypeDIdentifier(getMinRadio());
    }

    @Override
    public void setMinIdentifier(int identifier)
    {
        setMinRadio(NXDNTalkgroupFormatter.toTypeDValue(getHomeRepeater(), identifier));
    }

    @JsonIgnore
    @Override
    public int getMaxIdentifier()
    {
        return NXDNTalkgroupFormatter.getTypeDIdentifier(getMaxRadio());
    }

    @Override
    public void setMaxIdentifier(int identifier)
    {
        setMaxRadio(NXDNTalkgroupFormatter.toTypeDValue(getHomeRepeater(), identifier));
    }

    /**
     * Valid when the base range is valid and both ends of the range use the same home repeater.
     */
    @Override
    public boolean isValid()
    {
        return super.isValid() && getHomeRepeater() == NXDNTalkgroupFormatter.getTypeDHomeRepeater(getMaxRadio());
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Type-D Radio ID Range:").append(NXDNTalkgroupFormatter.toTypeD(getHomeRepeater(), getMinIdentifier(), true));
        sb.append(" to ").append(NXDNTalkgroupFormatter.toTypeD(getHomeRepeater(), getMaxIdentifier(), true));
        sb.append(" Protocol:").append(getProtocol());

        if(!isValid())
        {
            sb.append(" **NOT VALID**");
        }

        return sb.toString();
    }
}
