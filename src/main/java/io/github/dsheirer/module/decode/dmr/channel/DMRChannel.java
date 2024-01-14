/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.protocol.Protocol;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * Base DMR Channel
 *
 * Note: timeslots are tracked as 1 and 2
 */
public abstract class DMRChannel extends IntegerIdentifier implements IChannelDescriptor,  ITimeslotFrequencyReceiver
{
    private TimeslotFrequency mTimeslotFrequency;
    private int mTimeslot;

    /**
     * Constructs an instance
     * @param channel number or repeater number, one-based repeater number.
     * @param timeslot in range: 1 or 2
     */
    public DMRChannel(int channel, int timeslot)
    {
        super(channel, IdentifierClass.NETWORK, Form.CHANNEL, Role.BROADCAST);
        Validate.inclusiveBetween(0, 2, timeslot, "Timeslot must be between 1 and 2");
        mTimeslot = timeslot;
    }

    /**
     * Creates a channel of the same type, but for the alternate timeslot.
     * @return sister timeslot channel
     */
    public abstract DMRChannel getSisterTimeslot();

    @Override
    public Protocol getProtocol()
    {
        return Protocol.DMR;
    }

    /**
     * Repeater number aka Logical Channel Number (LCN) for the channel.
     */
    public int getChannelNumber()
    {
        return getValue();
    }

    /**
     * Convenience method to format the single channel number as an array for TimeslotFrequency matching.
     * @return channel number as an integer array.
     */
    public int[] getLogicalChannelNumbers()
    {
        return new int[]{getChannelNumber()};
    }


    /**
     * Timeslot for the channel.
     * @return timeslot as zero-based index with values in range: 0 or 1.
     */
    public int getTimeslot()
    {
        return mTimeslot;
    }

    /**
     * Number of timeslots for the DMR channel.
     * @return 2 always.
     */
    @Override
    public int getTimeslotCount()
    {
        return 2;
    }

    /**
     * Indicates (true) that this is a DMR TDMA channel.
     */
    @Override
    public boolean isTDMAChannel()
    {
        return true;
    }


    @Override
    public String toString()
    {
        return "CHAN:" + getChannelNumber() + ":" + getTimeslot();
    }

    /**
     * Not implemented
     */
    @Override
    public int[] getFrequencyBandIdentifiers()
    {
        return new int[0];
    }

    /**
     * Not implemented.
     */
    @Override
    public void setFrequencyBand(IFrequencyBand bandIdentifier)
    {
        throw new IllegalArgumentException("This method is not supported");
    }

    @Override
    public long getDownlinkFrequency()
    {
        if(mTimeslotFrequency != null)
        {
            return mTimeslotFrequency.getDownlinkFrequency();
        }

        return 0;
    }

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
     * Sets the lsn to frequency mapper value.
     * @param timeslotFrequency to set
     */
    public void setTimeslotFrequency(TimeslotFrequency timeslotFrequency)
    {
        mTimeslotFrequency = timeslotFrequency;
    }

    /**
     * TimeslotFrequency for this channel.
     * @return timeslot frequency instance, or null.
     */
    public TimeslotFrequency getTimeslotFrequency()
    {
        return mTimeslotFrequency;
    }

    @Override
    public void apply(List<TimeslotFrequency> timeslotFrequencies)
    {
        for(TimeslotFrequency timeslotFrequency: timeslotFrequencies)
        {
            if(timeslotFrequency.getChannelNumber() == getChannelNumber())
            {
                setTimeslotFrequency(timeslotFrequency);
                return;
            }
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(!(o instanceof DMRChannel that))
        {
            return false;
        }
        if(!super.equals(o))
        {
            return false;
        }
        return getTimeslot() == that.getTimeslot();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), getTimeslot());
    }
}
