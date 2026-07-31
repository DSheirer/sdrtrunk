/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.identifier;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.string.StringIdentifier;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationCategory;
import io.github.dsheirer.protocol.Protocol;

/**
 * Indicates the network location category
 */
public class NXDNCategory extends StringIdentifier
{
    public NXDNCategory(String value, IdentifierClass identifierClass, Form form, Role role)
    {
        super(value, identifierClass, form, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.NXDN;
    }

    /**
     * Creates an instance
     * @param category for the system/site values
     * @return category identifier
     */
    public static NXDNCategory create(LocationCategory category)
    {
        return new NXDNCategory(category.toString(), IdentifierClass.NETWORK, Form.CATEGORY, Role.BROADCAST);
    }
}
