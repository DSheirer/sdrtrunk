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

package io.github.dsheirer.module.decode.dmr.message.data.header;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.type.DataPacketFormat;

/**
 * Data Header message.
 */
public class DataHeader extends HeaderMessage
{
    private static final int[] DATA_PACKET_FORMAT = new int[]{4, 5, 6, 7};


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
    public DataHeader(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType,
                      long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CC:").append(getSlotType().getColorCode());
        if(!isValid())
        {
            sb.append(" [CRC ERROR]");
        }
        sb.append(" DATA HEADER");
        sb.append(" FORMAT:").append(getDataPacketFormat());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Data packet format for the specified message
     * @param message
     * @return
     */
    public static DataPacketFormat getDataPacketFormat(CorrectedBinaryMessage message)
    {
        return DataPacketFormat.fromValue(message.getInt(DATA_PACKET_FORMAT));
    }

    /**
     * Data Packet Format for this message
     */
    public DataPacketFormat getDataPacketFormat()
    {
        return getDataPacketFormat(getMessage());
    }

    /**
     * Indicates if this is a header for a confirmed packet sequence
     */
    public boolean isConfirmedData()
    {
        return getDataPacketFormat() == DataPacketFormat.CONFIRMED_DATA_PACKET;
    }

    /**
     * Indicates if this is a header for an unconfirmed packet sequence
     */
    public boolean isUnconfirmedData()
    {
        return getDataPacketFormat() == DataPacketFormat.UNCONFIRMED_DATA_PACKET;
    }

}
