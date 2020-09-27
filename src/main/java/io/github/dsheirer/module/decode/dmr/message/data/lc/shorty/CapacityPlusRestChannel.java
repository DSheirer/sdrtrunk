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

package io.github.dsheirer.module.decode.dmr.message.data.lc.shorty;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.channel.DMRLogicalChannel;
import io.github.dsheirer.module.decode.dmr.channel.ITimeslotFrequencyReceiver;
import io.github.dsheirer.module.decode.dmr.channel.TimeslotFrequency;
import io.github.dsheirer.module.decode.dmr.identifier.DMRSite;

import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Capacity Plus - Rest Channel Notification
 */
public class CapacityPlusRestChannel extends ShortLCMessage implements ITimeslotFrequencyReceiver
{
    private static final int[] UNKNOWN = new int[]{12, 13, 14};
    private static final int[] REST_REPEATER = new int[]{15, 16, 17, 18};
    private static final int[] REST_TIMESLOT = new int[]{19};
    private static final int[] SITE = new int[]{20, 21, 22, 23, 24};
    private static final int[] UNKNOWN_2 = new int[]{25, 26, 27};

    private DMRLogicalChannel mRestChannel;
    private DMRSite mSite;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance
     *
     * @param message containing the short link control message bits
     */
    public CapacityPlusRestChannel(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if(!isValid())
        {
            sb.append("[CRC ERROR] ");
        }
        sb.append("SLC MOTOROLA CAP+ SITE:").append(getSite());
        sb.append(" REST CHANNEL:").append(getRestChannel());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Site number
     */
    public DMRSite getSite()
    {
        if(mSite == null)
        {
            mSite = DMRSite.create(getMessage().getInt(SITE));
        }

        return mSite;
    }

    /**
     * Rest Channel Number
     */
    public DMRLogicalChannel getRestChannel()
    {
        if(mRestChannel == null)
        {
            mRestChannel = new DMRLogicalChannel(getRestRepeater(), getRestTimeslot());
        }

        return mRestChannel;
    }

    /**
     * Rest repeater
     */
    public int getRestRepeater()
    {
        return getMessage().getInt(REST_REPEATER) + 1;
    }

    /**
     * Rest timeslot
     */
    public int getRestTimeslot()
    {
        return getMessage().getInt(REST_TIMESLOT) + 1;
    }

    /**
     * Exposes the rest channel logical slot number so that a LSN to frequency map can be applied to this message.
     */
    @Override
    public int[] getLogicalTimeslotNumbers()
    {
        return getRestChannel().getLSNArray();
    }

    /**
     * Applies the LSN to frequency map to the rest channel.
     * @param timeslotFrequencies that match the logical timeslots
     */
    @Override
    public void apply(List<TimeslotFrequency> timeslotFrequencies)
    {
        for(TimeslotFrequency timeslotFrequency: timeslotFrequencies)
        {
            if(getRestChannel().getLogicalSlotNumber() == timeslotFrequency.getNumber())
            {
                getRestChannel().setTimeslotFrequency(timeslotFrequency);
            }
        }
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getSite());
            mIdentifiers.add(getRestChannel());
        }

        return mIdentifiers;
    }
}
