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

package io.github.dsheirer.module.decode.dmr.message.data.lc.full;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.message.type.TalkerAliasDataFormat;
import java.util.Collections;
import java.util.List;

/**
 * Talker alias header with format and length description and a fragment of the alias data.
 */
public class TalkerAliasHeader extends FullLCMessage
{
    private static final int[] FORMAT = new int[]{16, 17};
    private static final int[] LENGTH = new int[]{18, 19, 20, 21, 22};
    private static final int PAYLOAD_START = 23;
    private static final int PAYLOAD_END = 72;

    /**
     * Constructs an instance
     *
     * @param message for link control payload
     * @param timestamp for the message
     * @param timeslot where the message was transmitted
     */
    public TalkerAliasHeader(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
    }

    /**
     * Formatting used for the alias characters.
     * @return format
     */
    public TalkerAliasDataFormat getFormat()
    {
        return TalkerAliasDataFormat.fromValue(getMessage().getInt(FORMAT));
    }

    /**
     * Length of the alias in characters.
     * @return length in characters.
     */
    public int getCharacterLength()
    {
        return getMessage().getInt(LENGTH);
    }

    /**
     * Total bit length of the alias characters.
     */
    public int getTotalBitLength()
    {
        int total = getFormat().getBitsPerCharacter() * getCharacterLength();

        //Max payload bit length is 217 (49 + 56 + 56 + 56)
        if(total > 217)
        {
            return 217;
        }

        return total;
    }

    /**
     * Payload fragment carried by the header.
     * @return payload fragment.
     */
    public CorrectedBinaryMessage getPayloadFragment()
    {
        return getMessage().getSubMessage(PAYLOAD_START, PAYLOAD_END);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("[CRC-ERROR] ");
        }

        sb.append("FLC TALKER ALIAS HEADER");
        sb.append(" FORMAT:").append(getFormat());
        sb.append(" CHARACTERS:").append(getCharacterLength());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }
}
