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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.hytera;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import io.github.dsheirer.module.decode.dmr.identifier.DmrTier3Radio;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.Preamble;
import java.util.ArrayList;
import java.util.List;

/**
 * Hytera Preamble
 */
public class HyteraXPTPreamble extends Preamble
{
    private static final int[] FREE_REPEATER = new int[]{32, 33, 34, 35};
    private static final int[] PRIORITY_REPEATER = new int[]{36, 37, 38, 39};
    private static final int[] TARGET_ADDRESS = new int[]{40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] PRIORITY_CALL_HASHED_ADDRESS = new int[]{56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] SOURCE_ADDRESS = new int[]{64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};

    private IntegerIdentifier mTargetAddress;
    private RadioIdentifier mSourceAddress;
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
    public HyteraXPTPreamble(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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
        sb.append(" CSBK PREAMBLE FM:").append(getSourceAddress());
        sb.append(" TO:").append(getTargetAddress());
        sb.append(isCSBKPreamble() ? " CSBK" : " DATA");
        sb.append(" BLOCKS TO FOLLOW:").append(getBlocksToFollow());
        if(isAllChannelsBusy())
        {
            sb.append(" ALL REPEATERS BUSY");
        }
        else
        {
            sb.append(" FREE REPEATER:").append(getFreeRepeater());
        }

        if(hasPriorityCall())
        {
            sb.append(" PRIORITY CALL FOR:").append(getPriorityCallHashedAddress());
            sb.append(" ON REPEATER:").append(getPriorityCallRepeater());
        }
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Indicates if all free repreaters are busy
     */
    public boolean isAllChannelsBusy()
    {
        return getFreeRepeater() == 0;
    }


    /**
     * Free repeater number.
     *
     * @return repeater, or 0 for all repeaters busy
     */
    public int getFreeRepeater()
    {
        return getMessage().getInt(FREE_REPEATER);
    }

    /**
     * Indicates if there is a priority call on another repeater channel that users should monitor when their
     * talkgroup matches the priority call hashed address
     */
    public boolean hasPriorityCall()
    {
        return getPriorityCallRepeater() > 0;
    }

    /**
     * Indicates the repeater number that is forwarding a priority call
     */
    public int getPriorityCallRepeater()
    {
        return getMessage().getInt(PRIORITY_REPEATER);
    }

    /**
     * Hashed address of priority call
     */
    public String getPriorityCallHashedAddress()
    {
        return String.format("%02X", getMessage().getInt(PRIORITY_CALL_HASHED_ADDRESS)).toUpperCase();
    }

    /**
     * Target radio address that is either a radio or talkgroup identifier
     */
    public IntegerIdentifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            if(isTalkgroupTargetAddress())
            {
                mTargetAddress = DMRTalkgroup.create(getMessage().getInt(TARGET_ADDRESS));
            }
            else
            {
                mTargetAddress = DmrTier3Radio.createTo(getMessage().getInt(TARGET_ADDRESS));
            }
        }

        return mTargetAddress;
    }

    /**
     * Source radio identifier
     */
    public RadioIdentifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = DmrTier3Radio.createFrom(getMessage().getInt(SOURCE_ADDRESS));
        }

        return mSourceAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
            mIdentifiers.add(getSourceAddress());
        }

        return mIdentifiers;
    }
}
