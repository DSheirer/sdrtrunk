/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.message.hdu.HDUMessage;
import io.github.dsheirer.module.decode.p25.message.ldu.LDU2Message;
import io.github.dsheirer.module.decode.p25.message.ldu.lc.LDULCMessageFactory;
import io.github.dsheirer.module.decode.p25.message.pdu.PDUMessageFactory;
import io.github.dsheirer.module.decode.p25.message.tdu.TDUMessage;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.TDULCMessageFactory;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
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
     * @param dataUnitID that identifies the message type
     * @param correctedBinaryMessage containing message bits and bit error count results from nid error detection and
     * correction
     * @param nac decoded from the network identifier (NID)
     * @return constructed message or null of the message type is unrecognized
     */
    public static P25Message create(DataUnitID dataUnitID, int nac, long timestamp,
                                    CorrectedBinaryMessage correctedBinaryMessage)
    {
        switch(dataUnitID)
        {
            case HEADER_DATA_UNIT:
                return new HDUMessage(correctedBinaryMessage, dataUnitID, null);
            case LOGICAL_LINK_DATA_UNIT_1:
                return LDULCMessageFactory.create(dataUnitID, nac, timestamp, correctedBinaryMessage);
            case LOGICAL_LINK_DATA_UNIT_2:
                return new LDU2Message(correctedBinaryMessage, dataUnitID, null);
            case PACKET_HEADER_DATA_UNIT:
                return PDUMessageFactory.create(dataUnitID, nac, timestamp, correctedBinaryMessage);
            case TERMINATOR_DATA_UNIT:
                return new TDUMessage(correctedBinaryMessage, dataUnitID, null);
            case TERMINATOR_DATA_UNIT_LINK_CONTROL:
                return TDULCMessageFactory.create(dataUnitID, nac, timestamp, correctedBinaryMessage);
            default:
                mLog.debug("Unrecognized P25 Data Unit ID [" + dataUnitID + "] - cannot create message");
                return new P25Message(correctedBinaryMessage, dataUnitID, null);
        }
    }
}
