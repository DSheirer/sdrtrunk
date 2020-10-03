/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.ip.cellocator;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.ip.IPacket;
import io.github.dsheirer.module.decode.ip.Packet;

import java.util.Collections;
import java.util.List;

/**
 * Cellocator MCGP Packet
 */
public abstract class MCGPPacket extends Packet
{
    private MCGPHeader mHeader;

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param header for this message
     * @param message containing the packet
     * @param offset to the packet within the message
     */
    public MCGPPacket(MCGPHeader header, CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
        mHeader = header;
    }

    @Override
    public MCGPHeader getHeader()
    {
        return mHeader;
    }

    /**
     * Unit ID (to/from) for this packet
     */
    protected abstract CellocatorRadioIdentifier getRadioId();

    @Override
    public IPacket getPayload()
    {
        return null;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        CellocatorRadioIdentifier radioIdentifier = getRadioId();

        if(radioIdentifier != null)
        {
            return Collections.singletonList(radioIdentifier);
        }

        return Collections.emptyList();
    }
}
