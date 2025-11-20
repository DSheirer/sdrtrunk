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

package io.github.dsheirer.module.decode.nxdn.layer2;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.NXDNMessage;
import io.github.dsheirer.module.decode.nxdn.layer3.type.Structure;
import java.util.List;

/**
 * SACCH message fragment
 */
public class SACCHFragment extends NXDNMessage
{
    private static final IntField STRUCTURE = IntField.length2(0);
    private static final IntField RADIO_ACCESS_NUMBER = IntField.length6(2);

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     */
    public SACCHFragment(CorrectedBinaryMessage message, long timestamp, LICH lich)
    {
        super(message, timestamp, message.getInt(RADIO_ACCESS_NUMBER), lich);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
        sb.append(getStructure());
        sb.append(" MSG:").append(getMessage().toHexString());
        sb.append(" LICH:").append(getLICH());
        return sb.toString();
    }

    /**
     * SACCH message fragment
     */
    public CorrectedBinaryMessage getFragment()
    {
        return getMessage().getSubMessage(8, 26);
    }

    /**
     * Indicates if the SACCH is standalone (false) or a superframe fragment (true).
     */
    public boolean isSuperFrame()
    {
        return getLICH().isSACCHSuperFrame();
    }

    /**
     * Structure of the SACCH message
     * @return
     */
    public Structure getStructure()
    {
        return Structure.fromTrafficValue(getMessage().getInt(STRUCTURE));
    }

    /**
     * Radio Access Network (RAN) code
     */
    public int getRAN()
    {
        return getMessage().getInt(RADIO_ACCESS_NUMBER);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of();
    }
}
