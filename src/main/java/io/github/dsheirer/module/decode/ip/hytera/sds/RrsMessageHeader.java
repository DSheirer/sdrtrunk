/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.ip.hytera.sds;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * Radio Registration Service (RRS) Message Header
 */
public class RrsMessageHeader extends HyteraToken
{
    private static int[] MESSAGE_LENGTH_BCD_TENS = new int[]{16, 17, 18, 19};
    private static int[] MESSAGE_LENGTH_BCD_ONES = new int[]{20, 21, 22, 23};

    /**
     * Constructs an instance
     *
     * @param message containing both the token, run-length, and content
     */
    public RrsMessageHeader(CorrectedBinaryMessage message)
    {
        super(message);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("RRS HEADER LENGTH:").append(getMessageLength());
        return sb.toString();
    }

    @Override
    HyteraTokenType getTokenType()
    {
        return HyteraTokenType.RADIO_REGISTRATION_SERVICE_HEADER;
    }

    /**
     * Overall message length in bytes
     * @return message length.
     */
    public int getMessageLength()
    {
        int length = mMessage.getInt(MESSAGE_LENGTH_BCD_TENS) * 10;
        length += mMessage.getInt(MESSAGE_LENGTH_BCD_ONES);
        return length;
    }
}
