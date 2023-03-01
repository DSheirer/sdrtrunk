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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.standard;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.channel.DMRChannel;
import io.github.dsheirer.module.decode.dmr.channel.DMRTier3Channel;
import io.github.dsheirer.module.decode.dmr.identifier.DmrTier3Radio;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.data.mbc.MBCContinuationBlock;
import io.github.dsheirer.module.decode.dmr.message.type.AbsoluteChannelParameters;
import io.github.dsheirer.module.decode.dmr.message.type.DataType;
import java.util.ArrayList;
import java.util.List;

/**
 * DMR Tier III - Move Trunking System Control Channel (TSCC)
 */
public class MoveTSCC extends CSBKMessage
{
    private static final int[] RESERVED = new int[]{16, 17, 18, 19, 20, 20, 22, 23, 24};
    private static final int[] MASK = new int[]{25, 26, 27, 28, 29};
    private static final int[] RESERVED_2 = new int[]{30, 31, 32, 33, 34};
    private static final int REGISTRATION_REQUIRED_FLAG = 35;
    private static final int[] BACKOFF = new int[]{36, 37, 38, 39};
    private static final int[] RESERVED_3 = new int[]{40, 41, 42, 43};
    private static final int[] CHANNEL_NUMBER = new int[]{44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] RADIO = new int[]{56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72,
            73, 74, 75, 76, 77, 78, 79};

    private RadioIdentifier mRadioIdentifier;
    private DMRChannel mDMRChannel;
    private AbsoluteChannelParameters mAbsoluteChannelParameters;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a single-block CSBK instance
     *
     * @param syncPattern for the CSBK
     * @param message     bits
     * @param cach        for the DMR burst
     * @param slotType    for this message
     * @param timestamp
     * @param timeslot
     */
    public MoveTSCC(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    /**
     * Constructs a multi-block MBC instance
     *
     * @param syncPattern for the CSBK
     * @param message     bits
     * @param cach        for the DMR burst
     * @param slotType    for this message
     * @param timestamp
     * @param timeslot
     * @param multiBlock containing absolute frequency parameters
     */
    public MoveTSCC(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType,
                    long timestamp, int timeslot, MBCContinuationBlock multiBlock)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);

        if(multiBlock != null)
        {
            mAbsoluteChannelParameters = new AbsoluteChannelParameters(multiBlock.getMessage(), 0, 0);
        }
    }

    /**
     * Checks CRC and sets the message valid flag according to the results.
     */
    public void checkCRC()
    {
        if(getSlotType().getDataType() == DataType.MBC_HEADER)
        {
            checkMultiBlockCRC(getAbsoluteChannelParameters());
        }
        else
        {
            //Use the standard CSBK CRC check
            super.checkCRC();
        }
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
        sb.append(" MOVE TRUNK CONTROL CHANNEL ").append(getChannel());

        if(hasRadioIdentifier())
        {
            sb.append(" TO:").append(getRadioIdentifier());
        }

        return sb.toString();
    }

    /**
     * Absolute channel parameters structure
     */
    public AbsoluteChannelParameters getAbsoluteChannelParameters()
    {
        return mAbsoluteChannelParameters;
    }

    /**
     * Indicates if this message has an absolute channel parameters structure
     */
    public boolean hasAbsoluteChannelParameters()
    {
        return getAbsoluteChannelParameters() != null;
    }

    public DMRChannel getChannel()
    {
        if(mDMRChannel == null)
        {
            if(hasAbsoluteChannelParameters())
            {
                mDMRChannel = getAbsoluteChannelParameters().getChannel();
            }
            else
            {
                mDMRChannel = new DMRTier3Channel(getMessage().getInt(CHANNEL_NUMBER), 1);
            }
        }

        return mDMRChannel;
    }

    /**
     * Mobile subscriber ID masking value.  See: 102 361-4 p6.1.3
     */
    public int getMask()
    {
        return getMessage().getInt(MASK);
    }

    /**
     * Acknowledged radio identifier
     */
    public RadioIdentifier getRadioIdentifier()
    {
        if(mRadioIdentifier == null)
        {
            mRadioIdentifier = DmrTier3Radio.createTo(getMessage().getInt(RADIO));
        }

        return mRadioIdentifier;
    }

    public boolean hasRadioIdentifier()
    {
        return getMessage().getInt(RADIO) != 0;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            if(hasRadioIdentifier())
            {
                mIdentifiers.add(getRadioIdentifier());
            }
            mIdentifiers.add(getChannel());
        }

        return mIdentifiers;
    }
}
