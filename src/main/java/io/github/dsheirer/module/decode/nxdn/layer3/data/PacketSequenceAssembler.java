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
import io.github.dsheirer.module.decode.nxdn.layer3.call.IPacketHeader;
import io.github.dsheirer.module.decode.nxdn.layer3.call.ShortDataInitializationVector;
import io.github.dsheirer.module.decode.nxdn.layer3.call.UserData;
import java.util.Collections;
import java.util.List;

/**
 * Reassembles short data call and data call packet sequences
 */
public class PacketSequenceAssembler
{
    private PacketSequenceAssembly mPacketSequenceAssembly;

    /**
     * Process either a short data header on control or traffic channel or a data call header on traffic channel.
     * @param header to process
     */
    public void process(IPacketHeader header)
    {
        mPacketSequenceAssembly = new PacketSequenceAssembly(header);
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
     * Adds the user data block to the sequence
     * @param userData to add
     * @return packet sequence if it's now completed after adding the user data block.
     */
    public List<NXDNMessage> process(UserData userData)
    {
        if(mPacketSequenceAssembly != null)
        {
            mPacketSequenceAssembly.add(userData);

            if(mPacketSequenceAssembly.isComplete())
            {
                PacketSequence packetSequence = mPacketSequenceAssembly.getPacketSequence();
                NXDNPacketMessage message = NXDNMessageFactory.get(packetSequence);
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
        }
        else
        {
            int a = 0;
        }

        return Collections.emptyList();
    }
}
