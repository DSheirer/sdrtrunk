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

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * APCO-25 Unit or User status
 */
public class APCO25Status extends IntegerIdentifier
{
    private Role mRole;

    /**
     * Constructs an APCO-25 status
     * @param status value
     * @param role of the user/unit that the status applies to
     */
    public APCO25Status(int status, IdentifierClass identifierClass, Form form, Role role)
    {
        super(status, identifierClass, form, role);
        mRole = role;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    /**
     * Creates a unit status
     * @param status
     * @return
     */
    public static APCO25Status createUnitStatus(int status)
    {
        return new APCO25Status(status, IdentifierClass.USER, Form.UNIT_STATUS, Role.FROM);
    }

    /**
     * Creates a User status
     */
    public static APCO25Status createUserStatus(int status)
    {
        return new APCO25Status(status, IdentifierClass.USER, Form.USER_STATUS, Role.FROM);
    }
}
