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

package io.github.dsheirer.module.decode.dmr.message.data.packet;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import io.github.dsheirer.module.decode.ip.IPacket;
import java.util.ArrayList;
import java.util.List;

/**
 * Packet message assembled from a packet sequence and decoded into an IPacket implementation
 */
public class DMRPacketMessage extends DMRMessage
{
    private PacketSequence mPacketSequence;
    private IPacket mPacket;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance
     * @param packetSequence that was transmitted
     * @param packet implementation
     * @param rawPacket that was extracted from the packet sequence
     * @param timeslot for the packet
     * @param timestamp for the initial preamble or packet header
     */
    public DMRPacketMessage(PacketSequence packetSequence, IPacket packet, CorrectedBinaryMessage rawPacket,
                            int timeslot, long timestamp)
    {
        super(rawPacket, timestamp, timeslot);
        mPacketSequence = packetSequence;
        mPacket = packet;
    }

    /**
     * Packet sequence containing preamble(s), headers, and data blocks.
     */
    public PacketSequence getPacketSequence()
    {
        return mPacketSequence;
    }

    /**
     * Packet implementation
     */
    public IPacket getPacket()
    {
        return mPacket;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CC:").append(getPacketSequence().getPacketSequenceHeader().getSlotType().getColorCode());
        sb.append(" FM:").append(getPacketSequence().getPacketSequenceHeader().getSourceLLID());
        sb.append(" TO:").append(getPacketSequence().getPacketSequenceHeader().getDestinationLLID());
        if(getPacketSequence().isEncrypted())
        {
            sb.append(" ENCRYPTED");
        }
        sb.append(" ").append(getPacket().toString());
        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();

            if(getPacketSequence().hasPacketSequenceHeader())
            {
                mIdentifiers.addAll(getPacketSequence().getPacketSequenceHeader().getIdentifiers());
            }

            mIdentifiers.addAll(getPacket().getIdentifiers());
        }

        return mIdentifiers;
    }
}
