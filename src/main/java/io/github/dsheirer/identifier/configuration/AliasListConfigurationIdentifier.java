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

/**
 * User specified alias list name for a decode configuration.
 */
public class AliasListConfigurationIdentifier extends ConfigurationStringIdentifier
{
    public AliasListConfigurationIdentifier(String value)
    {
        super(value, Form.ALIAS_LIST);
    }

    @Override
    public boolean isValid()
    {
        return getValue() != null && !getValue().isEmpty();
    }

    public static AliasListConfigurationIdentifier create(String value)
    {
        return new AliasListConfigurationIdentifier(value);
    }
}
