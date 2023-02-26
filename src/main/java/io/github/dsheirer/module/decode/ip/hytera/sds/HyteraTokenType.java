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

/**
 * Hytera run-length encoding tokens.
 *
 * Each token implies a parseable field that is optionally followed by a 4-byte field content length and then followed
 * by the contents of the field.
 *
 * In the enum entries below, a field length of -1 indicates the opcode is followed by a 4-byte content length value.
 */
public enum HyteraTokenType
{
    //Note: this may be the message type, but is definitely followed by 4-byte total length field. Need more examples
    MESSAGE_HEADER(0x0201, 4),  //First 2 bytes are total message length, 2nd 2 bytes = unknown
    ID_MESSAGE(0x0001, -1),
    ID_SOURCE(0x0002, -1),
    ID_DESTINATION(0x0003, -1),
    ENCODING(0x0101, -1),
    PAYLOAD(0x0102, -1),
    UNKNOWN(-1, -1);

    private int mOpcode;
    private int mFieldLength;

    /**
     * Constructs an instance
     * @param opcode value
     * @param fieldLength in bytes, or -1 to indicate the opcode is followed by a 4-byte run length value.
     */
    HyteraTokenType(int opcode, int fieldLength)
    {
        mOpcode = opcode;
        mFieldLength = fieldLength;
    }

    /**
     * Opcode value
     * @return integer value of the opcode
     */
    public int getOpcode()
    {
        return mOpcode;
    }

    /**
     * Length of the field content in bytes that follow the opcode.  When this value is a -1, then the opcode is
     * followed by a 4-byte value that indicates the length of the content field that follows immediately after.
     * @return field length
     */
    public int getFieldLength()
    {
        return mFieldLength;
    }

    /**
     * Indicates if this is a fixed length field.
     */
    public boolean isFixedLength()
    {
        return mFieldLength >= 0;
    }

    /**
     * Lookup a token from the opcode value.
     * @param opcode value to lookup
     * @return token or UNKNOWN
     */
    public static HyteraTokenType fromOpcode(int opcode)
    {
        for(HyteraTokenType token: HyteraTokenType.values())
        {
            if(token.getOpcode() == opcode)
            {
                return token;
            }
        }

        return UNKNOWN;
    }
}
