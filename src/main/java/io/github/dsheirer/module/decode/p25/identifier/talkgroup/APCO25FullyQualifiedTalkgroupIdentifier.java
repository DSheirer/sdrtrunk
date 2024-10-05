/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.identifier.talkgroup;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.talkgroup.FullyQualifiedTalkgroupIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * Fully Qualified Radio Identifier (talkgroup) that includes WACN, System, and Radio Address.
 */
public class APCO25FullyQualifiedTalkgroupIdentifier extends FullyQualifiedTalkgroupIdentifier
{
    /**
     * Constructs an instance
     * @param groupAddress used on the local system as an alias to the fully qualified talkgroup.
     * @param wacn for the talkgroup home system.
     * @param system for the talkgroup home system.
     * @param id for the talkgroup within the home system.
     * @param role played by the talkgroup.
     */
    public APCO25FullyQualifiedTalkgroupIdentifier(int groupAddress, int wacn, int system, int id, Role role)
    {
        super(groupAddress, wacn, system, id, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    @Override
    public String toString()
    {
        return "ISSI " + super.toString();
    }

    /**
     * Creates an identifier for the fully qualified talkgroup using the FROM role.
     * @param groupAddress used on the local system as an alias to the fully qualified talkgroup.
     * @param wacn for the talkgroup home system.
     * @param system for the talkgroup home system.
     * @param id for the talkgroup within the home system.
     */
    public static APCO25FullyQualifiedTalkgroupIdentifier createFrom(int groupAddress, int wacn, int system, int id)
    {
        return new APCO25FullyQualifiedTalkgroupIdentifier(groupAddress, wacn, system, id, Role.FROM);
    }

    /**
     * Creates an identifier for the fully qualified talkgroup ising the TO role.
     * @param groupAddress used on the local system as an alias to the fully qualified talkgroup.
     * @param wacn for the talkgroup home system.
     * @param system for the talkgroup home system.
     * @param id for the talkgroup within the home system.
     */
    public static APCO25FullyQualifiedTalkgroupIdentifier createTo(int groupAddress, int wacn, int system, int id)
    {
        return new APCO25FullyQualifiedTalkgroupIdentifier(groupAddress, wacn, system, id, Role.TO);
    }

    /**
     * Creates an identifier for the fully qualified talkgroup ising the ANY role.
     * @param groupAddress used on the local system as an alias to the fully qualified talkgroup.
     * @param wacn for the talkgroup home system.
     * @param system for the talkgroup home system.
     * @param id for the talkgroup within the home system.
     */
    public static APCO25FullyQualifiedTalkgroupIdentifier createAny(int groupAddress, int wacn, int system, int id)
    {
        return new APCO25FullyQualifiedTalkgroupIdentifier(groupAddress, wacn, system, id, Role.ANY);
    }
}
