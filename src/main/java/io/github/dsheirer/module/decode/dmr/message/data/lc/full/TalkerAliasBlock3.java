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

package io.github.dsheirer.module.decode.dmr.message.data.lc.full;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import java.util.Collections;
import java.util.List;

/**
 * Talker alias continuation block 3.
 */
public class TalkerAliasBlock3 extends FullLCMessage
{
    private static final int PAYLOAD_START = 16;
    private static final int PAYLOAD_END = 72;

    /**
     * Constructs an instance
     *
     * @param message for link control payload
     * @param timestamp for the message
     * @param timeslot where the message was transmitted
     */
    public TalkerAliasBlock3(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
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
        sb.append("FLC TALKER ALIAS BLOCK 3");
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }
}
