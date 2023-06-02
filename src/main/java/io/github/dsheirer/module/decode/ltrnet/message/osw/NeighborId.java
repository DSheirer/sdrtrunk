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
import io.github.dsheirer.module.decode.ltrnet.identifier.NeighborIdentifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Neighbor Identifier.
 */
public class NeighborId extends LtrNetOswMessage
{
    private NeighborIdentifier mNeighborIdentifier;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a message
     */
    public NeighborId(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    public LtrNetMessageType getLtrNetMessageType()
    {
        return LtrNetMessageType.OSW_NEIGHBOR_ID;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if(!isValid())
        {
            sb.append("[CRC FAIL: ").append(CRCLTR.getCRCReason(mMessage, getMessageDirection())).append("] ");
        }
        sb.append("NEIGHBOR:").append(getNeighborID());
        sb.append(" RANK:").append(getNeighborRank());
        sb.append(" MSG:").append(getMessage().toString());
        return sb.toString();
    }

    public NeighborIdentifier getNeighborID()
    {
        if(mNeighborIdentifier == null)
        {
            mNeighborIdentifier = NeighborIdentifier.create(getMessage().getInt(23, 32));
        }

        return mNeighborIdentifier;
    }

    public int getNeighborRank()
    {
        return getMessage().getInt(15, 18) + 1;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getNeighborID());
        }

        return mIdentifiers;
    }
}
