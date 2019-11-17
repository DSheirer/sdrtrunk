/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.filter;

import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.P25Message;

public class P25MessageFilterSet extends FilterSet<IMessage>
{
    public P25MessageFilterSet()
    {
        super("P25 Message Filter");

        addFilter(new AMBTCMessageFilter());
        addFilter(new HDUMessageFilter());
        addFilter(new IPPacketMessageFilter());
        addFilter(new LDUMessageFilter());
        addFilter(new PDUMessageFilter());
        addFilter(new SNDCPMessageFilter());
        addFilter(new SyncLossMessageFilter());
        addFilter(new TDUMessageFilter());
        addFilter(new TDULCMessageFilter());
        addFilter(new TSBKMessageFilterSet());
        addFilter(new UMBTCMessageFilter());
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof P25Message || message instanceof SyncLossMessage;
    }
}
