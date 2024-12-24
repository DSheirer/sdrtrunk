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

package io.github.dsheirer.module.decode.dmr.message.data.usb;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRCDMR;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.DataMessage;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.type.ServiceType;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified Single Block Data
 */
public class USBData extends DataMessage
{
    private static final int[] SERVICE_TYPE = new int[]{0, 1, 2, 3};
    private static final int[] RESPONSE_DELAY = new int[]{4, 5};
    private static final int PAYLOAD_CONTENT_FLAG = 6;
    private static final int RESERVED_FLAG = 7;
    private static final int PARAMETERS_START = 8;
    private static final int[] TARGET_RADIO = new int[]{56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71,
        72, 73, 74, 75, 76, 77, 78, 79};

    private List<Identifier> mIdentifiers;
    private RadioIdentifier mTargetRadio;

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
    public USBData(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);

        int correctedBitCount = CRCDMR.correctCCITT80(message, 0, 80, 0x3333);

        //Set message valid flag according to the corrected bit count for the CRC protected message
        setValid(correctedBitCount < 2);
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

        sb.append(" USB DATA BLOCK");
        sb.append(" TO:").append(getTargetRadio());
        sb.append(" SERVICE:").append(getServiceType());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Utility method to lookup service type for the specified message
     */
    public static ServiceType getServiceType(CorrectedBinaryMessage message)
    {
        return ServiceType.fromValue(message.getInt(SERVICE_TYPE));
    }

    /**
     * Service type for this message
     */
    public ServiceType getServiceType()
    {
        return getServiceType(getMessage());
    }

    public RadioIdentifier getTargetRadio()
    {
        if(mTargetRadio == null)
        {
            mTargetRadio = DMRRadio.createTo(getMessage().getInt(TARGET_RADIO));
        }

        return mTargetRadio;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetRadio());
        }

        return mIdentifiers;
    }
}
