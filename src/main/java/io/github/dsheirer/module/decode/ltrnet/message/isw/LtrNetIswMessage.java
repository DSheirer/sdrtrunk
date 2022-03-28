/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.module.decode.ltrnet.message.isw;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRC;
import io.github.dsheirer.edac.CRCLTR;
import io.github.dsheirer.message.MessageDirection;
import io.github.dsheirer.module.decode.ltrnet.LtrNetMessageType;
import io.github.dsheirer.module.decode.ltrnet.message.LtrNetMessage;

public abstract class LtrNetIswMessage extends LtrNetMessage
{
    /**
     * LTR-Net Inbound (mobile - to tower) Message
     */
    public LtrNetIswMessage(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, MessageDirection.ISW, timestamp);

        /**
         * If the CRC fails, test for a transmitted CRC of 127 and then check
         * the Free field for special messages
         */
        if(mCRC == CRC.FAILED_CRC && CRCLTR.getTransmittedChecksum(mMessage) == 127)
        {
            int free = getFree(getMessage());

            if(free == 31 || free == 23)
            {
                mCRC = CRC.PASSED;
            }
        }
    }

    /**
     * Determines the ISW message type
     */
    public static LtrNetMessageType getMessageType(CorrectedBinaryMessage message)
    {
        int channel = getChannel(message);

        if(channel == 31)
        {
            return LtrNetMessageType.ISW_CALL_END;
        }
        else if(channel > 20)
        {
            switch(channel)
            {
                case 24:
                    return LtrNetMessageType.ISW_UNIQUE_ID;
                case 27:
                    return LtrNetMessageType.ISW_REGISTRATION_REQUEST_ESN_LOW;
                case 29:
                    return LtrNetMessageType.ISW_REGISTRATION_REQUEST_ESN_HIGH;
            }
        }
        else if(channel > 0)
        {
            int free = getFree(message);

            switch(free)
            {
                case 21:
                    return LtrNetMessageType.ISW_CALL_START;
                case 23:
                    return LtrNetMessageType.ISW_CALL_END;
                case 31:
                    return LtrNetMessageType.ISW_REQUEST_ACCESS;
            }
        }

        return LtrNetMessageType.ISW_UNKNOWN;
    }
}
