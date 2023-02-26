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

package io.github.dsheirer.module.decode.dmr.message.data.header;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import io.github.dsheirer.module.decode.dmr.identifier.DmrTier3Radio;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.type.ServiceAccessPoint;

/**
 * Packet Sequence Data Header
 */
public abstract class PacketSequenceHeader extends DataHeader
{
    private static final int RADIO_TALKGROUP_FLAG = 0;
    private static final int RESPONSE_REQUESTED = 1;
    private static final int[] SERVICE_ACCESS_POINT = new int[]{8, 9, 10, 11};
    private static final int[] DESTINATION_IDENTIFIER = new int[]{16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28,
        29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] SOURCE_RADIO = new int[]{40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54,
        55, 56, 57, 58, 59, 60, 61, 62, 63};

    private IntegerIdentifier mDestinationLLID;
    private RadioIdentifier mSourceLLID;

    /**
     * Constructs an instance.
     *
     * @param syncPattern either BASE_STATION_DATA or MOBILE_STATION_DATA
     * @param message containing extracted 196-bit payload.
     * @param cach for the DMR burst
     * @param slotType for this data message
     * @param timestamp message was received
     * @param timeslot for the DMR burst
     */
    public PacketSequenceHeader(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    /**
     * Indicates if a response to this packet is requested to indicate if this is a CONFIRMED(true) or UNCONFIRMED (false)
     * message type.
     * @return true for confirmed and false for unconfirmed.
     */
    public boolean isResponseRequested()
    {
        return getMessage().get(RESPONSE_REQUESTED);
    }

    /**
     * Number of data blocks to follow
     */
    public abstract int getBlocksToFollow();

    /**
     * Service access point for the specified message
     */
    public static ServiceAccessPoint getServiceAccessPoint(CorrectedBinaryMessage message)
    {
        return ServiceAccessPoint.fromValue(message.getInt(SERVICE_ACCESS_POINT));
    }

    /**
     * Service access point for this message
     */
    public ServiceAccessPoint getServiceAccessPoint()
    {
        return getServiceAccessPoint(getMessage());
    }

    /**
     * Destination Logical Link ID
     */
    public IntegerIdentifier getDestinationLLID()
    {
        if(mDestinationLLID == null)
        {
            if(getMessage().get(RADIO_TALKGROUP_FLAG))
            {
                mDestinationLLID = DMRTalkgroup.create(getMessage().getInt(DESTINATION_IDENTIFIER));
            }
            else
            {
                mDestinationLLID = DmrTier3Radio.createTo(getMessage().getInt(DESTINATION_IDENTIFIER));
            }
        }

        return mDestinationLLID;
    }


    /**
     * Source Logical Link ID
     */
    public RadioIdentifier getSourceLLID()
    {
        if(mSourceLLID == null)
        {
            mSourceLLID = DmrTier3Radio.createFrom(getMessage().getInt(SOURCE_RADIO));
        }

        return mSourceLLID;
    }
}
