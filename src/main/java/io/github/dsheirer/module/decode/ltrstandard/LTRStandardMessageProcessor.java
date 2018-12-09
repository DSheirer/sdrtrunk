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
package io.github.dsheirer.module.decode.ltrstandard;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRC;
import io.github.dsheirer.edac.CRCLTR;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.MessageDirection;
import io.github.dsheirer.module.decode.ltrstandard.message.CallEndMessage;
import io.github.dsheirer.module.decode.ltrstandard.message.CallMessage;
import io.github.dsheirer.module.decode.ltrstandard.message.IdleMessage;
import io.github.dsheirer.module.decode.ltrstandard.message.LTRStandardMessage;
import io.github.dsheirer.module.decode.ltrstandard.message.UnknownMessage;
import io.github.dsheirer.sample.Listener;

public class LTRStandardMessageProcessor implements Listener<CorrectedBinaryMessage>
{
    private MessageDirection mDirection;
    private AliasList mAliasList;
    private Listener<IMessage> mMessageListener;

    /**
     * Processes raw binary messages and converts them to the correct message class
     *
     * @param direction - inbound (ISW) or outbound (OSW)
     * @param list - alias list
     */
    public LTRStandardMessageProcessor(MessageDirection direction, AliasList list)
    {
        mDirection = direction;
        mAliasList = list;
    }

    @Override
    public void receive(CorrectedBinaryMessage binaryMessage)
    {
        if(mMessageListener != null)
        {
            //Inbound Status Word (ISW) is a bit-flipped version of the Outbound
            //(OSW), so flip the bits and process it as an OSW
            if(mDirection == MessageDirection.ISW)
            {
                binaryMessage.flip(0, 40);
            }

            CRC crc = CRCLTR.check(binaryMessage, mDirection);

            if(crc != CRC.FAILED_CRC &&
                crc != CRC.FAILED_PARITY)
            {
                LTRStandardMessage message;

                int channel = binaryMessage.getInt(LTRStandardMessage.CHANNEL);
                int home = binaryMessage.getInt(LTRStandardMessage.HOME_REPEATER);
                int free = binaryMessage.getInt(LTRStandardMessage.FREE);
                int group = binaryMessage.getInt(LTRStandardMessage.GROUP);

                if(isValidChannel(channel) &&
                    isValidChannel(home) &&
                    isValidChannel(free))
                {
                    if(channel == free && group == 255)
                    {
                        message = new IdleMessage(binaryMessage, mDirection, mAliasList, crc);
                    }
                    else
                    {
                        message = new CallMessage(binaryMessage, mDirection, mAliasList, crc);
                    }
                }
                else if(channel == 31 &&
                    isValidChannel(home) &&
                    isValidChannel(free))
                {
                    message = new CallEndMessage(binaryMessage, mDirection, mAliasList, crc);
                }
                else
                {
                    message = new UnknownMessage(binaryMessage, mDirection, mAliasList, crc);
                }

                mMessageListener.receive(message);
            }
        }
    }

    /**
     * Checks the channel (LCN) number to ensure it is in the range 1 -20
     */
    private boolean isValidChannel(int channel)
    {
        return (1 <= channel && channel <= 20);
    }

    public void setMessageListener(Listener<IMessage> listener)
    {
        mMessageListener = listener;
    }

    public void removeMessageListener()
    {
        mMessageListener = null;
    }
}