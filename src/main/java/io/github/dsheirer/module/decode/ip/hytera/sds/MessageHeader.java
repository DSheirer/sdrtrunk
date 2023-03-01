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
 * Message Header
 *
 * Initial token that specifies the overall message length and some other unidentified bit fields.
 *
 * Note: I've only confirmed this is the start token for long SMS messages, but it may also be the start token for
 * other packet data messages like GPS or any other packet overlay application.
 */
public class MessageHeader extends HyteraToken
{
    private static int[] MESSAGE_LENGTH = new int[]{16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    private static int[] MESSAGE_FLAGS = new int[]{32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};

    /**
     * Constructs an instance
     *
     * @param message containing both the token, run-length, and content
     */
    public MessageHeader(CorrectedBinaryMessage message)
    {
        super(message);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MESSAGE HEADER LENGTH:").append(getMessageLength());
        sb.append(" FLAGS:").append(getFlags());
        return sb.toString();
    }

    @Override
    HyteraTokenType getTokenType()
    {
        return HyteraTokenType.MESSAGE_HEADER;
    }

    /**
     * Likely a set of bit fields that indicate optional flags for the message like:
     * - Acknowledge Receipt (boolean)
     * - Priority/Flash (boolean)
     * - Encryption Type (enum of at least 4 values, ie 2+ bits)
     * - Emergency (boolean)
     * @return
     */
    public String getFlags()
    {
        return mMessage.getHex(MESSAGE_FLAGS, 4);
    }

    /**
     * Overall message length in bytes
     * @return message length.
     */
    public int getMessageLength()
    {
        return mMessage.getInt(MESSAGE_LENGTH);
    }
}
