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

package io.github.dsheirer.module.decode.ip.mototrbo.ars;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import java.util.Collections;
import java.util.List;

public class UserRegistrationAcknowledge extends ARSHeader
{
    private static final int[] SESSION_TIME_OR_REASON = {25, 26, 27, 28, 29, 30, 31};

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param message containing the header
     * @param offset to the header within the message
     */
    public UserRegistrationAcknowledge(BinaryMessage message, int offset)
    {
        super(message, offset);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if(isSuccessful())
        {
            sb.append("USER REGISTRATION SUCCESS - SESSION TIME:").append(getSessionTime());
        }
        else
        {
            sb.append("USER REGISTRATION FAILED - REASON:").append(getFailureReason());
        }
        return sb.toString();
    }

    public boolean isSuccessful()
    {
        return !isAcknowledge();
    }

    public int getSessionTime()
    {
        if(hasHeaderExtension())
        {
            return getMessage().getInt(SESSION_TIME_OR_REASON, getOffset());
        }

        return 0;
    }

    public String getFailureReason()
    {
        if(hasHeaderExtension())
        {
            int reason = getMessage().getInt(SESSION_TIME_OR_REASON, getOffset());

            switch(reason)
            {
                case 0x01:
                    return "USER VALIDATION FAILED";
                case 0x02:
                    return "USER VALIDATION TIMEOUT";
                default:
                    return "TRANSMISSION FAILURE";
            }
        }

        return "NONE";
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
