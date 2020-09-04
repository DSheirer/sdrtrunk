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

package io.github.dsheirer.module.decode.dmr.channel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.util.Callback;

/**
 * Maps a timeslot number to a pair of channel frequency values
 */
public class TimeslotFrequency
{
    private IntegerProperty mNumberProperty = new SimpleIntegerProperty();
    private LongProperty mDownlinkFrequencyProperty = new SimpleLongProperty();
    private LongProperty mUplinkFrequencyProperty = new SimpleLongProperty();
    private DoubleProperty mDownlinkMhzProperty = new SimpleDoubleProperty();
    private DoubleProperty mUplinkMhzProperty = new SimpleDoubleProperty();

    /**
     * Constructs an instance
     */
    public TimeslotFrequency()
    {
    }

    /**
     * Creates a deep copy of this instance
     */
    public TimeslotFrequency copy()
    {
        TimeslotFrequency copy = new TimeslotFrequency();
        copy.setNumber(getNumber());
        copy.setDownlinkFrequency(getDownlinkFrequency());
        copy.setUplinkFrequency(getUplinkFrequency());
        return copy;
    }

    /**
     * Logical Slot Number property
     */
    @JsonIgnore
    public IntegerProperty getNumberProperty()
    {
        return mNumberProperty;
    }

    /**
     * Downlink Frequency property
     */
    @JsonIgnore
    public LongProperty downlinkFrequencyPropertyProperty()
    {
        return mDownlinkFrequencyProperty;
    }

    /**
     * Uplink Frequency property
     */
    @JsonIgnore
    public LongProperty uplinkFrequencyProperty()
    {
        return mUplinkFrequencyProperty;
    }

    @JsonIgnore
    public DoubleProperty getDownlinkMHz()
    {
        return mDownlinkMhzProperty;
    }

    @JsonIgnore
    public DoubleProperty getUplinkMHz()
    {
        return mUplinkMhzProperty;
    }

    /**
     * Logical slot number (LSN) as a 1-index based counter
     */
    @JacksonXmlProperty(isAttribute = true, localName = "lsn")
    public int getNumber()
    {
        return mNumberProperty.get();
    }

    /**
     * Sets the logical slot number (LSN) as a 1-index start
     * @param lsn where LSN 1 is the first slot, 2 the second, etc
     */
    public void setNumber(int lsn)
    {
        mNumberProperty.set(lsn);
    }

    /**
     * Downlink frequency
     * @return value in hertz
     */
    @JacksonXmlProperty(isAttribute = true, localName = "downlink")
    public long getDownlinkFrequency()
    {
        return mDownlinkFrequencyProperty.get();
    }

    /**
     * Sets the downlink frequency value
     * @param downlinkFrequency in hertz
     */
    public void setDownlinkFrequency(long downlinkFrequency)
    {
        mDownlinkFrequencyProperty.set(downlinkFrequency);
        getDownlinkMHz().set(downlinkFrequency / 1E6);
    }

    /**
     * Uplink frequency
     * @return value in hertz
     */
    @JacksonXmlProperty(isAttribute = true, localName = "uplink")
    public long getUplinkFrequency()
    {
        return mUplinkFrequencyProperty.get();
    }

    /**
     * Sets the uplink frequency value
     * @param uplinkFrequency in hertz
     */
    public void setUplinkFrequency(long uplinkFrequency)
    {
        mUplinkFrequencyProperty.set(uplinkFrequency);
        getUplinkMHz().set(uplinkFrequency / 1E6);
    }

    @Override
    public String toString()
    {
        return "TIMESLOT LSN:" + getNumber() + " DOWNLINK:" + getDownlinkFrequency();
    }

    /**
     * Creates an observable property extractor for use with observable lists to detect changes internal to this object.
     */
    public static Callback<TimeslotFrequency,Observable[]> extractor()
    {
        return (TimeslotFrequency tf) -> new Observable[] {tf.getNumberProperty(), tf.downlinkFrequencyPropertyProperty(),
            tf.uplinkFrequencyProperty(), tf.getDownlinkMHz(), tf.getUplinkMHz()};
    }
}
