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

public class RegistrationAcknowledgement extends ARSHeader
{
    private static final int[] TIMER_OR_REASON = {25, 26, 27, 28, 29, 30, 31};

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param message containing the header
     * @param offset to the header within the message
     */
    public RegistrationAcknowledgement(BinaryMessage message, int offset)
    {
        super(message, offset);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(isValid())
        {
            sb.append("REGISTRATION ");

            if(isSuccessful())
            {
                sb.append("SUCCESS");

                int units = getPayloadValue();

                sb.append(" REFRESH IN:").append(units * 30).append("mins");
            }
            else
            {
                sb.append("FAIL");

                int units = getPayloadValue();

                if(units == 0)
                {
                    sb.append(" - UNAUTHORIZED DEVICE");
                }
                else
                {
                    sb.append(" REASON:").append(units);
                }
            }
        }
        else
        {
            sb.append("REGISTRATION ACKNOWLEDGE - ERROR - INVALID MESSAGE LENGTH");
        }

        return sb.toString();
    }

    /**
     * Indicates if the registration succeeded (true) or failed (false)
     */
    public boolean isSuccessful()
    {
        return !isAcknowledge();
    }

    /**
     * Payload value.  If successful, this is the number of 30 minute units until a registration
     * refresh is required.  If unsuccessful, this is the reason (0-unauthorized device).
     */
    public int getPayloadValue()
    {
        return getMessage().getInt(TIMER_OR_REASON, getOffset());
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
