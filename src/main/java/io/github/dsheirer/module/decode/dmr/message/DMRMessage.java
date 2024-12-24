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

package io.github.dsheirer.module.decode.dmr.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.message.TimeslotMessage;
import io.github.dsheirer.protocol.Protocol;

/**
 * Base DMR Message
 */
public abstract class DMRMessage extends TimeslotMessage
{
    /**
     * Constructs an instance
     * @param timestamp for the message
     */
    public DMRMessage(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timeslot, timestamp);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.DMR;
    }
}
