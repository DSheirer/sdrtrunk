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

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.module.decode.nxdn.NXDNMessage;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import java.util.List;

/**
 * Reassembled packet sequence
 */
public class PacketSequence extends NXDNMessage
{
    private final NXDNLayer3Message mHeader;
    private final EncryptionKeyIdentifier mEncryption;
    private final String mShortDataIV;

    /**
     * Constructs an instance
     * @param message payload
     * @param header message
     * @param encryption for the packet
     * @param iv short data initialization vector, optional, can be null.
     */
    public PacketSequence(CorrectedBinaryMessage message, NXDNLayer3Message header, EncryptionKeyIdentifier encryption, String iv)
    {
        super(message, header.getTimestamp(), header.getRAN(), header.getLICH());
        mHeader = header;
        mEncryption = encryption;
        mShortDataIV = iv;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
        sb.append("PACKET SEQUENCE ").append(getMessage().toHexString());
        sb.append(" ").append(mHeader);

        if(mEncryption != null && mEncryption.isEncrypted())
        {
            sb.append(" ").append(mEncryption);

            if(mShortDataIV != null)
            {
                sb.append(" IV:").append(mShortDataIV);
            }
        }

        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return mHeader.getIdentifiers();
    }
}
