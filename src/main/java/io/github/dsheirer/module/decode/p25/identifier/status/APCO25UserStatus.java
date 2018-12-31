/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.p25.identifier.status;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.status.UserStatusIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * APCO-25 User status
 */
public class APCO25UserStatus extends UserStatusIdentifier
{
    /**
     * Constructs an APCO-25 unit status
     *
     * @param status value
     */
    public APCO25UserStatus(int status)
    {
        super(status, Role.STATUS, Protocol.APCO25);
    }

    /**
     * Creates a unit status
     */
    public static APCO25UserStatus create(int status)
    {
        return new APCO25UserStatus(status);
    }
}
