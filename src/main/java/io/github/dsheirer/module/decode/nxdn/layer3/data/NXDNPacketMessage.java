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

package io.github.dsheirer.module.decode.nxdn.layer3.data;

import io.github.dsheirer.module.decode.nxdn.NXDNMessage;

/**
 * NXDN packet message
 */
public abstract class NXDNPacketMessage extends NXDNMessage
{
    private PacketSequence mPacketSequence;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param ran
     * @param lich
     */
    public NXDNPacketMessage(PacketSequence packetSequence)
    {
        super(packetSequence.getMessage(), packetSequence.getTimestamp(), packetSequence.getRAN(), packetSequence.getLICH());
        mPacketSequence = packetSequence;
    }

    /**
     * Packet sequence for this message
     * @return packet sequence
     */
    public PacketSequence getPacketSequence()
    {
        return mPacketSequence;
    }
}
