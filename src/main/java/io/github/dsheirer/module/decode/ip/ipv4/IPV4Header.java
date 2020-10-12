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

package io.github.dsheirer.module.decode.ip.ipv4;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.ipv4.IPV4Identifier;
import io.github.dsheirer.module.decode.ip.Header;
import io.github.dsheirer.module.decode.ip.IPProtocol;
import io.github.dsheirer.module.decode.p25.identifier.ipv4.APCO25IpAddress;

public class IPV4Header extends Header
{
    private static final int[] VERSION = {0, 1, 2, 3};
    private static final int[] HEADER_LENGTH = {4, 5, 6, 7};
    private static final int[] TOTAL_LENGTH = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] PROTOCOL = {72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] FROM_ADDRESS = {96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110,
        111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127};
    private static final int[] TO_ADDRESS = {128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142,
        143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159};

    private IPV4Identifier mFromAddress;
    private IPV4Identifier mToAddress;

    public IPV4Header(BinaryMessage message, int offset)
    {
        super(message, offset);
        checkValid();
    }

    /**
     * Performs simple header and packet length validations relative to the packet offset within the binary message.
     */
    private void checkValid()
    {
        int headerLength = getLength();

        if(headerLength < 40)
        {
            setValid(false);
            return;
        }

        if(getMessage().size() < headerLength + getOffset())
        {
            setValid(false);
            return;
        }

        int totalLength = getTotalLength();

        if(totalLength < headerLength)
        {
            setValid(false);
            return;
        }

//        if(getMessage().size() < totalLength + getOffset())
//        {
//            setValid(false);
//            return;
//        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("IP FROM:").append(getFromAddress());
        sb.append(" TO:").append(getToAddress());
        return sb.toString();
    }

    /**
     * Determines the IP version of the packet that starts at the indicated offset within the binary message.
     *
     * @param message message containing an IP packet
     * @param offset to the start of the packet within the binary message
     * @return
     */
    public static int getIPVersion(BinaryMessage message, int offset)
    {
        return message.getInt(VERSION, offset);
    }

    /**
     * IP Version of this packet
     */
    public int getIPVersion()
    {
        return getIPVersion(getMessage(), getOffset());
    }

    /**
     * Length of this IPV4 header in bits (NOT BYTES)
     */
    public int getLength()
    {
        return getMessage().getInt(HEADER_LENGTH, getOffset()) * 32;
    }

    /**
     * Total Length of this IPV4 packet in bits (NOT BYTES)
     */
    public int getTotalLength()
    {
        return getMessage().getInt(TOTAL_LENGTH, getOffset()) * 8;
    }

    /**
     * Protocol for the payload being carried by this IP packet
     */
    public IPProtocol getProtocol()
    {
        return IPProtocol.fromValue(getMessage().getInt(PROTOCOL, getOffset()));
    }

    /**
     * From IP Address
     */
    public IPV4Identifier getFromAddress()
    {
        if(mFromAddress == null)
        {
            mFromAddress = APCO25IpAddress.createFrom(getMessage().getInt(FROM_ADDRESS, getOffset()));
        }

        return mFromAddress;
    }

    /**
     * To IP Address
     */
    public IPV4Identifier getToAddress()
    {
        if(mToAddress == null)
        {
            mToAddress = APCO25IpAddress.createTo(getMessage().getInt(TO_ADDRESS, getOffset()));
        }

        return mToAddress;
    }
}
