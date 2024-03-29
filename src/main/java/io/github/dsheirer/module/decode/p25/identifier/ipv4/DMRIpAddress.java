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

package io.github.dsheirer.module.decode.p25.identifier.ipv4;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.ipv4.IPV4Address;
import io.github.dsheirer.identifier.ipv4.IPV4Identifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * DMR IPV4 Address
 */
public class DMRIpAddress extends IPV4Identifier
{
    public DMRIpAddress(IPV4Address ipAddress, Role role)
    {
        super(ipAddress, role);
    }

    public Protocol getProtocol()
    {
        return Protocol.DMR;
    }

    public static DMRIpAddress createTo(int value)
    {
        return new DMRIpAddress(new IPV4Address(value), Role.TO);
    }

    public static DMRIpAddress createFrom(int value)
    {
        return new DMRIpAddress(new IPV4Address(value), Role.FROM);
    }
}
