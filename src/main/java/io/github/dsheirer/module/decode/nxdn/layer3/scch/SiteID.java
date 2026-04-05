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

package io.github.dsheirer.module.decode.nxdn.layer3.scch;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.SiteType;
import java.util.List;

/**
 * Site ID
 */
public class SiteID extends Information4
{
    private static final IntField SITE_TYPE = IntField.length2(3);
    private static final IntField SITE_CODE = IntField.length5(8);

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran from the frame
     * @param lich from the frame
     */
    public SiteID(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
        sb.append("SITE:").append(getSite());
        sb.append(" TYPE:").append(getSiteType());
        return sb.toString();
    }

    public int getSite()
    {
        return getMessage().getInt(SITE_CODE);
    }

    /**
     * Site type: WIDE, MIDDLE, or NARROW
     */
    public SiteType getSiteType()
    {
        return SiteType.fromValue(getMessage().getInt(SITE_TYPE));
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of();
    }
}
