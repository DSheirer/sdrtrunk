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

/**
 * Subnetwork Dependent Convergence Protocol (SNDCP) base message parser
 */
public class SNDCPMessage
{
    private static final int[] PDU_TYPE = {0, 1, 2, 3};

    private BinaryMessage mMessage;
    private boolean mOutbound;

    /**
     * Constructs an SNDCP message parser instance.
     *
     * @param message containing the binary sequence
     * @param outbound where true is outbound (from repeater) and false is inbound (from mobile)
     */
    public SNDCPMessage(BinaryMessage message, boolean outbound)
    {
        mMessage = message;
        mOutbound = outbound;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getPDUType());
        return sb.toString();
    }

    /**
     * Binary message sequence
     */
    public BinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * Indicates if this is an outbound (versus inbound) SNDCP message
     */
    public boolean isOutbound()
    {
        return mOutbound;
    }

    /**
     * Indicates the type of SNDCP packet contained in this message
     */
    public PDUType getPDUType()
    {
        return getPDUType(getMessage(), isOutbound());
    }

    /**
     * Determines PDU type of the message
     *
     * @param message containing a PDU type field
     * @param outbound where true is outbound and false is inbound
     * @return pdu type
     */
    public static PDUType getPDUType(BinaryMessage message, boolean outbound)
    {
        return PDUType.fromValue(message.getInt(PDU_TYPE), outbound);
    }

}
