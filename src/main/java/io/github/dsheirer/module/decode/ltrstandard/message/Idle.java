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

import java.util.Collections;
import java.util.List;

/**
 * Indicates the repeater is IDLE and available for use
 */
public class Idle extends LTRMessage
{
    public Idle(CorrectedBinaryMessage message, MessageDirection direction, CRC crc)
    {
        super(message, direction, crc);
    }

    @Override
    public LtrStandardMessageType getMessageType()
    {
        return LtrStandardMessageType.IDLE;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("IDLE AREA:").append(getArea());
        sb.append(" LCN:").append(getChannel());
        sb.append(" HOME:").append(getHomeRepeater());
        sb.append(" GRP:").append(getGroup());
        sb.append(" FREE:").append(getFree());
        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}