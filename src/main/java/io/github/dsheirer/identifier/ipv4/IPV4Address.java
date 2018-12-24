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

import java.util.Objects;

/**
 * IPV4 Address
 */
public class IPV4Address
{
    private int mIPAddress;

    public IPV4Address(int ipAddress)
    {
        mIPAddress = ipAddress;
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
    public boolean equals(Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(o == null || getClass() != o.getClass())
        {
            return false;
        }
        IPV4Address that = (IPV4Address)o;
        return mIPAddress == that.mIPAddress;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mIPAddress);
    }
}
