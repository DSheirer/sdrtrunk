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

package io.github.dsheirer.module.decode.dmr.channel;

/**
 * DMR logical channel. This channel uses a logical channel number and a timeslot.
 */
public class DMRLogicalChannel extends DMRChannel
{
    private TimeslotFrequency mTimeslotFrequency;

    /**
     * Constructs an instance.  Note: radio reference uses a one based index, so we add a value of one to the
     * calculated logical slot value for visual compatibility for users.
     *
     * @param channel number or repeater number
     * @param logicalSlotNumber - zero based index.
     */
    public DMRLogicalChannel(int channel, int timeslot)
    {
        super(channel, timeslot);
    }

    /**
     * Downlink frequency
     * @return value in Hertz, or 0 if this channel doesn't have a timeslot frequency mapping
     */
    @Override
    public long getDownlinkFrequency()
    {
        if(mTimeslotFrequency != null)
        {
            return mTimeslotFrequency.getDownlinkFrequency();
        }

        return 0;
    }

    /**
     * Uplink frequency
     * @return value in Hertz, or 0 if this channel doesn't have a timeslot frequency mapping
     */
    @Override
    public long getUplinkFrequency()
    {
        if(mTimeslotFrequency != null)
        {
            return mTimeslotFrequency.getUplinkFrequency();
        }

        return 0;
    }

    /**
     * Sets the timeslot frequency mapping
     * @param timeslotFrequency
     */
    public void setTimeslotFrequency(TimeslotFrequency timeslotFrequency)
    {
        mTimeslotFrequency = timeslotFrequency;
    }

    /**
     * Formatted channel number
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("LSN:").append(getLogicalSlotNumber());
        sb.append(" LCN:").append(getValue());
        sb.append(" TS:").append(getTimeslot());
        return sb.toString();
    }
}
