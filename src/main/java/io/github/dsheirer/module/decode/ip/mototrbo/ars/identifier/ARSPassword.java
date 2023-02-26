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

package io.github.dsheirer.module.decode.ip.mototrbo.ars.identifier;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.string.StringIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * Automatic Registration Service - User Password
 */
public class ARSPassword extends StringIdentifier
{
    /**
     * Constructs an instance
     * @param value of the password
     * @param role of the password (TO/FROM)
     */
    public ARSPassword(String value, Role role)
    {
        super(value, IdentifierClass.USER, Form.ARS_USER, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.ARS;
    }

    /**
     * Creates an ARS password with a FROM role
     */
    public static ARSPassword createFrom(String value)
    {
        return new ARSPassword(value, Role.FROM);
    }

    /**
     * Creates an ARS password with a TO role
     */
    public static ARSPassword createTo(String value)
    {
        return new ARSPassword(value, Role.TO);
    }
}
