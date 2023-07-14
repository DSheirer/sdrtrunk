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

package io.github.dsheirer.module.decode.p25.phase1.message.filter;

import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.umbtc.UMBTCMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.Opcode;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.TSBKMessage;
import java.util.List;

/**
 * Filter set for trunking messages
 */
public class TrunkingOpcodeMessageFilterSet extends FilterSet<IMessage>
{
    /**
     * Constructor
     */
    public TrunkingOpcodeMessageFilterSet()
    {
        super("Trunking Messages");
        addFilter(new TrunkingOpcodeMessageFilter("Mobile Data Requests", Opcode.MOBILE_DATA_REQUESTS));
        addFilter(new TrunkingOpcodeMessageFilter("Mobile Request/Response", Opcode.MOBILE_REQUEST_RESPONSE));
        addFilter(new TrunkingOpcodeMessageFilter("Mobile Voice Requests", Opcode.MOBILE_VOICE_REQUESTS));
        addFilter(new TrunkingOpcodeMessageFilter("Network/Channel Status", Opcode.NETWORK_CHANNEL_STATUS));
        addFilter(new TrunkingOpcodeMessageFilter("Network Data Grants", Opcode.NETWORK_DATA_GRANTS));
        addFilter(new TrunkingOpcodeMessageFilter("Network Command/Request/Response", Opcode.NETWORK_COMMAND_REQUEST_RESPONSE));
        addFilter(new TrunkingOpcodeMessageFilter("Network Voice Grants", Opcode.NETWORK_VOICE_GRANTS));
        addFilter(new TrunkingOpcodeMessageFilter("Vendor-Harris Messages", Opcode.HARRIS));
        addFilter(new TrunkingOpcodeMessageFilter("Vendor-Motorola Messages", Opcode.MOTOROLA));
        addFilter(new TrunkingOpcodeMessageFilter("Vendor-Unknown Messages", Opcode.UNKNOWN));

        List<Opcode> ungrouped = Opcode.getUngrouped();
        if(!ungrouped.isEmpty())
        {
            addFilter(new TrunkingOpcodeMessageFilter("Unknown Opcode Messages", ungrouped));
        }
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return (message instanceof TSBKMessage || message instanceof AMBTCMessage || message instanceof UMBTCMessage) &&
                super.canProcess(message);
    }
}
