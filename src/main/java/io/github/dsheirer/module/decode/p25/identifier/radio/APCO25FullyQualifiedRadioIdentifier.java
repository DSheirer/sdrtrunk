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

package io.github.dsheirer.module.decode.p25.identifier.radio;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.radio.FullyQualifiedRadioIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * Fully Qualified Radio Identifier (talkgroup) that includes WACN, System, and Radio Address.
 */
public class APCO25FullyQualifiedRadioIdentifier extends FullyQualifiedRadioIdentifier
{
    /**
     * Constructs an instance
     * @param localAddress radio identifier.  This can be the same as the radio ID when the fully qualified radio
     * is not being aliased on a local radio system.
     * @param wacn of the home network for the radio.
     * @param system of the home network for the radio.
     * @param id of the radio within the home network.
     */
    public APCO25FullyQualifiedRadioIdentifier(int localAddress, int wacn, int system, int id, Role role)
    {
        super(localAddress, wacn, system, id, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    @Override
    public String toString()
    {
        if(isAliased())
        {
            return "ROAM " + super.toString();
        }
        else
        {
            return "ISSI " + super.toString();
        }
    }

    /**
     * Creates a fully qualified radio and assigns the FROM role.
     * @param localAddress radio identifier.  This can be the same as the radio ID when the fully qualified radio
     * is not being aliased on a local radio system.
     * @param wacn of the home network for the radio.
     * @param system of the home network for the radio.
     * @param id of the radio within the home network.
     * @return identifier
     */
    public static APCO25FullyQualifiedRadioIdentifier createFrom(int localAddress, int wacn, int system, int id)
    {
        return new APCO25FullyQualifiedRadioIdentifier(localAddress, wacn, system, id, Role.FROM);
    }

    /**
     * Creates a fully qualified radio and assigns the TO role.
     * @param localAddress radio identifier.  This can be the same as the radio ID when the fully qualified radio
     * is not being aliased on a local radio system.
     * @param wacn of the home network for the radio.
     * @param system of the home network for the radio.
     * @param id of the radio within the home network.
     * @return identifier
     */
    public static APCO25FullyQualifiedRadioIdentifier createTo(int localAddress, int wacn, int system, int id)
    {
        return new APCO25FullyQualifiedRadioIdentifier(localAddress, wacn, system, id, Role.TO);
    }
}
