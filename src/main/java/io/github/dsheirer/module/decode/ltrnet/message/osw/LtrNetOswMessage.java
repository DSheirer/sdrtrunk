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
package io.github.dsheirer.module.decode.ltrnet.message.osw;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.message.MessageDirection;
import io.github.dsheirer.module.decode.ltrnet.LtrNetMessageType;
import io.github.dsheirer.module.decode.ltrnet.message.LtrNetMessage;

public abstract class LtrNetOswMessage extends LtrNetMessage
{
    private static final int CHANNEL_FREQUENCY_MESSAGE_TYPE_BIT = 20;
    private static final int CHANNEL_MAP_MESSAGE_TYPE_BIT = 17;
    private static final int HOME_DIRECTED_GROUP_CALL = 29;
    private static final int HOME_CHANNEL_IN_USE_MUTE_AUDIO = 30;
    private static final int GROUP_DO_NOTHING = 253;
    private static final int GROUP_CWID = 254;
    private static final int GROUP_CHANNEL_IDLE = 255;

    private static final int FREE_ALL_CHANNELS_BUSY = 0;
    private static final int FREE_LTR_GROUP_CALL = 30;
    private static final int FREE_LTRNET_GROUP_CALL = 31;

    public LtrNetOswMessage(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, MessageDirection.OSW, timestamp);
    }

    public static LtrNetMessageType getMessageType(CorrectedBinaryMessage message)
    {
        int channel = getChannel(message);
        int home = getHomeRepeater(message);

        //LTR-Net messages
        if(home != 31 && (channel > 20 || home > 20))
        {
            switch(channel)
            {
                case 17:
                    return LtrNetMessageType.OSW_REGISTRATION_ACCEPT;
                case 18:
                    return LtrNetMessageType.OSW_SITE_ID;
                case 24:
                    if(message.get(CHANNEL_FREQUENCY_MESSAGE_TYPE_BIT))
                    {
                        return LtrNetMessageType.OSW_TRANSMIT_FREQUENCY_HIGH;
                    }
                    else
                    {
                        return LtrNetMessageType.OSW_TRANSMIT_FREQUENCY_LOW;
                    }
                case 25:
                    if(message.get(CHANNEL_FREQUENCY_MESSAGE_TYPE_BIT))
                    {
                        return LtrNetMessageType.OSW_RECEIVE_FREQUENCY_HIGH;
                    }
                    else
                    {
                        return LtrNetMessageType.OSW_RECEIVE_FREQUENCY_LOW;
                    }
                case 26:
                    return LtrNetMessageType.OSW_NEIGHBOR_ID;
                case 28:
                    if(message.get(CHANNEL_MAP_MESSAGE_TYPE_BIT))
                    {
                        return LtrNetMessageType.OSW_CHANNEL_MAP_HIGH;
                    }
                    else
                    {
                        return LtrNetMessageType.OSW_CHANNEL_MAP_LOW;
                    }
                case 31:
                    return LtrNetMessageType.OSW_CALL_END;
            }
        }
        else
        {
            int group = getGroup(message);

            if(group == GROUP_CHANNEL_IDLE)
            {
                return LtrNetMessageType.OSW_SYSTEM_IDLE;
            }
            else
            {
                return LtrNetMessageType.OSW_CALL_START;
            }
        }

        return LtrNetMessageType.OSW_UNKNOWN;
    }
}
