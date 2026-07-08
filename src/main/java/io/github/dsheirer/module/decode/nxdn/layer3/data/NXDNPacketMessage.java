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
    private final PacketSequence mPacketSequence;

    /**
     * Constructs an instance
     * @param sequence reassembled and CRC-32 checked.
     *
     */
    public NXDNPacketMessage(PacketSequence sequence)
    {
        super(sequence.getMessage(), sequence.getTimestamp(), sequence.getRAN(), sequence.getLICH());
        mPacketSequence = sequence;
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
