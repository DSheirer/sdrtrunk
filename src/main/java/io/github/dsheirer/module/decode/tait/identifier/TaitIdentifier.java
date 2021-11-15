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

package io.github.dsheirer.module.decode.tait.identifier;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.string.StringIdentifier;
import io.github.dsheirer.protocol.Protocol;
import org.apache.commons.lang3.StringUtils;

/**
 * Tait-1200 String (ASCII) Identifier
 */
public class TaitIdentifier extends StringIdentifier implements Comparable<TaitIdentifier>
{
    /**
     * Constructs an identifier with the specified role
     */
    public TaitIdentifier(String value, Role role)
    {
        super(value, IdentifierClass.USER, Form.UNIT_IDENTIFIER, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.TAIT1200;
    }

    /***
     * Creates an identifier with the FROM role
     */
    public static TaitIdentifier createFrom(String value)
    {
        return new TaitIdentifier(value, Role.FROM);
    }

    /***
     * Creates an identifier with the TO role
     */
    public static TaitIdentifier createTo(String value)
    {
        return new TaitIdentifier(value, Role.TO);
    }

    @Override
    public int compareTo(TaitIdentifier other)
    {
        return StringUtils.compare(getValue(), other != null ? other.getValue() : null);
    }
}
