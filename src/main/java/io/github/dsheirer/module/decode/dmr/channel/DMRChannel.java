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

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.protocol.Protocol;
import org.apache.commons.lang3.Validate;

/**
 * Base DMR Channel
 *
 * Note: timeslots are tracked as 1 and 2
 */
public abstract class DMRChannel extends IntegerIdentifier implements IChannelDescriptor
{
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

    @Override
    public Protocol getProtocol()
    {
        return Protocol.DMR;
    }

    /**
     * Repeater number for the channel.
     */
    public int getRepeater()
    {
        return getValue();
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
     * Logical slot number for this channel.  LSN is a 1-based index value where repeater one, timeslot 1 is
     * LSN 1, timeslot 2 is LSN 2, etc.
     *
     * Formula: LSN = ((channel - 1) * 2) + timeslot
     *
     * @return logical slot number, a 1-based index value
     */
    public int getLogicalSlotNumber()
    {
        int repeater = getRepeater();

        if(repeater > 0)
        {
            return ((repeater - 1) * 2) + getTimeslot();
        }

        return 0;
    }

    /**
     * Returns an array of length 1 containing this channel's logical slot number
     */
    public int[] getLSNArray()
    {
        int[] logicalSlotNumbers = new int[1];
        logicalSlotNumbers[0] = getLogicalSlotNumber();
        return logicalSlotNumbers;
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
}
