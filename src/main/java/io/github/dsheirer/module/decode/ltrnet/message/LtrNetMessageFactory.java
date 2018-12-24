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

package io.github.dsheirer.module.decode.ltrnet.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.message.MessageDirection;
import io.github.dsheirer.module.decode.ltrnet.LtrNetMessageType;
import io.github.dsheirer.module.decode.ltrnet.message.isw.IswCallEnd;
import io.github.dsheirer.module.decode.ltrnet.message.isw.IswCallStart;
import io.github.dsheirer.module.decode.ltrnet.message.isw.IswUniqueId;
import io.github.dsheirer.module.decode.ltrnet.message.isw.IswUnknown;
import io.github.dsheirer.module.decode.ltrnet.message.isw.LtrNetIswMessage;
import io.github.dsheirer.module.decode.ltrnet.message.isw.RegistrationRequestEsnHigh;
import io.github.dsheirer.module.decode.ltrnet.message.isw.RegistrationRequestEsnLow;
import io.github.dsheirer.module.decode.ltrnet.message.isw.RequestAccess;
import io.github.dsheirer.module.decode.ltrnet.message.osw.ChannelMapHigh;
import io.github.dsheirer.module.decode.ltrnet.message.osw.ChannelMapLow;
import io.github.dsheirer.module.decode.ltrnet.message.osw.LtrNetOswMessage;
import io.github.dsheirer.module.decode.ltrnet.message.osw.NeighborId;
import io.github.dsheirer.module.decode.ltrnet.message.osw.OswCallEnd;
import io.github.dsheirer.module.decode.ltrnet.message.osw.OswCallStart;
import io.github.dsheirer.module.decode.ltrnet.message.osw.OswUnknown;
import io.github.dsheirer.module.decode.ltrnet.message.osw.ReceiveFrequencyHigh;
import io.github.dsheirer.module.decode.ltrnet.message.osw.ReceiveFrequencyLow;
import io.github.dsheirer.module.decode.ltrnet.message.osw.RegistrationAccept;
import io.github.dsheirer.module.decode.ltrnet.message.osw.SiteId;
import io.github.dsheirer.module.decode.ltrnet.message.osw.SystemIdle;
import io.github.dsheirer.module.decode.ltrnet.message.osw.TransmitFrequencyHigh;
import io.github.dsheirer.module.decode.ltrnet.message.osw.TransmitFrequencyLow;

/**
 * Message factory for creating LTR-Net messages
 */
public class LtrNetMessageFactory
{
    public static LtrNetMessage create(MessageDirection messageDirection, CorrectedBinaryMessage message, long timestamp)
    {
        switch(messageDirection)
        {
            case ISW:
                //Flip all of the message bits since ISW messages are inverted
                message.flip(0, 40);

                LtrNetMessageType iswType = LtrNetIswMessage.getMessageType(message);

                switch(iswType)
                {
                    case ISW_CALL_END:
                        return new IswCallEnd(message, timestamp);
                    case ISW_CALL_START:
                        return new IswCallStart(message, timestamp);
                    case ISW_REGISTRATION_REQUEST_ESN_HIGH:
                        return new RegistrationRequestEsnHigh(message, timestamp);
                    case ISW_REGISTRATION_REQUEST_ESN_LOW:
                        return new RegistrationRequestEsnLow(message, timestamp);
                    case ISW_REQUEST_ACCESS:
                        return new RequestAccess(message, timestamp);
                    case ISW_UNIQUE_ID:
                        return new IswUniqueId(message, timestamp);
                    case ISW_UNKNOWN:
                    default:
                        return new IswUnknown(message, timestamp);
                }
            case OSW:
                LtrNetMessageType oswType = LtrNetOswMessage.getMessageType(message);

                switch(oswType)
                {
                    case OSW_CALL_END:
                        return new OswCallEnd(message, timestamp);
                    case OSW_CALL_START:
                        return new OswCallStart(message, timestamp);
                    case OSW_CHANNEL_MAP_HIGH:
                        return new ChannelMapHigh(message, timestamp);
                    case OSW_CHANNEL_MAP_LOW:
                        return new ChannelMapLow(message, timestamp);
                    case OSW_SYSTEM_IDLE:
                        return new SystemIdle(message, timestamp);
                    case OSW_NEIGHBOR_ID:
                        return new NeighborId(message, timestamp);
                    case OSW_RECEIVE_FREQUENCY_HIGH:
                        return new ReceiveFrequencyHigh(message, timestamp);
                    case OSW_RECEIVE_FREQUENCY_LOW:
                        return new ReceiveFrequencyLow(message, timestamp);
                    case OSW_REGISTRATION_ACCEPT:
                        return new RegistrationAccept(message, timestamp);
                    case OSW_SITE_ID:
                        return new SiteId(message, timestamp);
                    case OSW_TRANSMIT_FREQUENCY_HIGH:
                        return new TransmitFrequencyHigh(message, timestamp);
                    case OSW_TRANSMIT_FREQUENCY_LOW:
                        return new TransmitFrequencyLow(message, timestamp);
                    case OSW_UNKNOWN:
                    default:
                        return new OswUnknown(message, timestamp);
                }
        }

        return null;
    }
}
