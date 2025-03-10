/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.ip.mototrbo.tms;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.ip.Header;
import java.util.Collections;
import java.util.List;

/**
 * Text Messaging Service (TMS) Header
 *
 * The payload contains 4x bytes of ?UTF-8? values (0-9) indicating the message length followed by UTF-16 Little Endian
 * characters.
 */
public class TMSHeader extends Header
{
    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param message containing the header
     * @param offset to the header within the message
     */
    public TMSHeader(BinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Character count is stored in the first 4x bytes as UTF-8 values.
     */
    public int getCharacterCount()
    {
        StringBuilder sb = new StringBuilder();
        sb.append((char)getMessage().getByte(getOffset() + 0));
        sb.append((char)getMessage().getByte(getOffset() + 8));
        sb.append((char)getMessage().getByte(getOffset() + 16));
        sb.append((char)getMessage().getByte(getOffset() + 24));

        try
        {
            return Integer.parseInt(sb.toString());
        }
        catch(Exception e)
        {
            return 0;
        }
    }

    /**
     * Length of this header in bits
     */
    @Override
    public int getLength()
    {
        return 32;
    }

    /**
     * Identifiers for this packet
     */
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
