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
package io.github.dsheirer.module.decode.ltrnet.message.isw;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.ltrnet.LtrNetMessageType;

import java.util.ArrayList;
import java.util.List;

/**
 * Call Start Message.
 */
public class IswCallStart extends LtrNetIswMessage
{
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a message
     */
    public IswCallStart(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    public LtrNetMessageType getLtrNetMessageType()
    {
        return LtrNetMessageType.ISW_CALL_START;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CALL START TALKGROUP:").append(getTalkgroup().formatted());
        sb.append(" AREA:").append(getArea(getMessage()));
        sb.append(" LCN:").append(getChannel(getMessage()));
        sb.append(" FREE:").append(getFree(getMessage()));
        return sb.toString();
    }

    /**
     * LCN for the call
     */
    public int getChannel()
    {
        return getChannel(getMessage());
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTalkgroup());
        }

        return mIdentifiers;
    }
}
