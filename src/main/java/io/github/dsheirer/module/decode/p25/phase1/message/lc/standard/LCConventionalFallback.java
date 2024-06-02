/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.standard;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import java.util.Collections;
import java.util.List;

/**
 * Conventional fallback sent on the outbound channel to indicate that the repeater sending it is providing conventional
 * fallback service and to indicate which users should use it.
 */
public class LCConventionalFallback extends LinkControlWord
{
    private static final IntField FALLBACK_CHANNEL_ID_1 = IntField.length8(OCTET_3_BIT_24);
    private static final IntField FALLBACK_CHANNEL_ID_2 = IntField.length8(OCTET_4_BIT_32);
    private static final IntField FALLBACK_CHANNEL_ID_3 = IntField.length8(OCTET_5_BIT_40);
    private static final IntField FALLBACK_CHANNEL_ID_4 = IntField.length8(OCTET_6_BIT_48);
    private static final IntField FALLBACK_CHANNEL_ID_5 = IntField.length8(OCTET_7_BIT_56);
    private static final IntField FALLBACK_CHANNEL_ID_6 = IntField.length8(OCTET_8_BIT_64);

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCConventionalFallback(CorrectedBinaryMessage message)
    {
        super(message);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if(!isValid())
        {
            sb.append(" [CRC ERROR]");
        }
        sb.append(" CONVENTIONAL FALLBACK CHANNELS 1:").append(getFallbackChannel1());
        sb.append(" 2:").append(getFallbackChannel2());
        sb.append(" 3:").append(getFallbackChannel3());
        sb.append(" 4:").append(getFallbackChannel4());
        sb.append(" 5:").append(getFallbackChannel5());
        sb.append(" 6:").append(getFallbackChannel6());

        return sb.toString();
    }

    public int getFallbackChannel1()
    {
        return getInt(FALLBACK_CHANNEL_ID_1);
    }

    public int getFallbackChannel2()
    {
        return getInt(FALLBACK_CHANNEL_ID_2);
    }

    public int getFallbackChannel3()
    {
        return getInt(FALLBACK_CHANNEL_ID_3);
    }

    public int getFallbackChannel4()
    {
        return getInt(FALLBACK_CHANNEL_ID_4);
    }

    public int getFallbackChannel5()
    {
        return getInt(FALLBACK_CHANNEL_ID_5);
    }

    public int getFallbackChannel6()
    {
        return getInt(FALLBACK_CHANNEL_ID_6);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
