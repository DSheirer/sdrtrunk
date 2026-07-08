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

package io.github.dsheirer.module.decode.nxdn.layer3.mobility;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNEncryptionKey;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.call.IPacketHeader;
import io.github.dsheirer.module.decode.nxdn.layer3.type.PacketInformation;
import io.github.dsheirer.protocol.Protocol;

/**
 * Header for writing data to a SU radio
 */
public class DataWriteHeader extends DataWrite implements IPacketHeader
{
    private static final int PACKET_INFORMATION_OFFSET = OCTET_8;
    private PacketInformation mPacketInformation;;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type
     * @param ran
     * @param lich
     */
    public DataWriteHeader(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
        sb.append("DATA WRITE HEADER");
        if(getDataWriteOption().isEmergency())
        {
            sb.append(" EMERGENCY");
        }
        sb.append(" FROM:").append(getSource());
        sb.append(" TO:").append(getDestination());
        sb.append(" ").append(getPacketInformation());
        return sb.toString();
    }

    /**
     * Packet information
     */
    public PacketInformation getPacketInformation()
    {
        if(mPacketInformation == null)
        {
            mPacketInformation = new PacketInformation(getMessage(), PACKET_INFORMATION_OFFSET, true);
        }

        return mPacketInformation;
    }

    @Override
    public EncryptionKeyIdentifier getEncryptionKeyIdentifier()
    {
        return EncryptionKeyIdentifier.create(Protocol.NXDN, new NXDNEncryptionKey(0, 0));
    }
}
