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

package io.github.dsheirer.module.decode.p25.message.pdu.packet.sndcp;

import io.github.dsheirer.module.decode.p25.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.message.pdu.packet.PacketMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Direction;

public class SNDCPPacketMessage extends PacketMessage
{
    private SNDCPMessage mSNDCPMessage;

    public SNDCPPacketMessage(PDUSequence PDUSequence, int nac, long timestamp)
    {
        super(PDUSequence, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());

        sb.append(" LLID:").append(getHeader().getLLID());
        if(getPDUSequence().isComplete())
        {
            sb.append(" ").append(getSNDCPMessage().toString());
        }
        else
        {
            sb.append(" **INCOMPLETE DATA BLOCKS RECEIVED***");
        }
        sb.append(" MSG:").append(getPacketMessage().toHexString());
        return sb.toString();
    }

    @Override
    public DataUnitID getDUID()
    {
        return DataUnitID.SUBNETWORK_DEPENDENT_CONVERGENCE_PROTOCOL;
    }

    public SNDCPMessage getSNDCPMessage()
    {
        if(mSNDCPMessage == null)
        {
            mSNDCPMessage = SNDCPMessageFactory.create(getPacketMessage(),
                getPDUSequence().getHeader().getDirection() == Direction.OUTBOUND);
        }

        return mSNDCPMessage;
    }
}
