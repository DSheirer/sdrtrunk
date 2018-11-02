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

package io.github.dsheirer.identifier.ipv4;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.protocol.Protocol;

/**
 * APCO25 IPV4 Address
 */
public class APCO25IpAddress extends IPV4Address
{
    public APCO25IpAddress(int ipAddress, Role role)
    {
        super(ipAddress, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    public static APCO25IpAddress createTo(int ipAddress)
    {
        return new APCO25IpAddress(ipAddress, Role.TO);
    }

    public static APCO25IpAddress createFrom(int ipAddress)
    {
        return new APCO25IpAddress(ipAddress, Role.FROM);
    }
}
