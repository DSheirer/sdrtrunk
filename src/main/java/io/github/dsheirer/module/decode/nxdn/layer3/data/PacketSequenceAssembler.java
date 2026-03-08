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

import io.github.dsheirer.module.decode.nxdn.NXDNMessage;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageFactory;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallBlock;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallHeader;
import io.github.dsheirer.module.decode.nxdn.layer3.call.ShortDataCallBlock;
import io.github.dsheirer.module.decode.nxdn.layer3.call.ShortDataCallRequestHeader;
import io.github.dsheirer.module.decode.nxdn.layer3.call.ShortDataInitializationVector;
import io.github.dsheirer.module.decode.nxdn.layer3.call.UserData;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Reassembles short call and data call packet sequences
 */
public class PacketSequenceAssembler
{
    private PacketSequenceAssembly mPacketSequenceAssembly;

    /**
     * Process a short data header on control or traffic channel
     * @param header to process
     */
    public void process(ShortDataCallRequestHeader header)
    {
        mPacketSequenceAssembly = new PacketSequenceAssembly(header);
    }

    /**
     * Process a short data block on control or traffic channel
     * @param data to process
     */
    public List<NXDNMessage> process(ShortDataCallBlock data)
    {
        if(mPacketSequenceAssembly != null && mPacketSequenceAssembly.isShortData())
        {
            return processUserData(data);
        }

        return Collections.emptyList();
    }

    /**
     * Process a short data call encryption initialization vector.
     * @param iv to process
     */
    public void process(ShortDataInitializationVector iv)
    {
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
        mPacketSequenceAssembly = new PacketSequenceAssembly(header);
    }

    /**
     * Process a data call block (on the traffic channel)
     * @param data to process
     */
    public List<NXDNMessage> process(DataCallBlock data)
    {
        if(mPacketSequenceAssembly != null && mPacketSequenceAssembly.isDataCall())
        {
            return processUserData(data);
        }

        return Collections.emptyList();
    }

    /**
     * Adds the user data block to the sequence
     * @param userData to add
     * @return packet sequene if it's now complete.
     */
    private List<NXDNMessage> processUserData(UserData userData)
    {
        mPacketSequenceAssembly.add(userData);

        if(mPacketSequenceAssembly.isComplete())
        {
            PacketSequence packetSequence = mPacketSequenceAssembly.getPacketSequence();
            NXDNPacketMessage message = NXDNMessageFactory.get(packetSequence);

            if(message != null)
            {
                System.out.println(new Date() + " " + message);
            }
            else
            {
                System.out.println(new Date() + " " + packetSequence);
            }

            mPacketSequenceAssembly = null;

            if(message != null)
            {
                return List.of(packetSequence, message);
            }
            else
            {
                return List.of(packetSequence);
            }
        }

        return Collections.emptyList();
    }
}
