/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.alias.id.talkgroup;


import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.protocol.Protocol;

/**
 * Integer talkgroup identifier with protocol.
 */
public class Talkgroup extends AliasID
{
    private Protocol mProtocol;
    private int mValue;

    public Talkgroup()
    {
        //No arg JAXB constructor
    }

    public Talkgroup(Protocol protocol, int value)
    {
        mProtocol = protocol;
        mValue = value;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "value")
    public int getValue()
    {
        return mValue;
    }

    public void setValue(int value)
    {
        mValue = value;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "protocol")
    public Protocol getProtocol()
    {
        return mProtocol;
    }

    public void setProtocol(Protocol protocol)
    {
        mProtocol = protocol;
    }

    @Override
    public boolean isValid()
    {
        return mValue != 0;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Talkgroup:").append(mValue).append(" Protocol:").append((mProtocol != null ? mProtocol : "(unspecified)"));

        if(!isValid())
        {
            sb.append(" **NOT VALID**");
        }

        return sb.toString();
    }

    @Override
    public boolean matches(AliasID id)
    {
        if(id instanceof Talkgroup)
        {
            Talkgroup tgid = (Talkgroup)id;
            return (getProtocol() == tgid.getProtocol()) && (getValue() == tgid.getValue());
        }

        return false;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public AliasIDType getType()
    {
        return AliasIDType.TALKGROUP;
    }
}
