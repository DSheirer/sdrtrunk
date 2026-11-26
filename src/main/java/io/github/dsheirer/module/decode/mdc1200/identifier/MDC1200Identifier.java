/*
 * *****************************************************************************
 * Copyright (C) 2014-2021 Dennis Sheirer
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

package io.github.dsheirer.module.decode.mdc1200.identifier;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * Identifier for an MDC1200 radio identity
 */
public class MDC1200Identifier extends TalkgroupIdentifier implements Comparable<MDC1200Identifier>
{

    /**
     * Constructs an MDC1200 talkgroup Identifier
     */
    public MDC1200Identifier(Integer value, Role role)
    {
        super(value, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.MDC1200;
    }

    /**
     * Creates an MDC1200 talkgroup identifier for the TO identity
     */
    public static MDC1200Identifier createTo(int value)
    {
        return new MDC1200Identifier(value, Role.TO);
    }

    /**
     * Creates an MDC1200 talkgroup identifier for the FROM identity
     */
    public static MDC1200Identifier createFrom(int value)
    {
        return new MDC1200Identifier(value, Role.FROM);
    }

    @Override
    public int compareTo(MDC1200Identifier other)
    {
        return Integer.compare(getValue(), other != null ? other.getValue() : 0);
    }
}
