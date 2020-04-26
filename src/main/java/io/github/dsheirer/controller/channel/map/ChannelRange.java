/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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
package io.github.dsheirer.controller.channel.map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.text.DecimalFormat;

@JacksonXmlRootElement(localName = "range")
public class ChannelRange
{
    private static final DecimalFormat FREQUENCY_FORMATTER = new DecimalFormat("#.0000");

    private int mFirst = 1;
    private int mLast = 1023;
    private int mBase = 450000000;
    private int mSize = 12500;

    private boolean mOverlapping = false;

    public ChannelRange()
    {
    }

    public ChannelRange(int first, int last, int base, int size)
    {
        mFirst = first;
        mLast = last;
        mBase = base;
        mSize = size;
    }

    public ChannelRange copyOf()
    {
        return new ChannelRange(mFirst, mLast, mBase, mSize);
    }

    @JsonIgnore
    public String getDescription()
    {
        StringBuilder sb = new StringBuilder();

        if(isValid())
        {
            sb.append("First: ");
            sb.append(mFirst).append("=");

            long frequency = getFrequency(mFirst);

            sb.append(FREQUENCY_FORMATTER.format((double)frequency / 1E6D));
            sb.append("  Last: ");
            sb.append(mLast).append("=");

            frequency = getFrequency(mLast);
            sb.append(FREQUENCY_FORMATTER.format((double)frequency / 1E6D));
            sb.append(" MHz");
        }
        else
        {
            sb.append("First channel must be smaller than last channel");
        }

        if(isOverlapping())
        {
            if(sb.length() > 0)
            {
                sb.append(", ");
            }

            sb.append("Overlap!");
        }

        return sb.toString();
    }

    @JsonIgnore
    public boolean isValid()
    {
        return mFirst < mLast;
    }

    public boolean overlaps(ChannelRange other)
    {
        return (mFirst <= other.mFirst && other.mFirst <= mLast) ||
            (mFirst <= other.mLast && other.mLast <= mLast) ||
            (mFirst <= other.mFirst && other.mLast <= mLast) ||
            (other.mFirst <= mFirst && mLast <= other.mLast);
    }

    @JsonIgnore
    public boolean isOverlapping()
    {
        return mOverlapping;
    }

    public void setOverlapping(boolean overlapping)
    {
        mOverlapping = overlapping;
    }

    public boolean hasChannel(int channel)
    {
        return mFirst <= channel && channel <= mLast;
    }

    public long getFrequency(int channel)
    {
        if(hasChannel(channel))
        {
            return mBase + ((channel - mFirst) * mSize);
        }
        else
        {
            return 0;
        }
    }

    @JacksonXmlProperty(isAttribute = true, localName = "first")
    public int getFirstChannelNumber()
    {
        return mFirst;
    }

    public void setFirstChannelNumber(int first)
    {
        mFirst = first;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "last")
    public void setLastChannelNumber(int last)
    {
        mLast = last;
    }

    public int getLastChannelNumber()
    {
        return mLast;
    }

    /**
     * Sets the base frequency
     *
     * @param base frequency in hertz
     */
    @JacksonXmlProperty(isAttribute = true, localName = "base")
    public void setBase(int base)
    {
        mBase = base;
    }

    public int getBase()
    {
        return mBase;
    }

    /**
     * Sets the channel size
     *
     * @param size in hertz
     */
    @JacksonXmlProperty(isAttribute = true, localName = "size")
    public void setSize(int size)
    {
        mSize = size;
    }

    public int getSize()
    {
        return mSize;
    }
}
