/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.hytera;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import java.util.ArrayList;
import java.util.List;

/**
 * Hytera XPT - Site State
 *
 * Note: I've only seen this documented in the patent and haven't yet seen it in the wild.  So, I'm not sure if this
 * is a CSBK or not.  CSBK is the only thing that makes sense, but they've redefined bits 0 and 1 of the message which
 * collides with CSBK bits 0 and 1.
 */
public class HyteraXPTSiteState extends CSBKMessage
{
    private static final int[] SEQUENCE_NUMBER = new int[]{0, 1};
    private static final int[] FREE_REPEATER = new int[]{16, 17, 18, 19};
    private static final int[] REPEATER_A_STATE = new int[]{20, 21, 22, 23};
    private static final int[] REPEATER_B_STATE = new int[]{24, 25, 26, 27};
    private static final int[] REPEATER_C_STATE = new int[]{28, 29, 30, 31};
    private static final int[] REPEATER_A_TS0_ADDRESS = new int[]{32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] REPEATER_A_TS1_ADDRESS = new int[]{40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] REPEATER_B_TS0_ADDRESS = new int[]{48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] REPEATER_B_TS1_ADDRESS = new int[]{56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] REPEATER_C_TS0_ADDRESS = new int[]{64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] REPEATER_C_TS1_ADDRESS = new int[]{72, 73, 74, 75, 76, 77, 78, 79};

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
    public HyteraXPTSiteState(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    /**
     * Sequence number to indicate the set of site's repeaters that are being transmitted:
     *
     * SN 0: repeaters 1-3
     * SN 1: repeaters 4-6
     * SN 2: repeaters 7-9
     *
     * @return sequence number
     */
    public int getSequenceNumber()
    {
        return getMessage().getInt(SEQUENCE_NUMBER);
    }

    /**
     * Indicates if all channels of the site are busy.
     */
    public boolean isAllChannelsBusy()
    {
        return getFreeRepeater() == 0;
    }

    /**
     * Free repeater number to use when both timeslots of this repeater are busy
     */
    public int getFreeRepeater()
    {
        return getMessage().getInt(FREE_REPEATER);
    }

    /**
     * Repeater number for repeater A field for this message
     */
    public int getRepeaterANumber()
    {
        switch(getSequenceNumber())
        {
            case 0:
                return 1;
            case 1:
                return 4;
            case 2:
                return 7;
        }

        return 0;
    }

    /**
     * Repeater number for repeater B field for this message
     */
    public int getRepeaterBNumber()
    {
        switch(getSequenceNumber())
        {
            case 0:
                return 2;
            case 1:
                return 5;
            case 2:
                return 8;
        }

        return 0;
    }

    /**
     * Repeater number for repeater C field for this message
     */
    public int getRepeaterCNumber()
    {
        switch(getSequenceNumber())
        {
            case 0:
                return 3;
            case 1:
                return 6;
            case 2:
                return 9;
        }

        return 0;
    }

    /**
     * State of Repeater A
     */
    public int getRepeaterAStateInfo()
    {
        return getMessage().getInt(REPEATER_A_STATE);
    }

    /**
     * State of Repeater B
     */
    public int getRepeaterBStateInfo()
    {
        return getMessage().getInt(REPEATER_B_STATE);
    }

    /**
     * State of Repeater C
     */
    public int getRepeaterCStateInfo()
    {
        return getMessage().getInt(REPEATER_C_STATE);
    }

    /**
     * Hashed Address that is currently using Repeater A Timeslot 0
     */
    public String getRepeaterATimeslot0HashedAddress()
    {
        return String.format("%02X", getMessage().getInt(REPEATER_A_TS0_ADDRESS));
    }

    /**
     * Hashed Address that is currently using Repeater A Timeslot 1
     */
    public String getRepeaterATimeslot1HashedAddress()
    {
        return String.format("%02X", getMessage().getInt(REPEATER_A_TS1_ADDRESS));
    }

    /**
     * Hashed Address that is currently using Repeater B Timeslot 0
     */
    public String getRepeaterBTimeslot0HashedAddress()
    {
        return String.format("%02X", getMessage().getInt(REPEATER_B_TS0_ADDRESS));
    }

    /**
     * Hashed Address that is currently using Repeater B Timeslot 1
     */
    public String getRepeaterBTimeslot1HashedAddress()
    {
        return String.format("%02X", getMessage().getInt(REPEATER_B_TS1_ADDRESS));
    }

    /**
     * Hashed Address that is currently using Repeater C Timeslot 0
     */
    public String getRepeaterCTimeslot0HashedAddress()
    {
        return String.format("%02X", getMessage().getInt(REPEATER_C_TS0_ADDRESS));
    }

    /**
     * Hashed Address that is currently using Repeater C Timeslot 1
     */
    public String getRepeaterCTimeslot1HashedAddress()
    {
        return String.format("%02X", getMessage().getInt(REPEATER_C_TS1_ADDRESS));
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
        if(hasRAS())
        {
            sb.append(" RAS:").append(getBPTCReservedBits());
        }
        sb.append(" HYTERA XPT SITE");

        if(isAllChannelsBusy())
        {
            sb.append(" ALL CHANNELS BUSY");
        }
        else
        {
            sb.append(" FREE:").append(getFreeRepeater());
        }

        //Repeater A
        sb.append(" STATE R").append(getRepeaterANumber()).append(":").append(getRepeaterAStateInfo());
        sb.append(" (").append(getRepeaterATimeslot0HashedAddress()).append("/");
        sb.append(getRepeaterATimeslot1HashedAddress()).append(")");

        //Repeater B
        sb.append(" R").append(getRepeaterBNumber()).append(":").append(getRepeaterBStateInfo());
        sb.append(" (").append(getRepeaterBTimeslot0HashedAddress()).append("/");
        sb.append(getRepeaterBTimeslot1HashedAddress()).append(")");

        //Repeater C
        sb.append(" R").append(getRepeaterCNumber()).append(":").append(getRepeaterCStateInfo());
        sb.append(" (").append(getRepeaterCTimeslot0HashedAddress()).append("/");
        sb.append(getRepeaterCTimeslot1HashedAddress()).append(")");

        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
        }

        return mIdentifiers;
    }
}
