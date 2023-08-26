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

package io.github.dsheirer.module.decode.ip.hytera.shortdata;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.message.data.header.hytera.HyteraDataEncryptionHeader;
import io.github.dsheirer.module.decode.dmr.message.data.packet.PacketSequence;
import io.github.dsheirer.module.decode.ip.IHeader;
import io.github.dsheirer.module.decode.ip.IPacket;
import java.util.List;

/**
 * Hytera Short Data Packet Sequence
 */
public class HyteraShortDataPacket implements IPacket
{
    private final PacketSequence mPacketSequence;
    private CorrectedBinaryMessage mMessage;

    /**
     * Constructor
     * @param packetSequence containing the headers and data blocks
     * @param payload containing the reconstructed short data message from the data blocks.
     */
    public HyteraShortDataPacket(PacketSequence packetSequence, CorrectedBinaryMessage payload)
    {
        mPacketSequence = packetSequence;
        mMessage = payload;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CC:").append(getPacketSequence().getPacketSequenceHeader().getSlotType().getColorCode());
        sb.append(" HYTERA SHORT DATA MESSAGE FM:").append(getPacketSequence().getPacketSequenceHeader().getSourceLLID());
        sb.append(" TO:").append(getPacketSequence().getPacketSequenceHeader().getDestinationLLID());

        if(getPacketSequence().isEncrypted())
        {
             HyteraDataEncryptionHeader hdeh = (HyteraDataEncryptionHeader)getPacketSequence().getProprietaryDataHeader();
             sb.append(" ENCRYPTED ALGORITHM:").append(hdeh.getAlgorithm());
             sb.append(" KEY:").append(hdeh.getKeyId());
             sb.append(" IV:").append(hdeh.getIV());
        }

        sb.append(" SHORT DATA:").append(getMessage().toHexString());
        return sb.toString();

    }

    @Override
    public IHeader getHeader()
    {
        return null;
    }

    @Override
    public IPacket getPayload()
    {
        return null;
    }

    @Override
    public boolean hasPayload()
    {
        return false;
    }

    /**
     * Payload message
     * @return message
     */
    public CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * Captured packet sequence
     * @return packet sequence
     */
    public PacketSequence getPacketSequence()
    {
        return mPacketSequence;
    }

    /**
     * Indicates if this packet is encrypted, meaning it contains a Hytera Data Encryption Header.
     * @return true if encrypted
     */
    public boolean isEncrypted()
    {
        return getPacketSequence().isEncrypted();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return getPacketSequence().getPacketSequenceHeader().getIdentifiers();
    }
}
