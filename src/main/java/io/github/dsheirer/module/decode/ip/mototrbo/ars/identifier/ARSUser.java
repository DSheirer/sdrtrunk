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
 * Automatic Registration Service - User Identifier
 */
public class ARSUser extends StringIdentifier
{
    /**
     * Constructs an instance
     * @param value of the user
     * @param role of the user (TO/FROM)
     */
    public ARSUser(String value, Role role)
    {
        super(value, IdentifierClass.USER, Form.ARS_USER, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.ARS;
    }

    /**
     * Creates an ARS user with a FROM role
     */
    public static ARSUser createFrom(String value)
    {
        return new ARSUser(value, Role.FROM);
    }

    /**
     * Creates an ARS user with a TO role
     */
    public static ARSUser createTo(String value)
    {
        return new ARSUser(value, Role.TO);
    }
}
