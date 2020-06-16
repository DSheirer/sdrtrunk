/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * Integer talkgroup identifier range of values with protocol.
 */
public class TalkgroupRange extends AliasID implements Comparable<TalkgroupRange>
{
    private Protocol mProtocol = Protocol.UNKNOWN;
    private int mMinTalkgroup;
    private int mMaxTalkgroup;

    public TalkgroupRange()
    {
        //No arg JAXB constructor
    }

    /**
     * Creates a talkgroup range of from - to talkgroup values (inclusive) for the specified protocol
     * @param protocol for the talkgroup range
     * @param minTalkgroup starting or minimum talkgroup value
     * @param maxTalkgroup ending or maximum talkgroup value
     */
    public TalkgroupRange(Protocol protocol, int minTalkgroup, int maxTalkgroup)
    {
        mProtocol = protocol;
        mMinTalkgroup = minTalkgroup;
        mMaxTalkgroup = maxTalkgroup;
    }

    @Override
    public boolean isAudioIdentifier()
    {
        return false;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "min")
    public int getMinTalkgroup()
    {
        return mMinTalkgroup;
    }

    public void setMinTalkgroup(int minTalkgroup)
    {
        mMinTalkgroup = minTalkgroup;
        updateValueProperty();
    }

    @JacksonXmlProperty(isAttribute = true, localName = "max")
    public int getMaxTalkgroup()
    {
        return mMaxTalkgroup;
    }

    public void setMaxTalkgroup(int maxTalkgroup)
    {
        mMaxTalkgroup = maxTalkgroup;
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
        updateValueProperty();
    }

    @Override
    public boolean isValid()
    {
        if(mProtocol == null || mProtocol == Protocol.UNKNOWN)
        {
            return false;
        }

        TalkgroupFormat talkgroupFormat = TalkgroupFormat.get(mProtocol);
        return talkgroupFormat.getMinimumValidValue() <= mMinTalkgroup &&
               mMinTalkgroup <= talkgroupFormat.getMaximumValidValue() &&
               talkgroupFormat.getMinimumValidValue() <= mMaxTalkgroup &&
               mMaxTalkgroup <= talkgroupFormat.getMaximumValidValue() &&
               mMinTalkgroup < mMaxTalkgroup;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Talkgroup Range:").append(TalkgroupFormatter.format(mProtocol, mMinTalkgroup));
        sb.append(" to ").append(TalkgroupFormatter.format(mProtocol, mMaxTalkgroup));
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
        if(id instanceof TalkgroupRange)
        {
            TalkgroupRange tgid = (TalkgroupRange)id;
            return (getProtocol() == tgid.getProtocol()) &&
                (getMinTalkgroup() == tgid.getMinTalkgroup()) &&
                (getMaxTalkgroup() == tgid.getMaxTalkgroup());
        }

        return false;
    }

    /**
     * Indicates if this talkgroup range contains/includes the talkgroup value
     * @param talkgroupValue
     * @return
     */
    public boolean contains(int talkgroupValue)
    {
        return getMinTalkgroup() <= talkgroupValue && talkgroupValue <= getMaxTalkgroup();
    }

    @Override
    public boolean overlaps(AliasID other)
    {
        return other instanceof TalkgroupRange && overlaps((TalkgroupRange)other);
    }

    /**
     * Indicates if this talkgroup range overlaps the talkgroup range argument.
     * @param talkgroupRange to check for overlap
     * @return true if the ranges overlap
     */
    public boolean overlaps(TalkgroupRange talkgroupRange)
    {
        return contains(talkgroupRange.getMinTalkgroup()) ||
               contains(talkgroupRange.getMaxTalkgroup()) ||
               (getMinTalkgroup() < talkgroupRange.getMinTalkgroup() &&
                   talkgroupRange.getMaxTalkgroup() < getMaxTalkgroup()) ||
               (talkgroupRange.getMinTalkgroup() < getMinTalkgroup() &&
                   getMaxTalkgroup() < talkgroupRange.getMaxTalkgroup());
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public AliasIDType getType()
    {
        return AliasIDType.TALKGROUP_RANGE;
    }

    @Override
    public int compareTo(TalkgroupRange other)
    {
        if(other == null)
        {
            return -1;
        }

        if(getProtocol().equals(other.getProtocol()))
        {
            if(getMinTalkgroup() == other.getMinTalkgroup())
            {
                return Integer.compare(getMaxTalkgroup(), other.getMaxTalkgroup());
            }
            else
            {
                return Integer.compare(getMinTalkgroup(), other.getMinTalkgroup());
            }
        }
        else
        {
            return getProtocol().compareTo(other.getProtocol());
        }
    }
}
