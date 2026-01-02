/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import java.util.List;

/**
 * Unknown message type
 */
public class UnknownMessage extends NXDNLayer3Message
{
    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     */
    public UnknownMessage(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type)
    {
        super(message, timestamp, type);
    }

    @Override
    public String toString()
    {
        return "UNKNOWN/UNRECOGNIZED MESSAGE TYPE " + getMessageType() + " MSG:" + getMessage().toHexString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of();
    }
}
