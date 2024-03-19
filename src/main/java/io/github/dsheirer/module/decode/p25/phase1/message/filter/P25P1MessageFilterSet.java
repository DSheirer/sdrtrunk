/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
import io.github.dsheirer.filter.SyncLossMessageFilter;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;

/**
 * Filter set for P25 messages
 */
public class P25P1MessageFilterSet extends FilterSet<IMessage>
{
    public P25P1MessageFilterSet()
    {
        super("P25 Message Filter");

        addFilter(new HeaderMessageFilter());
        addFilter(new PacketMessageFilter());
        addFilter(new PDUMessageFilter());
        addFilter(new SNDCPMessageFilter());
        addFilter(new SyncLossMessageFilter());
        addFilter(new TerminatorMessageFilterSet());
        addFilter(new TrunkingOpcodeMessageFilterSet());
        addFilter(new VoiceMessageFilter());
        addFilter(new P25OtherMessageFilter());
    }

    /**
     * Override default to descope handling to P25 or sync-loss messages.
     * @param message to test
     * @return true if the message can be processed
     */
    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof P25P1Message || message instanceof SyncLossMessage;
    }
}
