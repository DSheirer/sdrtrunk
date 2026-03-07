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

package io.github.dsheirer.module.decode.nxdn.layer3.data;

import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallBlock;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallHeader;
import io.github.dsheirer.module.decode.nxdn.layer3.call.ShortDataCallBlock;
import io.github.dsheirer.module.decode.nxdn.layer3.call.ShortDataCallRequestHeader;
import io.github.dsheirer.module.decode.nxdn.layer3.call.ShortDataInitializationVector;
import java.util.Date;

/**
 * Reassembles short and packet data
 */
public class PacketDataAssembler
{
    private PacketSequenceAssembly mPacketSequenceAssembly;

    /**
     * Process a short data header on control or traffic channel
     * @param header to process
     */
    public void process(ShortDataCallRequestHeader header)
    {
//        System.out.println("Short Data Call Header received: " + header);
        mPacketSequenceAssembly = new PacketSequenceAssembly(header);
    }

    /**
     * Process a short data block on control or traffic channel
     * @param data to process
     */
    public PacketSequence process(ShortDataCallBlock data)
    {
//        System.out.println("Short Data Call Block received: " + data);

        if(mPacketSequenceAssembly != null && mPacketSequenceAssembly.isShortData())
        {
            mPacketSequenceAssembly.add(data);

            if(mPacketSequenceAssembly.isComplete())
            {
                PacketSequence sequence = mPacketSequenceAssembly.getPacketSequence();
                System.out.println(new Date() + " " + sequence);
                mPacketSequenceAssembly = null;
                return sequence;
            }
        }

        return null;
    }

    /**
     * Process a short data call encryption initialization vector.
     * @param iv to process
     */
    public void process(ShortDataInitializationVector iv)
    {
        System.out.println("Short Data Initialization Vector received: " + iv);
        if(mPacketSequenceAssembly != null && mPacketSequenceAssembly.isShortData())
        {
            mPacketSequenceAssembly.set(iv);
        }
    }

    /**
     * Process a data call header (on the traffic channel)
     * @param header to process
     */
    public void process(DataCallHeader header)
    {
//        System.out.println("Data Call Header received: " + header);
        mPacketSequenceAssembly = new PacketSequenceAssembly(header);
    }

    /**
     * Process a data call block (on the traffic channel)
     * @param data to process
     */
    public PacketSequence process(DataCallBlock data)
    {
//        System.out.println("Data Call Block received: " + data);

        if(mPacketSequenceAssembly != null && mPacketSequenceAssembly.isDataCall())
        {
            mPacketSequenceAssembly.add(data);

            if(mPacketSequenceAssembly.isComplete())
            {
                PacketSequence sequence = mPacketSequenceAssembly.getPacketSequence();
                System.out.println(new Date() + " " + sequence);
                mPacketSequenceAssembly = null;
                return sequence;
            }
        }

        return null;
    }
}
