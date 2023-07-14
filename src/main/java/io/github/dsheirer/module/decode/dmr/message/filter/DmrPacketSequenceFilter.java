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

package io.github.dsheirer.module.decode.dmr.message.filter;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.dmr.message.data.packet.DMRPacketMessage;
import io.github.dsheirer.module.decode.dmr.message.data.packet.UDTShortMessageService;
import io.github.dsheirer.module.decode.ip.hytera.sds.HyteraUnknownPacket;
import io.github.dsheirer.module.decode.ip.hytera.sms.HyteraSmsPacket;
import io.github.dsheirer.module.decode.ip.mototrbo.ars.ARSPacket;
import io.github.dsheirer.module.decode.ip.mototrbo.lrrp.LRRPPacket;
import io.github.dsheirer.module.decode.ip.mototrbo.xcmp.XCMPPacket;
import java.util.function.Function;

/**
 * Filter for DMR packet sequence messages
 */
public class DmrPacketSequenceFilter extends Filter<IMessage,String>
{
    private static final String KEY_ARS = "Automatic Registration Service (ARS)";
    private static final String KEY_HYTERA_SMS = "Hytera Short Message Service (SMS)";
    private static final String KEY_HYTERA_UNKNOWN = "Hytera Unknown";
    private static final String KEY_LRRP = "Location Request/Response Protocol (LRRP)";
    private static final String KEY_UDT_SMS = "Unified Data Transport - Short Message Service";
    private static final String KEY_XCMP = "Extensible Command Message Protocol (XCMP)";
    private static final String KEY_UNKNOWN = "Other/Unknown";

    private KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructs an instance
     */
    public DmrPacketSequenceFilter()
    {
        super("Packet Sequence Messages");
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof DMRPacketMessage && super.canProcess(message);
    }

    @Override
    public Function<IMessage, String> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    private class KeyExtractor implements Function<IMessage,String>
    {
        @Override
        public String apply(IMessage message)
        {
            if(message instanceof UDTShortMessageService)
            {
                return KEY_UDT_SMS;
            }
            else if(message instanceof DMRPacketMessage packetMessage)
            {
                if(packetMessage.getPacket() instanceof ARSPacket)
                {
                    return KEY_ARS;
                }
                else if(packetMessage.getPacket() instanceof LRRPPacket)
                {
                    return KEY_LRRP;
                }
                else if(packetMessage.getPacket() instanceof XCMPPacket)
                {
                    return KEY_XCMP;
                }
                else if(packetMessage.getPacket() instanceof HyteraSmsPacket)
                {
                    return KEY_HYTERA_SMS;
                }
                else if(packetMessage.getPacket() instanceof HyteraUnknownPacket)
                {
                    return KEY_HYTERA_UNKNOWN;
                }

                //TODO: finish this
            }

            return KEY_UNKNOWN;
        }
    }
}
