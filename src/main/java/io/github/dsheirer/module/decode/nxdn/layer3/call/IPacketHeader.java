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

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNRadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.type.PacketInformation;
import java.util.List;

/**
 * Interface for data sequence headers (ie short data and data call).
 */
public interface IPacketHeader
{
    /**
     * Packet information for the sequence.
     */
    public PacketInformation getPacketInformation();

    /**
     * Source radio identifier
     */
    NXDNRadioIdentifier getSource();

    /**
     * Destination, either radio or talkgroup
     */
    IntegerIdentifier getDestination();

    /**
     * Optional encryption information.
     */
    EncryptionKeyIdentifier getEncryptionKeyIdentifier();

    /**
     * Message timestamp
     */
    long getTimestamp();

    /**
     * Link information channel (LICH)
     */
    LICH getLICH();

    /**
     * Radio access network (RAN)
     */
    int getRAN();

    /**
     * Identifiers (source and destination)
     */
    List<Identifier> getIdentifiers();
}
