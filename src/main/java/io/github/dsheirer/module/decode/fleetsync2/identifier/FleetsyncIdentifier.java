/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.module.decode.fleetsync2.identifier;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * Fleetsync talkgroup identifier
 */
public class FleetsyncIdentifier extends TalkgroupIdentifier implements Comparable<FleetsyncIdentifier>
{
    public FleetsyncIdentifier(Integer talkgroup, Role role)
    {
        super(talkgroup, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.FLEETSYNC;
    }

    /**
     * This identifier formatted as FFF:IIII where F=fleet and I=ident
     */
    public String formatted()
    {
        return String.format("%03d:%04d", getFleet(), getIdent());
    }

    /**
     * Fleet prefix for the identifier
     */
    public int getFleet()
    {
        return (getValue() >> 12) & 0xFF;
    }

    /**
     * Unit identifier
     */
    public int getIdent()
    {
        return getValue() & 0xFFF;
    }

    /**
     * Creates a fleetsync identifier from the integer value that contains both the fleet and the ident with a FROM role
     */
    public static FleetsyncIdentifier createFromUser(int talkgroup)
    {
        return new FleetsyncIdentifier(talkgroup, Role.FROM);
    }

    /**
     * Creates a fleetsync identifier from the integer value that contains both the fleet and the ident with a TO role
     */
    public static FleetsyncIdentifier createToUser(int talkgroup)
    {
        return new FleetsyncIdentifier(talkgroup, Role.TO);
    }

    @Override
    public int compareTo(FleetsyncIdentifier o)
    {
        return getValue().compareTo(o.getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FleetsyncIdentifier)) return false;
        return compareTo((FleetsyncIdentifier) o) == 0;
    }
}
