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

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.protocol.Protocol;

/**
 * IPV4 Address
 */
public class IPV4Address implements IIdentifier
{
    private int mIPAddress;
    private Role mRole;

    public IPV4Address(int ipAddress, Role role)
    {
        mIPAddress = ipAddress;
        mRole = role;
    }

    @Override
    public Role getRole()
    {
        return mRole;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.IPV4;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getOctet(0));
        sb.append(".");
        sb.append(getOctet(1));
        sb.append(".");
        sb.append(getOctet(2));
        sb.append(".");
        sb.append(getOctet(3));
        return sb.toString();
    }

    /**
     * Extracts the octet value from the IP address
     * @param index of the octet where 0 is the MSB and 3 is the LSB
     * @return octet value
     */
    private int getOctet(int index)
    {
        switch(index)
        {
            case 0:
                return ((mIPAddress & 0xFF000000) >> 24) & 0xFF;
            case 1:
                return ((mIPAddress & 0xFF0000) >> 16);
            case 2:
                return ((mIPAddress & 0xFF00) >> 8);
            case 3:
                return ((mIPAddress & 0xFF));
            default:
                throw new IllegalArgumentException("Invalid Octet index: " + index);
        }
    }

    @Override
    public Form getForm()
    {
        return Form.IPV4_ADDRESS;
    }

    /**
     * Creates a TO role IPV4 address
     */
    public static IPV4Address createTo(int address)
    {
        return new IPV4Address(address, Role.TO);
    }

    /**
     * Creates a FROM role IPV4 address
     */
    public static IPV4Address createFrom(int address)
    {
        return new IPV4Address(address, Role.FROM);
    }
}
