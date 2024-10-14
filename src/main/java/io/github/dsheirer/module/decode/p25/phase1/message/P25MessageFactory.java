/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.module.decode.p25.phase1.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.hdu.HDUMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDU2Message;
import io.github.dsheirer.module.decode.p25.phase1.message.tdu.TDULCMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tdu.TDUMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.vselp.VSELP1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.vselp.VSELP2Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating P25 message parser instances.
 */
public class P25MessageFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(P25MessageFactory.class);

    /**
     * Creates a P25 message for known message types.
     *
     * NOTE: TSBK and PDU messages are not processed by this factory class.
     *
     * @param dataUnitID that identifies the message type
     * @param nac decoded from the network identifier (NID)
     * @param timestamp of the message
     * @param message containing message bits and bit error count results from nid error detection and
     * correction
     * @return constructed message parser
     */
    public static P25P1Message create(P25P1DataUnitID dataUnitID, int nac, long timestamp, CorrectedBinaryMessage message)
    {
        switch(dataUnitID)
        {
            case HEADER_DATA_UNIT:
                return new HDUMessage(message, nac, timestamp);
            case LOGICAL_LINK_DATA_UNIT_1:
                return new LDU1Message(message, nac, timestamp);
            case LOGICAL_LINK_DATA_UNIT_2:
                return new LDU2Message(message, nac, timestamp);
            case PACKET_HEADER_DATA_UNIT:
                mLog.warn("WARNING: PDU messages must be created by the PDUMessageFactory");
                return null;
            case TERMINATOR_DATA_UNIT:
                return new TDUMessage(message, nac, timestamp);
            case TERMINATOR_DATA_UNIT_LINK_CONTROL:
                return new TDULCMessage(message, nac, timestamp);
            case TRUNKING_SIGNALING_BLOCK_1:
            case TRUNKING_SIGNALING_BLOCK_2:
            case TRUNKING_SIGNALING_BLOCK_3:
                mLog.warn("WARNING: TSBK messages must be created by the TSBKMessageFactory");
                return null;
            case VSELP1:
                return new VSELP1Message(message, nac, timestamp);
            case VSELP2:
                return new VSELP2Message(message, nac, timestamp);
            default:
                return new UnknownP25Message(message, nac, timestamp, dataUnitID);
        }
    }
}
