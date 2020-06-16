/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.alias.id.radio;


import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.protocol.Protocol;

/**
 * Integer radio identifier range of values with protocol.
 */
public class RadioRange extends AliasID implements Comparable<RadioRange>
{
    private Protocol mProtocol = Protocol.UNKNOWN;
    private int mMinRadio;
    private int mMaxRadio;

    public RadioRange()
    {
        //No arg JAXB constructor
    }

    @Override
    public boolean isAudioIdentifier()
    {
        return false;
    }

    /**
     * Creates a radio range of from - to radio values (inclusive) for the specified protocol
     * @param protocol for the radio range
     * @param minRadio starting or minimum radio value
     * @param maxRadio ending or maximum radio value
     */
    public RadioRange(Protocol protocol, int minRadio, int maxRadio)
    {
        mProtocol = protocol;
        mMinRadio = minRadio;
        mMaxRadio = maxRadio;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "min")
    public int getMinRadio()
    {
        return mMinRadio;
    }

    public void setMinRadio(int minRadio)
    {
        mMinRadio = minRadio;
        updateValueProperty();
    }

    @JacksonXmlProperty(isAttribute = true, localName = "max")
    public int getMaxRadio()
    {
        return mMaxRadio;
    }

    public void setMaxRadio(int maxRadio)
    {
        mMaxRadio = maxRadio;
        updateValueProperty();
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
        if(mProtocol == null || mProtocol == Protocol.UNKNOWN)
        {
            return false;
        }

        RadioFormat radioFormat = RadioFormat.get(mProtocol);
        return radioFormat.getMinimumValidValue() <= mMinRadio &&
               mMinRadio <= radioFormat.getMaximumValidValue() &&
               radioFormat.getMinimumValidValue() <= mMaxRadio &&
               mMaxRadio <= radioFormat.getMaximumValidValue() &&
               mMinRadio < mMaxRadio;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Radio ID Range:").append(RadioFormatter.format(mProtocol, mMinRadio));
        sb.append(" to ").append(RadioFormatter.format(mProtocol, mMaxRadio));
        sb.append(" Protocol:").append(mProtocol != null ? mProtocol : "unspecified");

        if(!isValid())
        {
            sb.append(" **NOT VALID**");
        }

        return sb.toString();
    }

    @Override
    public boolean matches(AliasID id)
    {
        if(id instanceof RadioRange)
        {
            RadioRange tgid = (RadioRange)id;
            return (getProtocol() == tgid.getProtocol()) &&
                (getMinRadio() == tgid.getMinRadio()) &&
                (getMaxRadio() == tgid.getMaxRadio());
        }

        return false;
    }

    /**
     * Indicates if this radio range contains/includes the value
     * @param radioValue
     * @return
     */
    public boolean contains(int radioValue)
    {
        return getMinRadio() <= radioValue && radioValue <= getMaxRadio();
    }

    @Override
    public boolean overlaps(AliasID other)
    {
        return other instanceof RadioRange && overlaps((RadioRange)other);
    }


    /**
     * Indicates if this talkgroup range overlaps the talkgroup range argument.
     * @param radioRange to check for overlap
     * @return true if the ranges overlap
     */
    public boolean overlaps(RadioRange radioRange)
    {
        return contains(radioRange.getMinRadio()) ||
               contains(radioRange.getMaxRadio()) ||
               (getMinRadio() < radioRange.getMinRadio() &&
                   radioRange.getMaxRadio() < getMaxRadio()) ||
               (radioRange.getMinRadio() < getMinRadio() &&
                   getMaxRadio() < radioRange.getMaxRadio());
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public AliasIDType getType()
    {
        return AliasIDType.RADIO_ID_RANGE;
    }


    @Override
    public int compareTo(RadioRange other)
    {
        if(other == null)
        {
            return -1;
        }

        if(getProtocol().equals(other.getProtocol()))
        {
            if(getMinRadio() == other.getMinRadio())
            {
                return Integer.compare(getMaxRadio(), other.getMaxRadio());
            }
            else
            {
                return Integer.compare(getMinRadio(), other.getMinRadio());
            }
        }
        else
        {
            return getProtocol().compareTo(other.getProtocol());
        }
    }
}
