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

package io.github.dsheirer.module.decode.ip.icmp;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.ip.Header;

/**
 * Internet Control Message Protocol (ICMP) Header
 */
public class ICMPHeader extends Header
{
    private static final int[] TYPE = {0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] CODE = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] TYPE_CODE = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param message containing the header
     * @param offset to the header within the message
     */
    public ICMPHeader(BinaryMessage message, int offset)
    {
        super(message, offset);
        checkValid();
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        ICMPTypeCode typeCode = getTypeCode();

        if(typeCode != ICMPTypeCode.UNKNOWN)
        {
            sb.append("ICMP ").append(typeCode);
        }
        else
        {
            sb.append("ICMP UNKNOWN TYPE CODE:").append(getTypeCodeValue());
        }
        return sb.toString();
    }

    public ICMPTypeCode getTypeCode()
    {
        return ICMPTypeCode.fromValue(getTypeCodeValue());
    }

    public int getTypeCodeValue()
    {
        return getMessage().getInt(TYPE_CODE, getOffset());
    }

    public int getType()
    {
        return getMessage().getInt(TYPE, getOffset());
    }

    public int getCode()
    {
        return getMessage().getInt(CODE, getOffset());
    }

    private void checkValid()
    {
        setValid(getMessage().size() >= getOffset() + getLength());
    }

    @Override
    public int getLength()
    {
        return 64;
    }
}
