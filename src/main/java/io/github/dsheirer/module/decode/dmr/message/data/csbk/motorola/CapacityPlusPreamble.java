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
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.Preamble;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Capacity Plus - Preamble
 */
public class CapacityPlusPreamble extends Preamble
{
    private static final int RADIO_TALKGROUP_FLAG = 17;
    private static final int[] BLOCKS_TO_FOLLOW = new int[]{18, 19, 20, 21, 22};
    private static final int[] UNKNOWN_1 = new int[]{24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] UNKNOWN_2 = new int[]{32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] TARGET_ADDRESS = new int[]{40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] UNKNOWN_3 = new int[]{56, 57, 58, 59, 60, 61, 62, 63};
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
    public CapacityPlusPreamble(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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
        sb.append(" CSBK CAP+").append(isCSBKPreamble() ? " CSBK" : " DATA");
        sb.append(" PREAMBLE FM:").append(getSourceAddress());
        sb.append(" TO:").append(getTargetAddress());
        sb.append(" BLOCKS TO FOLLOW:").append(getBlocksToFollow());
        sb.append(" UNK1:").append(getUnknown1());
        sb.append(" UNK2:").append(getUnknown2());
        sb.append(" UNK3:").append(getUnknown3());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    public String getUnknown1()
    {
        return getMessage().getHex(UNKNOWN_1, 2);
    }

    public String getUnknown2()
    {
        return getMessage().getHex(UNKNOWN_2, 2);
    }

    public String getUnknown3()
    {
        return getMessage().getHex(UNKNOWN_3, 2);
    }

    /**
     * Number of data blocks that will follow this preamble
     */
    public int getBlocksToFollow()
    {
        return getMessage().getInt(BLOCKS_TO_FOLLOW);
    }

    /**
     * Target radio address that is either a radio or talkgroup identifier
     */
    public IntegerIdentifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            if(getMessage().get(RADIO_TALKGROUP_FLAG))
            {
                mTargetAddress = DMRTalkgroup.create(getMessage().getInt(TARGET_ADDRESS));
            }
            else
            {
                mTargetAddress = DMRRadio.createTo(getMessage().getInt(TARGET_ADDRESS));
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
            mSourceAddress = DMRRadio.createFrom(getMessage().getInt(SOURCE_ADDRESS));
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
