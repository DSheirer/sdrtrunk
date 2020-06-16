/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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
package io.github.dsheirer.controller.channel.map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.util.Callback;

import java.text.DecimalFormat;

@JacksonXmlRootElement(localName = "range")
public class ChannelRange
{
    private static final DecimalFormat FREQUENCY_FORMATTER = new DecimalFormat("#.0000");

    private IntegerProperty mFirstChannel = new SimpleIntegerProperty(1);
    private IntegerProperty mLastChannel = new SimpleIntegerProperty(1023);
    private IntegerProperty mBaseFrequency = new SimpleIntegerProperty(450000000);
    private IntegerProperty mStepSize = new SimpleIntegerProperty(12500);
    private BooleanProperty mValid = new SimpleBooleanProperty();
    private BooleanProperty mOverlapping = new SimpleBooleanProperty();

    public ChannelRange()
    {
        //Bind valid to first channel is less than last channel
        mValid.bind(Bindings.lessThan(mFirstChannel, mLastChannel));
    }

    public ChannelRange(int first, int last, int base, int sizeProperty)
    {
        this();
        mFirstChannel.set(first);
        mLastChannel.set(last);
        mBaseFrequency.set(base);
        mStepSize.set(sizeProperty);
    }

    public ChannelRange copyOf()
    {
        return new ChannelRange(mFirstChannel.get(), mLastChannel.get(), mBaseFrequency.get(), mStepSize.get());
    }

    /**
     * Indicates if this channel range overlaps with another channel range
     */
    public BooleanProperty overlappingProperty()
    {
        return mOverlapping;
    }

    public BooleanProperty validProperty()
    {
        return mValid;
    }

    public IntegerProperty firstChannelProperty()
    {
        return mFirstChannel;
    }

    public IntegerProperty lastChannelProperty()
    {
        return mLastChannel;
    }

    public IntegerProperty baseFrequencyProperty()
    {
        return mBaseFrequency;
    }

    public IntegerProperty stepSizeProperty()
    {
        return mStepSize;
    }

    @JsonIgnore
    public String getDescription()
    {
        StringBuilder sb = new StringBuilder();

        if(isValid())
        {
            sb.append("First: ");
            sb.append(mFirstChannel.get()).append("=");

            long frequency = getFrequency(mFirstChannel.get());

            sb.append(FREQUENCY_FORMATTER.format((double)frequency / 1E6D));
            sb.append("  Last: ");
            sb.append(mLastChannel.get()).append("=");

            frequency = getFrequency(mLastChannel.get());
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
        return mValid.get();
    }

    public boolean overlaps(ChannelRange other)
    {
        return (mFirstChannel.get() <= other.mFirstChannel.get() && other.mFirstChannel.get() <= mLastChannel.get()) ||
            (mFirstChannel.get() <= other.mLastChannel.get() && other.mLastChannel.get() <= mLastChannel.get()) ||
            (mFirstChannel.get() <= other.mFirstChannel.get() && other.mLastChannel.get() <= mLastChannel.get()) ||
            (other.mFirstChannel.get() <= mFirstChannel.get() && mLastChannel.get() <= other.mLastChannel.get());
    }

    @JsonIgnore
    public boolean isOverlapping()
    {
        return mOverlapping.get();
    }

    public void setOverlapping(boolean overlapping)
    {
        mOverlapping.set(overlapping);
    }

    public boolean hasChannel(int channel)
    {
        return mFirstChannel.get() <= channel && channel <= mLastChannel.get();
    }

    public long getFrequency(int channel)
    {
        if(hasChannel(channel))
        {
            return mBaseFrequency.get() + ((channel - mFirstChannel.get()) * mStepSize.get());
        }
        else
        {
            return 0;
        }
    }

    @JacksonXmlProperty(isAttribute = true, localName = "first")
    public int getFirstChannelNumber()
    {
        return mFirstChannel.get();
    }

    public void setFirstChannelNumber(int first)
    {
        mFirstChannel.set(first);;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "last")
    public void setLastChannelNumber(int last)
    {
        mLastChannel.set(last);;
    }

    public int getLastChannelNumber()
    {
        return mLastChannel.get();
    }

    /**
     * Sets the base frequency
     *
     * @param baseFrequency frequency in hertz
     */
    @JacksonXmlProperty(isAttribute = true, localName = "base")
    public void setBaseFrequency(int baseFrequency)
    {
        this.mBaseFrequency.set(baseFrequency);
    }

    public int getBaseFrequency()
    {
        return mBaseFrequency.get();
    }

    /**
     * Sets the channel size
     *
     * @param stepSize in hertz
     */
    @JacksonXmlProperty(isAttribute = true, localName = "size")
    public void setStepSize(int stepSize)
    {
        this.mStepSize.set(stepSize);
    }

    public int getStepSize()
    {
        return mStepSize.get();
    }

    /**
     * Creates an observable property extractor for use with obserable lists to detect changes internal to this object.
     */
    public static Callback<ChannelRange,Observable[]> extractor()
    {
        return (ChannelRange c) -> new Observable[] {c.firstChannelProperty(), c.lastChannelProperty(),
            c.baseFrequencyProperty(), c.stepSizeProperty(), c.validProperty(), c.overlappingProperty()};
    }
}
