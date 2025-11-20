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

package io.github.dsheirer.module.decode.nxdn.layer3.proprietary;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import java.util.List;

/**
 * Talker alias
 *
 * Sample:
 * L3: RDCH RAN:1 PROPRIETARY FORM - VENDOR:104 MSG:3F688204144449535000CF10
 * L3: RDCH RAN:1 PROPRIETARY FORM - VENDOR:104 MSG:3F688204244154434800C7D0
 * L3: RDCH RAN:1 PROPRIETARY FORM - VENDOR:104 MSG:3F6882043400000000008330
 * L3: RDCH RAN:1 PROPRIETARY FORM - VENDOR:104 MSG:3F6882044400000250007CF0
 */
public class TalkerAlias extends ProprietaryForm
{
    //OCTET 3: unknown (0x04).  May be encoding type (ASCII)
    private static final IntField FRAGMENT_NUMBER = IntField.length4(OCTET_4);
    private static final IntField FRAGMENT_COUNT = IntField.length4(OCTET_4 + 4);
    private static final int FRAGMENT_START = OCTET_5;
    private static final int FRAGMENT_END = OCTET_9;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type for the message
     * @param ran of the system
     * @param lich value
     */
    public TalkerAlias(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
        sb.append("TALKER ALIAS ").append(getFragmentNumber()).append("/").append(getFragmentCount());
        sb.append(" FRAGMENT [").append(getCharacters()).append("]");
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Fragment number
     */
    public int getFragmentNumber()
    {
        return getMessage().getInt(FRAGMENT_NUMBER);
    }

    /**
     * Count of fragments
     */
    public int getFragmentCount()
    {
        return getMessage().getInt(FRAGMENT_COUNT);
    }

    /**
     * Alias fragment
     */
    public BinaryMessage getFragment()
    {
        return getMessage().getSubMessage(FRAGMENT_START, FRAGMENT_END);
    }

    /**
     * ASCII characters
     */
    public String getCharacters()
    {
        return new String(getMessage().getSubMessage(FRAGMENT_START, FRAGMENT_END).getBytes());
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of();
    }
}
