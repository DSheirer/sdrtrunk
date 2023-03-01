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
 * Payload bytes
 */
public class Payload extends HyteraToken
{
    private static final int[] CONTENT_LENGTH = new int[]{16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    private static final int PAYLOAD_START = 32;

    /**
     * Constructs an instance
     *
     * @param message containing both the token, run-length, and content
     */
    public Payload(CorrectedBinaryMessage message)
    {
        super(message);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("PAYLOAD:").append(getPayload().toHexString());
        return sb.toString();
    }

    public CorrectedBinaryMessage getPayload()
    {
        return mMessage.getSubMessage(PAYLOAD_START, mMessage.length() + 1);
    }

    @Override
    HyteraTokenType getTokenType()
    {
        return HyteraTokenType.PAYLOAD;
    }
}
