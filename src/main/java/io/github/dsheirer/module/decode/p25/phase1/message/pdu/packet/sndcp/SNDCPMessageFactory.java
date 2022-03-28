/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.sndcp;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.p25.reference.PDUType;

public class SNDCPMessageFactory
{
    public static SNDCPMessage create(BinaryMessage binaryMessage, boolean outbound)
    {
        PDUType pduType = SNDCPMessage.getPDUType(binaryMessage, outbound);

        switch(pduType)
        {
            case OUTBOUND_SNDCP_RF_UNCONFIRMED_DATA:
                //Note: (un)confirmed data type is processed by the PDUMessageFactory as a packet message
            case OUTBOUND_SNDCP_RF_CONFIRMED_DATA:
                //Note: (un)confirmed data type is processed by the PDUMessageFactory as a packet message
            case INBOUND_SNDCP_RF_CONFIRMED_DATA:
                //Note: (un)confirmed data type is processed by the PDUMessageFactory as a packet message
            case OUTBOUND_SNDCP_ACTIVATE_TDS_CONTEXT_ACCEPT:
                return new ActivateTdsContextAccept(binaryMessage, outbound);
            case INBOUND_SNDCP_ACTIVATE_TDS_CONTEXT_REQUEST:
                return new ActivateTdsContextRequest(binaryMessage, outbound);
            case OUTBOUND_SNDCP_ACTIVATE_TDS_CONTEXT_REJECT:
                return new ActivateTdsContextReject(binaryMessage, outbound);
            case OUTBOUND_SNDCP_DEACTIVATE_TDS_CONTEXT_REQUEST:
                return new DeActivateTdsContextRequest(binaryMessage, outbound);
            case OUTBOUND_SNDCP_DEACTIVATE_TDS_CONTEXT_ACCEPT:
            case INBOUND_SNDCP_DEACTIVATE_TDS_CONTEXT_ACCEPT:
            case INBOUND_SNDCP_DEACTIVATE_TDS_CONTEXT_REQUEST:

            case OUTBOUND_UNKNOWN:
            case INBOUND_UNKNOWN:
            default:
                return new SNDCPMessage(binaryMessage, outbound);
        }
    }
}
