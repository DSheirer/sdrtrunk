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

package io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.location.LocationIdentifier;
import io.github.dsheirer.identifier.location.Point;
import io.github.dsheirer.protocol.Protocol;

/**
 * LRRP Position/Location
 */
public class LRRPPosition extends LocationIdentifier
{
    /**
     * Constructs an instance
     *
     * @param value location
     * @param role to or from
     */
    public LRRPPosition(Point value, Role role)
    {
        super(value, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.LRRP;
    }

    /**
     * Creates a FROM position
     */
    public static LRRPPosition createFrom(double latitude, double longitude)
    {
        return new LRRPPosition(new Point(latitude, longitude), Role.FROM);
    }

    /**
     * Creates a TO position
     */
    public static LRRPPosition createTo(double latitude, double longitude)
    {
        return new LRRPPosition(new Point(latitude, longitude), Role.TO);
    }
}
