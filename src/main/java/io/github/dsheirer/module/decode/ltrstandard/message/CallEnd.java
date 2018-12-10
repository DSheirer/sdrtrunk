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
package io.github.dsheirer.module.decode.ltrstandard.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRC;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.MessageDirection;
import io.github.dsheirer.module.decode.ltrstandard.LtrStandardMessageType;

import java.util.ArrayList;
import java.util.List;

public class CallEnd extends LTRStandardMessage
{
    private List<Identifier> mIdentifiers;

    public CallEnd(CorrectedBinaryMessage message, MessageDirection direction, CRC crc)
    {
        super(message, direction, crc);
    }

    @Override
    public LtrStandardMessageType getMessageType()
    {
        return LtrStandardMessageType.CALL_END;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CALL END ").append(getTalkgroup().formatted());
        sb.append(" LCN:").append(getChannel());
        sb.append(" FREE:").append(getFree());
        return sb.toString();
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