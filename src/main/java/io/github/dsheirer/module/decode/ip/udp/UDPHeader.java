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

package io.github.dsheirer.module.decode.ip.udp;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.ip.Header;

public class UDPHeader extends Header
{
    private static final int[] SOURCE_PORT = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] DESTINATION_PORT = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};

    private UDPPort mSourcePort;
    private UDPPort mDestinationPort;

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param message containing the header
     * @param offset to the header within the message
     */
    public UDPHeader(BinaryMessage message, int offset)
    {
        super(message, offset);
        checkValid();
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("UDP PORT FROM:").append(getSourcePort());
        sb.append(" TO:").append(getDestinationPort());
        return sb.toString();
    }

    private void checkValid()
    {
        setValid(getMessage().size() >= getOffset() + getLength());
    }

    @Override
    public int getLength()
    {
        return 64;
    }

    /**
     * Source UDP port number
     * @return
     */
    public UDPPort getSourcePort()
    {
        if(mSourcePort == null)
        {
            mSourcePort = UDPPort.createFrom(getMessage().getInt(SOURCE_PORT, getOffset()));
        }

        return mSourcePort;
    }

    /**
     * Destination UDP port number
     */
    public UDPPort getDestinationPort()
    {
        if(mDestinationPort == null)
        {
            mDestinationPort = UDPPort.createTo(getMessage().getInt(DESTINATION_PORT, getOffset()));
        }

        return mDestinationPort;
    }
}
