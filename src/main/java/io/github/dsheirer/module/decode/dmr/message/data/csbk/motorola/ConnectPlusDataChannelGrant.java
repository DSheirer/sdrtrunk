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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.channel.DMRLogicalChannel;
import io.github.dsheirer.module.decode.dmr.channel.ITimeslotFrequencyReceiver;
import io.github.dsheirer.module.decode.dmr.channel.TimeslotFrequency;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Connect Plus - Data Channel Grant
 */
public class ConnectPlusDataChannelGrant extends CSBKMessage implements ITimeslotFrequencyReceiver
{
    private static final int[] TARGET_ADDRESS = new int[]{16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
        32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] REPEATER = new int[]{40, 41, 42, 43};
    private static final int[] CHANNEL_GRANT_TIMESLOT = new int[]{44};

    //Analysis: this field correlates to UNKNOWN_FIELD_1(bits: 40-48) in ConnectPlusTerminateChannelGrant.
    private static final int[] UNKNOWN_FIELD = new int[]{48, 49, 50, 51, 52, 53, 54, 55};

    private RadioIdentifier mTargetRadio;
    private DMRLogicalChannel mDMRLogicalChannel;
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
    public ConnectPlusDataChannelGrant(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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
        sb.append(" CSBK ").append(getVendor());
        sb.append(" DATA CHANNEL GRANT TO:").append(getTargetRadio());
        sb.append(" ").append(getChannel());
        sb.append(" UNK:").append(getUnknownField());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Target radio address
     */
    public RadioIdentifier getTargetRadio()
    {
        if(mTargetRadio == null)
        {
            mTargetRadio = DMRRadio.createTo(getMessage().getInt(TARGET_ADDRESS));
        }

        return mTargetRadio;
    }

    /**
     * Unknown field
     */
    public int getUnknownField()
    {
        return getMessage().getInt(UNKNOWN_FIELD);
    }

    /**
     * Channel grant repeater number
     */
    public int getRepeater()
    {
        return getMessage().getInt(REPEATER);
    }

    /**
     * Channel grant timeslot
     * @return 1 or 2
     */
    public int getChannelGrantTimeslot()
    {
        return getMessage().getInt(CHANNEL_GRANT_TIMESLOT) + 1;
    }

    /**
     * DMR Channel
     */
    public DMRLogicalChannel getChannel()
    {
        if(mDMRLogicalChannel == null)
        {
            mDMRLogicalChannel = new DMRLogicalChannel(getRepeater(), getChannelGrantTimeslot());
        }

        return mDMRLogicalChannel;
    }

    @Override
    public int[] getLogicalTimeslotNumbers()
    {
        return getChannel().getLSNArray();
    }

    /**
     * Assigns a timeslot frequency map for the DMR channel
     * @param timeslotFrequencies that match the logical timeslots
     */
    @Override
    public void apply(List<TimeslotFrequency> timeslotFrequencies)
    {
        for(TimeslotFrequency timeslotFrequency: timeslotFrequencies)
        {
            if(timeslotFrequency.getNumber() == getChannel().getLogicalSlotNumber())
            {
                getChannel().setTimeslotFrequency(timeslotFrequency);
            }
        }
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetRadio());
            mIdentifiers.add(getChannel());
        }

        return mIdentifiers;
    }
}
