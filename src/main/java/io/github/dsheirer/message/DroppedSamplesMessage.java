/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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
package io.github.dsheirer.message;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.protocol.Protocol;
import java.util.Collections;
import java.util.List;

/**
 * Empty message to convey that a number of dropped samples produced a shortfall of expected bits.  This can result
 * from either the tuner dropping the samples, or from a decoder dropping samples due to timing adjustments.
 *
 * This message supports periodic updates to user to indicate that the USB bus and/or the tuner may be causing dropped
 * samples.
 */
public class DroppedSamplesMessage extends Message
{
    private int mBitsDropped;
    private Protocol mProtocol;
    private int mTimeslot = 0;

    /**
     * Constructs a dropped samples message.
     *
     * @param timestamp of the message
     * @param bitsDropped for the expected message length
     * @param timeslot that lost the sync
     */
    public DroppedSamplesMessage(long timestamp, int bitsDropped, Protocol protocol, int timeslot)
    {
        super(timestamp);
        mBitsDropped = bitsDropped;
        mProtocol = protocol;
        mTimeslot = timeslot;
    }

    /**
     * Constructs a dropped samples message.
     *
     * @param timestamp of the message
     * @param bitsDropped for the expected message length
     */
    public DroppedSamplesMessage(long timestamp, int bitsDropped, Protocol protocol)
    {
        this(timestamp, bitsDropped, protocol, 0);
    }

    /**
     * Timeslot that has dropped bits
     * @return timeslot or a default of 0
     */
    @Override
    public int getTimeslot()
    {
        return mTimeslot;
    }

    /**
     * Indicates the number of bits that were detected to be dropped
     */
    public int getBitsDropped()
    {
        return mBitsDropped;
    }

    @Override
    public String toString()
    {
        return "<-> DROPPED SAMPLES (POSSIBLY) DETECTED -- COULD NOT ACCOUNT FOR [" + getBitsDropped() + "] SYMBOL BITS";
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public Protocol getProtocol()
    {
        return mProtocol;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
