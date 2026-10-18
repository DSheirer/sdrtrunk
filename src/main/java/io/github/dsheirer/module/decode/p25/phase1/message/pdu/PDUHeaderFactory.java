/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.pdu;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCHeader;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.PacketHeader;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.response.ResponseHeader;
import io.github.dsheirer.module.decode.p25.reference.PDUFormat;
import io.github.dsheirer.module.decode.p25.reference.ServiceAccessPoint;

public class PDUHeaderFactory
{
    public static PDUHeader getPDUHeader(CorrectedBinaryMessage correctedBinaryMessage)
    {
        boolean passesCRC = correctedBinaryMessage.getCorrectedBitCount() >= 0;
        PDUFormat format = PDUHeader.getFormat(correctedBinaryMessage);

        switch(format)
        {
            case ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL:
                return new AMBTCHeader(correctedBinaryMessage, passesCRC);
            case PACKET_DATA:
                return new PacketHeader(correctedBinaryMessage, passesCRC);
            case RESPONSE_PACKET_HEADER_FORMAT:
                return new ResponseHeader(correctedBinaryMessage, passesCRC);
            case UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL:
                //Format 0x15 is the generic unconfirmed data header.  Unconfirmed multi-block trunking control is
                //only the trunking control SAP (61/63) use of it.  With a packet data SAP the header has the same
                //field layout as the confirmed packet data header and the data blocks carry an (IP) packet.
                if(isPacketData(getServiceAccessPoint(correctedBinaryMessage)))
                {
                    return new PacketHeader(correctedBinaryMessage, passesCRC);
                }

                return new PDUHeader(correctedBinaryMessage, passesCRC);
            default:
                return new PDUHeader(correctedBinaryMessage, passesCRC);
        }
    }

    /**
     * Service Access Point (SAP) from a packet data or unconfirmed data header message.
     */
    public static ServiceAccessPoint getServiceAccessPoint(CorrectedBinaryMessage correctedBinaryMessage)
    {
        return ServiceAccessPoint.fromValue(correctedBinaryMessage.getInt(PacketHeader.SAP_ID));
    }

    /**
     * Indicates if the SAP identifies packet data that is processed by the PDUMessageFactory as a packet message.
     */
    public static boolean isPacketData(ServiceAccessPoint sap)
    {
        switch(sap)
        {
            case UNENCRYPTED_USER_DATA:
            case PACKET_DATA:
            case SNDCP_PACKET_DATA_CONTROL:
                return true;
            default:
                return false;
        }
    }
}
