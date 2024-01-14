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

package io.github.dsheirer.identifier.alias;

import io.github.dsheirer.protocol.Protocol;

/**
 * DMR Talker alias (alternate/additional) value provided by the network for the current talker (ie FROM).
 */
public class DmrTalkerAlias2Identifier extends TalkerAlias2Identifier
{
    /**
     * Constructs an instance.
     * @param value of the talker alias
     */
    public DmrTalkerAlias2Identifier(String value)
    {
        super(value);
    }

    @Override
    public boolean isValid()
    {
        return getValue() != null && !getValue().isEmpty();
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.DMR;
    }

    public static DmrTalkerAlias2Identifier create(String value)
    {
        return new DmrTalkerAlias2Identifier(value);
    }
}
