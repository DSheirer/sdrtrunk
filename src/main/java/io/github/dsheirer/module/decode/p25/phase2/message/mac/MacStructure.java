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

import java.util.List;

/**
 * Structure parsing parent class for MAC message payload structures.
 */
public abstract class MacStructure
{
    private static int[] OPCODE = {0, 1, 2, 3, 4, 5, 6, 7};

    private CorrectedBinaryMessage mMessage;
    private int mOffset;

    /**
     * Constructs a MAC structure parser
     *
     * @param message containing a MAC structure
     * @param offset in the message to the start of the structure
     */
    protected MacStructure(CorrectedBinaryMessage message, int offset)
    {
        mMessage = message;
        mOffset = offset;
    }

    /**
     * List of identifiers provided by this structure
     */
    public abstract List<Identifier> getIdentifiers();

    /**
     * Underlying binary message.
     *
     * @return message
     */
    protected CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * Offset to the start of the structure in the underlying binary message
     *
     * @return offset
     */
    protected int getOffset()
    {
        return mOffset;
    }

    /**
     * Opcode for the message argument
     * @param message containing a mac opcode message
     * @param offset into the message
     * @return opcode
     */
    public static MacOpcode getOpcode(CorrectedBinaryMessage message, int offset)
    {
        return MacOpcode.fromValue(message.getInt(OPCODE, offset));
    }

    /**
     * Numeric value of the opcode
     * @param message containing a mac opcode message
     * @param offset into the message to the start of the mac sequence
     * @return integer value
     */
    public static int getOpcodeNumber(CorrectedBinaryMessage message, int offset)
    {
        return message.getInt(OPCODE, offset);
    }

    /**
     * Opcode for this message
     */
    public MacOpcode getOpcode()
    {
        return getOpcode(getMessage(), getOffset());
    }

    /**
     * Opcode numeric value for this structure
     */
    public int getOpcodeNumber()
    {
        return getOpcodeNumber(getMessage(), getOffset());
    }
}
