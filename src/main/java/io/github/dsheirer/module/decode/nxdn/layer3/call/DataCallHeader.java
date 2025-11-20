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

package io.github.dsheirer.module.decode.nxdn.layer3.call;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.LongField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.PacketInformation;
import java.util.List;

/**
 * Data call header message
 */
public class DataCallHeader extends DataCall
{
    private static final int OFFSET_PACKET_INFORMATION = OCTET_8;
    private static final LongField INITIALIZATION_VECTOR = LongField.length64(OCTET_11);
    private PacketInformation mPacketInformation;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public DataCallHeader(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
        sb.append("DATA CALL HEADER");
        sb.append(" FROM:").append(getSource());
        sb.append(" TO:").append(getDestination());
        sb.append(" ").append(getPacketInformation());

        if(getEncryptionKeyIdentifier().isEncrypted())
        {
            sb.append(" ").append(getEncryptionKeyIdentifier());
            sb.append(" IV:").append(getInitializationVector());
        }

        return sb.toString();
    }

    /**
     * Packet information field
     * @return packet information
     */
    public PacketInformation getPacketInformation()
    {
        if(mPacketInformation == null)
        {
            mPacketInformation = new PacketInformation(getMessage(), OFFSET_PACKET_INFORMATION, true);
        }

        return mPacketInformation;
    }

    /**
     * Initialization vector as hex string.
     */
    public String getInitializationVector()
    {
        return Long.toHexString(getMessage().getLong(INITIALIZATION_VECTOR)).toUpperCase();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of(getSource(), getDestination(), getEncryptionKeyIdentifier());
    }
}
