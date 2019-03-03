/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.module.decode.p25.phase2.message.mac;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase2.message.P25P2Message;

import java.util.Collections;
import java.util.List;

/**
 * Encoded MAC Information (EMI) Message base class
 */
public class MacMessage extends P25P2Message
{
    private static int[] MAC_OPCODE = {0, 1, 2};
    private static int[] OFFSET = {3, 4, 5};
    private static int[] RESERVED = {6, 7};

    private CorrectedBinaryMessage mMessage;

    /**
     * Constructs the message
     *
     * @param timestamp of the final bit of the message
     */
    public MacMessage(long timestamp, CorrectedBinaryMessage message)
    {
        super(timestamp);
        mMessage = message;
    }

    /**
     * Underlying binary message as transmitted and error-corrected
     */
    protected CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }


    /**
     * MAC opcode identifies the type of MAC PDU for this message
     */
    public MacOpcode getOpcode()
    {
        return fromMessage(getMessage());
    }

    /**
     * Lookup the MAC opcode from the message
     * @param message containing a mac opcode
     * @return opcode
     */
    public static MacOpcode fromMessage(CorrectedBinaryMessage message)
    {
        return MacOpcode.fromValue(message.getInt(MAC_OPCODE));
    }

    @Override
    public String toString()
    {
        return getMessage().toHexString() + (isValid() ? " VALID" : " INVALID") +
            " ERROR COUNT:" + getMessage().getCorrectedBitCount();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
