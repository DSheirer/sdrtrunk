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

package io.github.dsheirer.module.decode.ip.ars;

import io.github.dsheirer.bits.BinaryMessage;

public class UserRegistration extends ARSHeader
{
    private static final int HEADER_EXTENSION_FLAG = 24;
    private static final int[] EVENT = {25, 26};
    private static final int[] ENCODING = {27, 28, 29, 30, 31};
    private static final int DEVICE_IDENTIFIER_START = 24;
    private static final int DEVICE_IDENTIFIER_START_EXTENDED_HEADER = 32;

    private static final int[] BYTE_VALUE = {0, 1, 2, 3, 4, 5, 6, 7};

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param message containing the header
     * @param offset to the header within the message
     */
    public UserRegistration(BinaryMessage message, int offset)
    {
        super(message, offset);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("USER REGISTRATION");

        if(isValid())
        {
            sb.append(" ").append(getPayload());
        }
        else
        {
            sb.append(" - ERROR INVALID MESSAGE LENGTH");
        }
        return sb.toString();
    }

    /**
     * Device, user and password values contained in the registration packet.
     */
    public String getPayload()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("DEVICE:");

        int pointer = getOffset();

        if(hasHeaderExtension())
        {
            pointer += DEVICE_IDENTIFIER_START_EXTENDED_HEADER;
        }
        else
        {
            pointer += DEVICE_IDENTIFIER_START;
        }

        int identifierSize = getMessage().getInt(BYTE_VALUE, pointer += 8);

        if(identifierSize > 0)
        {
            for(int x = 0; x < identifierSize; x++)
            {
                sb.append(getCharacter(pointer += 8));
            }
        }
        else
        {
            sb.append("(none)");
        }

        sb.append(" USER:");

        int userIdentifierSize = getMessage().getInt(BYTE_VALUE, pointer += 8);

        if(userIdentifierSize > 0)
        {
            for(int x = 0; x < userIdentifierSize; x++)
            {
                sb.append(getCharacter(pointer += 8));
            }
        }
        else
        {
            sb.append("(none)");
        }

        sb.append(" PASSWORD:");

        int passwordSize = getMessage().getInt(BYTE_VALUE, pointer += 8);

        if(passwordSize > 0)
        {
            for(int x = 0; x < passwordSize; x++)
            {
                sb.append(getCharacter(pointer += 8));
            }
        }
        else
        {
            sb.append("(none)");
        }

        return sb.toString();
    }

    /**
     * Returns a UTF-8 encoded character that starts at the specified offset
     *
     * @param offset of the start of the 8-bit UTF-8 encoded character
     * @return character
     */
    private char getCharacter(int offset)
    {
        return (char)getMessage().getByte(BYTE_VALUE, offset);
    }
}
