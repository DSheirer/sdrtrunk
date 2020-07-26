/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.ip;

import io.github.dsheirer.identifier.Identifier;
import java.util.List;

/**
 * Data packet interface
 */
public interface IPacket
{
    /**
     * Header for the data packet.
     */
    IHeader getHeader();

    /**
     * Optional payload for the packet
     * @return null if there is no payload
     */
    IPacket getPayload();

    /**
     * Indicates if this packet contains an optional payload
     */
    boolean hasPayload();

    /**
     * List of identifiers for this packet and any child payload packets.
     */
    List<Identifier> getIdentifiers();
}
