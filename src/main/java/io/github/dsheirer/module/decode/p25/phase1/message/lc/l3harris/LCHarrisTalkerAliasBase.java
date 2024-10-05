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
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import java.util.Collections;
import java.util.List;

/**
 * Base class for L3Harris talker alias messages
 */
public abstract class LCHarrisTalkerAliasBase extends LinkControlWord
{
    private static final int PAYLOAD_START = OCTET_2_BIT_16;
    private static final int PAYLOAD_END = OCTET_9_BIT_72;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCHarrisTalkerAliasBase(CorrectedBinaryMessage message)
    {
        super(message);
    }

    /**
     * Payload fragment carried by the header.
     * @return payload fragment.
     */
    public CorrectedBinaryMessage getPayloadFragment()
    {
        return getMessage().getSubMessage(PAYLOAD_START, PAYLOAD_END);
    }

    /**
     * Payload fragment as a string with empty space removed.
     * @return fragment
     */
    public String getPayloadFragmentString()
    {
        return new String(getPayloadFragment().toByteArray()).trim();
    }

    /**
     * List of identifiers contained in this message
     */
    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
