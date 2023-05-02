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

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.ip.IPacket;
import java.util.Collections;
import java.util.List;

/**
 * Hytera SDS Long Data Message with unknown encoded payload
 */
public class HyteraUnknownPacket implements IPacket
{
    private HyteraTokenHeader mHeader;

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param header to the packet within the message
     */
    public HyteraUnknownPacket(HyteraTokenHeader header)
    {
        mHeader = header;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("HYTERA UNKNOWN LONG DATA SERVICE TOKEN MESSAGE:").append(getHeader().toString());
        return sb.toString();
    }

    @Override
    public HyteraTokenHeader getHeader()
    {
        return mHeader;
    }

    @Override
    public IPacket getPayload() {return null;}
    @Override
    public boolean hasPayload() {return false;}

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
