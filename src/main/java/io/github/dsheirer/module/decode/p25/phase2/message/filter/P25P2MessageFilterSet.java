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

package io.github.dsheirer.module.decode.p25.phase2.message.filter;

import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.P25P2Message;

/**
 * Filter set for P25 Phase 2 messages
 */
public class P25P2MessageFilterSet extends FilterSet<IMessage>
{
    public P25P2MessageFilterSet()
    {
        super("P25 Phase 2 Message Filter");

        addFilter(new MacOpcodeMessageFilterSet());
        addFilter(new VoiceMessageFilter());
        addFilter(new P25P2OtherMessageFilter());
    }

    /**
     * Override default to descope handling to P25P2 or sync-loss messages.
     * @param message to test
     * @return true if the message can be processed
     */
    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof P25P2Message || message instanceof SyncLossMessage;
    }
}
