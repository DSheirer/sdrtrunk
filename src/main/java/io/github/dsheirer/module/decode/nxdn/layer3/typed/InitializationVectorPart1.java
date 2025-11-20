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

package io.github.dsheirer.module.decode.nxdn.layer3.typed;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.FragmentedIntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import java.util.List;

/**
 * Initialization vector part 1
 */
public class InitializationVectorPart1 extends Information1
{
    private static final FragmentedIntField IV = FragmentedIntField.of(8, 9, 10, 11, 12, 18, 19, 20, 21, 22, 23);

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran from the frame
     * @param lich from the frame
     */
    public InitializationVectorPart1(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
        sb.append(getCallOption());
        sb.append(" ENCRYPTION IV PART 1:").append(getIV());
        sb.append(" FREE REPEATER 1:").append(getRepeater());
        sb.append(" 2:").append(getRepeater2());
        return sb.toString();

    }

    /**
     * Initialization vector part 2 fragment
     */
    public int getIV()
    {
        return getMessage().getInt(IV);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of();
    }
}
