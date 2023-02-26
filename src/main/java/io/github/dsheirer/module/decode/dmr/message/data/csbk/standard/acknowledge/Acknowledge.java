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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.acknowledge;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import io.github.dsheirer.module.decode.dmr.identifier.DmrTier3Radio;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.type.AcknowledgeType;
import io.github.dsheirer.module.decode.dmr.message.type.Reason;
import java.util.ArrayList;
import java.util.List;

/**
 * DMR Acknowledge Response
 */
public class Acknowledge extends CSBKMessage
{
    private static final int TARGET_GROUP_INDIVIDUAL_FLAG = 16;
    protected static final int[] RESPONSE_INFO = new int[]{16, 17, 18, 19, 20, 21, 22};
    private static final int[] ACKNOWLEDGE_TYPE = new int[]{23, 24};
    //Note: acknowledge type is the first 2 bits of the reason code - intentional overlap
    private static final int[] REASON_CODE = new int[]{23, 24, 25, 26, 27, 28, 29, 30};
    private static final int RESERVED = 31;
    private static final int[] TARGET_ADDRESS = new int[]{32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46,
            47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] ADDITIONAL_INFO_SOURCE_ADDRESS = new int[]{56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66,
            67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};

    protected List<Identifier> mIdentifiers;
    private IntegerIdentifier mTargetAddress;
    private RadioIdentifier mSourceRadio;

    /**
     * Constructs an instance
     *
     * @param syncPattern for the CSBK
     * @param message     bits
     * @param cach        for the DMR burst
     * @param slotType    for this message
     * @param timestamp
     * @param timeslot
     */
    public Acknowledge(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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
        sb.append(" ").append(getAcknowledgeType());
        sb.append(" REASON:").append(getReason());
        sb.append(" FM:").append(getSourceRadio());
        sb.append(" TO:").append(getTargetAddress());

        return sb.toString();
    }

    /**
     * Indicates if the target address is a talkgroup (true) or radio/gateway (false).
     */
    public boolean isTargetAddressTalkgroup()
    {
        return getMessage().get(TARGET_GROUP_INDIVIDUAL_FLAG);
    }

    /**
     * Acknowledgement message type
     */
    public AcknowledgeType getAcknowledgeType()
    {
        return AcknowledgeType.fromValue(getMessage().getInt(ACKNOWLEDGE_TYPE));
    }

    /**
     * Reason code.
     *
     * Note: the reason entries are coupled with the acknowledgement type and can optionally indicate when the
     * additional information field contains amplifying information.
     */
    public Reason getReason()
    {
        return getReason(getMessage());
    }

    /**
     * Utility method to determine the reason code for the specified message
     * @param message containing a reason code field
     * @return reason
     */
    public static Reason getReason(CorrectedBinaryMessage message)
    {
        return Reason.fromValue(message.getInt(REASON_CODE));
    }

    public IntegerIdentifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            if(isTargetAddressTalkgroup())
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

    public boolean hasSourceRadio()
    {
        return true; //TODO: delineate valid reason codes here
    }

    /**
     * Source radio identifier
     *
     * Note: the source radio field is dual-purpose and contains either a source radio address or
     * additional information, depending on the reason code and/or acknowledgement type.  Use the
     * hasSourceRadio() method to first check if this message carries a source radio identifier.
     */
    public RadioIdentifier getSourceRadio()
    {
        if(mSourceRadio == null)
        {
            mSourceRadio = DmrTier3Radio.createFrom(getMessage().getInt(ADDITIONAL_INFO_SOURCE_ADDRESS));
        }

        return mSourceRadio;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());

            if(hasSourceRadio())
            {
                mIdentifiers.add(getSourceRadio());
            }
        }

        return mIdentifiers;
    }
}
