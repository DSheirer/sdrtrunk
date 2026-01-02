/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.channel;

import io.github.dsheirer.module.decode.nxdn.layer3.type.TransmissionMode;
import io.github.dsheirer.protocol.Protocol;

/**
 * Base NXDN channel
 */
public abstract class NXDNChannel implements INXDNChannelDescriptor
{
    private final TransmissionMode mTransmissionMode;

    /**
     * Constructs an instance
     * @param transmissionMode for the channel
     */
    public NXDNChannel(TransmissionMode transmissionMode)
    {
        mTransmissionMode = transmissionMode;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.NXDN;
    }

    @Override
    public int getTimeslotCount()
    {
        return 1;
    }

    @Override
    public boolean isTDMAChannel()
    {
        return false;
    }

    /**
     * Indicates the transmission mode: 4800 or 9600 BPS (2400/4800 baud)
     */
    public TransmissionMode getTransmissionMode()
    {
        return mTransmissionMode;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("DN:").append(getDownlinkFrequency() / 1E6D);

        long up = getUplinkFrequency();

        if(up > 0)
        {
            sb.append(" UP:").append(up / 1E6D);
        }

        sb.append(" MHZ");
        return sb.toString();
    }
}
