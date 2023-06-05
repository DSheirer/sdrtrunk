/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.module.decode.ltrnet.message.osw;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRCLTR;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.ltrnet.LtrNetMessageType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Channel Map - High Channels (11 - 20).
 */
public class ChannelMapHigh extends LtrNetOswMessage
{
    /**
     * Constructs a message
     */
    public ChannelMapHigh(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    public LtrNetMessageType getLtrNetMessageType()
    {
        return LtrNetMessageType.OSW_CHANNEL_MAP_HIGH;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if(!isValid())
        {
            sb.append("[CRC FAIL: ").append(CRCLTR.getCRCReason(mMessage, getMessageDirection())).append("] ");
        }
        sb.append("CHANNEL MAP HIGH ").append(getChannels());
        sb.append(" MSG:").append(getMessage().toString());
        return sb.toString();
    }

    public List<Integer> getChannels()
    {
        List<Integer> retVal = new ArrayList<>();

        for(int x = 27; x >= 18; x--)
        {
            if(getMessage().get(x))
            {
                retVal.add(38 - x);
            }
        }

        return retVal;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
