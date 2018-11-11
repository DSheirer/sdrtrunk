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

package io.github.dsheirer.identifier.configuration;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.longnumber.LongIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * Decoder channel configuration identifier.
 */
public class ConfigurationLongIdentifier extends LongIdentifier
{
    public ConfigurationLongIdentifier(Long value, Form form)
    {
        super(value, IdentifierClass.CONFIGURATION, form, Role.ANY);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.UNKNOWN;
    }
}
