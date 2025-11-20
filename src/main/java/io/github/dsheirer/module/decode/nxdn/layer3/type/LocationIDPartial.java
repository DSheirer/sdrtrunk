/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3.type;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import org.jspecify.annotations.NonNull;

/**
 * 19-bit field with a location category and system ID
 * @param message containing the location ID field
 * @param offset to the start of the field.
 */
public record LocationIDPartial(CorrectedBinaryMessage message, int offset)
{
    private static final IntField CATEGORY = IntField.length2(0);
    private static final IntField SYSTEM_GLOBAL = IntField.range(2, 11);
    private static final IntField SYSTEM_REGIONAL = IntField.range(2, 15);
    private static final IntField SYSTEM_LOCAL = IntField.range(2, 18);

    @NonNull
    @Override
    public String toString()
    {
        return getLocationCategory().name() + " SYSTEM:" + getSystemID();
    }

    public LocationCategory getLocationCategory()
    {
        return LocationCategory.fromValue(message.getInt(CATEGORY, offset));
    }

    /**
     * System ID value.
     */
    public int getSystemID()
    {
        return switch(getLocationCategory())
        {
            case GLOBAL -> message.getInt(SYSTEM_GLOBAL, offset);
            case REGIONAL -> message.getInt(SYSTEM_REGIONAL, offset);
            case LOCAL -> message.getInt(SYSTEM_LOCAL, offset);
            default -> 0;
        };
    }
}
