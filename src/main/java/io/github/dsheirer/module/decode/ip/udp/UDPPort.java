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

package io.github.dsheirer.module.decode.ip.udp;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * UDP Port Number Identifier
 */
public class UDPPort extends IntegerIdentifier
{
    /**
     * Constructs an instance
     * @param value of the port
     * @param role of the port (TO/FROM)
     */
    public UDPPort(int value, Role role)
    {
        super(value, IdentifierClass.USER_NETWORK_PORT, Form.UDP_PORT, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.UDP;
    }

    /**
     * Utility method to create a FROM port
     */
    public static UDPPort createFrom(int value)
    {
        return new UDPPort(value, Role.FROM);
    }

    /**
     * Utility method to create a TO port
     */
    public static UDPPort createTo(int value)
    {
        return new UDPPort(value, Role.TO);
    }
}
