/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25.message.tsbk.osp.control;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

public class IdentifierUpdateVUHFFrequency extends IdentifierUpdateFrequency
{
    public static final int[] BANDWIDTH = {84, 85, 86, 87};

    public static final int TRANSMIT_OFFSET_VHF_UHF_SIGN = 88;

    public static final int[] TRANSMIT_OFFSET = {89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101};

    public IdentifierUpdateVUHFFrequency(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());
        sb.append(toString());

        return sb.toString();
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(" ID:" + getIdentifier());
        sb.append(" BASE:" + getBaseFrequency());
        sb.append(" BANDWIDTH:" + getBandwidth());
        sb.append(" SPACING:" + getChannelSpacing());
        sb.append(" OFFSET:" + getTransmitOffset());

        return sb.toString();
    }

    /**
     * Channel bandwidth in hertz
     */
    public int getBandwidth()
    {
        int bandwidth = mMessage.getInt(BANDWIDTH);

        switch(bandwidth)
        {
            case 4:
                return 6250;
            case 5:
                return 12500;
            default:
                return 0;
        }
    }

    /**
     * Transmit offset in hertz
     */
    @Override
    public long getTransmitOffset()
    {
        long offset = mMessage.getLong(TRANSMIT_OFFSET) *
            getChannelSpacing();

        if(mMessage.get(TRANSMIT_OFFSET_VHF_UHF_SIGN))
        {
            return offset;
        }
        else
        {
            return -offset;
        }
    }
}
