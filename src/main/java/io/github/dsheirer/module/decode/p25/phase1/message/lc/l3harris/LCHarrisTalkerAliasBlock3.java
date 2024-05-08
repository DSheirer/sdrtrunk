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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * L3Harris Opcode 52 (0x34) Talker Alias Block 3.
 */
public class LCHarrisTalkerAliasBlock3 extends LCHarrisTalkerAliasBase
{
    private static final int PAYLOAD_START = OCTET_2_BIT_16;
    private static final int PAYLOAD_END = OCTET_9_BIT_72;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCHarrisTalkerAliasBlock3(CorrectedBinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("**CRC-FAILED** ");
        }

        if(isEncrypted())
        {
            sb.append(" ENCRYPTED");
        }
        else
        {
            sb.append("L3HARRIS TALKER ALIAS FRAGMENT 3/4:").append(getPayloadFragmentString());
        }
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }
}
