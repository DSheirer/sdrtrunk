/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
package io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * Base class for LRRP tokens whose value is a 7-bit variable length integer (see VarInt).
 *
 * The value starts in the byte that follows the token identifier and occupies one or more bytes.  The read is
 * bounded by the number of payload bytes that remain, so a token at the end of the payload cannot consume bytes
 * that belong to the next packet.
 */
public abstract class VarIntToken extends Token
{
    private static final int VALUE_START = 8;
    private int mRemainingCharacterCount;

    /**
     * Constructs an instance
     *
     * @param message containing the token and value
     * @param offset to the start of the token identifier
     * @param remainingCharacterCount number of payload bytes remaining, counting from the token identifier
     */
    public VarIntToken(CorrectedBinaryMessage message, int offset, int remainingCharacterCount)
    {
        super(message, offset);
        mRemainingCharacterCount = remainingCharacterCount;
    }

    /**
     * Maximum number of value bytes that can follow the token identifier.
     */
    private int getValueLimit()
    {
        return Math.max(0, mRemainingCharacterCount - 1);
    }

    /**
     * Number of bytes occupied by the value, exclusive of the token identifier.
     */
    public int getValueLength()
    {
        return VarInt.length(getMessage(), getOffset() + VALUE_START, getValueLimit());
    }

    /**
     * Indicates if a value follows the token identifier.
     */
    public boolean hasValue()
    {
        return getValueLength() > 0;
    }

    /**
     * Decoded integer value, or zero when the token has no value.
     */
    public int getValue()
    {
        return VarInt.value(getMessage(), getOffset() + VALUE_START, getValueLimit());
    }

    /**
     * Total token length: identifier plus the variable length value.
     */
    @Override
    public int getByteLength()
    {
        return 1 + getValueLength();
    }
}
