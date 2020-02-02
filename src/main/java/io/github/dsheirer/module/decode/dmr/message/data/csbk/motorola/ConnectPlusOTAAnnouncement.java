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
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.channel.DMRLogicalChannel;
import io.github.dsheirer.module.decode.dmr.channel.ITimeslotFrequencyReceiver;
import io.github.dsheirer.module.decode.dmr.channel.TimeslotFrequency;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.ip.xcmp.XCMPMessageType;

import java.util.ArrayList;
import java.util.List;

/**
 * Connect Plus - Over The Air (OTA) Reprogramming Announcement
 *
 * Note: there are at least 2 file types that can be transferred:
 * 1. Network Frequency File
 * 2. Option Board Firmware
 */
public class ConnectPlusOTAAnnouncement extends CSBKMessage implements ITimeslotFrequencyReceiver
{
    private static final int[] MESSAGE_TYPE = new int[]{16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] VERSION = new int[]{24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] UNKNOWN = new int[]{40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56,
        57, 58, 59, 60, 61, 62, 63};
    private static final int[] REPEATER = new int[]{64, 65, 66, 67};
    private static final int[] TIMESLOT = new int[]{68};

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
    public ConnectPlusOTAAnnouncement(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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
        sb.append(" CON+ ANNOUNCE OTA ").append(getMessageType());
        sb.append(" VER:").append(getMessageVersion());
        sb.append(" AVAILABLE ON ").append(getDMRTimeslotFrequencyChannel());
        sb.append(" MSG:").append(getMessage().toHexString());

        return sb.toString();
    }

    public XCMPMessageType getMessageType()
    {
        return XCMPMessageType.fromValue(getMessage().getInt(MESSAGE_TYPE));
    }

    public int getMessageVersion()
    {
        return getMessage().getInt(VERSION);
    }

    /**
     * Repeater number
     */
    public int getRepeater()
    {
        return getMessage().getInt(REPEATER);
    }

    /**
     * Timeslot
     */
    public int getTimeslot()
    {
        return getMessage().getInt(TIMESLOT);
    }

    /**
     * DMR Channel
     */
    public DMRLogicalChannel getDMRTimeslotFrequencyChannel()
    {
        if(mDMRLogicalChannel == null)
        {
            mDMRLogicalChannel = new DMRLogicalChannel(getRepeater(), getTimeslot());
        }

        return mDMRLogicalChannel;
    }

    @Override
    public int[] getLogicalTimeslotNumbers()
    {
        return getDMRTimeslotFrequencyChannel().getLSNArray();
    }

    /**
     * Assigns a timeslot frequency map for the DMR channel
     *
     * @param timeslotFrequencies that match the logical timeslots
     */
    @Override
    public void apply(List<TimeslotFrequency> timeslotFrequencies)
    {
        for(TimeslotFrequency timeslotFrequency : timeslotFrequencies)
        {
            if(timeslotFrequency.getNumber() == getDMRTimeslotFrequencyChannel().getLogicalSlotNumber())
            {
                getDMRTimeslotFrequencyChannel().setTimeslotFrequency(timeslotFrequency);
            }
        }
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getDMRTimeslotFrequencyChannel());
        }

        return mIdentifiers;
    }
}
