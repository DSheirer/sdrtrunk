/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.sndcp;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * De-Activate Trunking Data Service (TDS) Context Request
 */
public class DeActivateTdsContextRequest extends SNDCPMessage
{
    private static final int[] NSAPI = {4, 5, 6, 7};
    private static final int[] DEACTIVATION_TYPE = {8, 9, 10, 11, 12, 13, 14, 15};

    /**
     * Constructs an SNDCP message parser instance.
     *
     * @param message containing the binary sequence
     * @param outbound where true is outbound (from repeater) and false is inbound (from mobile)
     */
    public DeActivateTdsContextRequest(BinaryMessage message, boolean outbound)
    {
        super(message, outbound);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(" DEACTIVATE NSAPIs ").append(getDeactivateNSAPIs());
        return sb.toString();
    }

    /**
     * Network Service Access Point Identifier
     */
    public int getNSAPI()
    {
        return getMessage().getInt(NSAPI);
    }

    /**
     * List of NSAPIs to deactivate for the mobile subscriber
     */
    public List<Integer> getDeactivateNSAPIs()
    {
        int type = getMessage().getInt(DEACTIVATION_TYPE);

        if(type == 0)
        {
            List<Integer> allNSAPIs = new ArrayList<>();

            for(int x = 0; x < 16; x++)
            {
                allNSAPIs.add(x);
            }

            return allNSAPIs;
        }
        else if(type == 1)
        {
            List<Integer> thisNSAPI = new ArrayList<>();
            thisNSAPI.add(getNSAPI());
            return thisNSAPI;
        }

        return Collections.EMPTY_LIST;
    }

    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
