/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.p25.message.pdu;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRCP25;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.AMBTCHeader;
import io.github.dsheirer.module.decode.p25.message.pdu.packet.PacketHeader;
import io.github.dsheirer.module.decode.p25.message.pdu.response.ResponseHeader;
import io.github.dsheirer.module.decode.p25.reference.PDUFormat;

public class PDUHeaderFactory
{
    public static PDUHeader getPDUHeader(CorrectedBinaryMessage correctedBinaryMessage)
    {
        //CCITT-16 can detect and correct up to 1 bit error max - 2 bit errors indicates CRC-fail
        int errorCount = CRCP25.correctCCITT80(correctedBinaryMessage, 0, 80);
        boolean passesCRC = errorCount < 2;

        correctedBinaryMessage.incrementCorrectedBitCount(errorCount);

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
            default:
                return new PDUHeader(correctedBinaryMessage, passesCRC);
        }
    }
}
