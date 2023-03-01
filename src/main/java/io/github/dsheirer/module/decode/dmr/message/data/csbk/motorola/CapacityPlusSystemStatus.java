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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.channel.DMRLogicalChannel;
import io.github.dsheirer.module.decode.dmr.channel.ITimeslotFrequencyReceiver;
import io.github.dsheirer.module.decode.dmr.channel.TimeslotFrequency;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.type.LCSS;
import java.util.ArrayList;
import java.util.List;

/**
 * Capacity+ System Status CSBKO=62 Message
 */
public class CapacityPlusSystemStatus extends CSBKMessage implements ITimeslotFrequencyReceiver
{
    private static final int[] FRAGMENT_INDICATOR = new int[]{16, 17};
//    private static final int TIMESLOT = 18;
    private static final int[] REST_REPEATER = new int[]{19, 20, 21, 22};
    private static final int[] REST_TIMESLOT = new int[]{23};

    private DMRLogicalChannel mRestChannel;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance
     *
     * @param syncPattern for the CSBK
     * @param message bits
     * @param cach for the DMR burst
     * @param slotType for this message
     * @param timestamp
     * @param timeslot
     */
    public CapacityPlusSystemStatus(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("[CRC-ERROR] ");
        }

        sb.append("CC:").append(getSlotType().getColorCode());
        sb.append(" CSBK CAP+ SYSTEM STATUS ").append(getFragmentIndicator());
        sb.append(" REST ").append(getRestChannel());
        sb.append(" MSG:").append(getMessage().toHexString());

        return sb.toString();
    }

    /**
     * Fragment indicator for system status message values that are fragmented across multiple system status
     * messages.
     */
    public LCSS getFragmentIndicator()
    {
        return LCSS.fromValue(getMessage().getInt(FRAGMENT_INDICATOR));
    }

    /**
     * Current rest channel for this site.
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
     * Rest Channel Repeater
     */
    public int getRestRepeater()
    {
        return getMessage().getInt(REST_REPEATER) + 1;
    }

    /**
     * Rest Channel Timeslot
     * @return 1 or 2
     */
    public int getRestTimeslot()
    {
        return getMessage().getInt(REST_TIMESLOT) + 1;
    }

    /**
     * Logical slot numbers that require slot to frequency mappings.
     */
    @Override
    public int[] getLogicalTimeslotNumbers()
    {
        return getRestChannel().getLSNArray();
    }

    /**
     * Applies logical slot number to frequency mapping.
     *
     * @param timeslotFrequencies that match the logical timeslots
     */
    @Override
    public void apply(List<TimeslotFrequency> timeslotFrequencies)
    {
        for(TimeslotFrequency timeslotFrequency : timeslotFrequencies)
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
            mIdentifiers.add(getRestChannel());
        }

        return mIdentifiers;
    }
}
