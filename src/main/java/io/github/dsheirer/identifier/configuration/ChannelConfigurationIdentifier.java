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
 * User specified channel name for a decode configuration.
 */
public class ChannelConfigurationIdentifier extends ConfigurationStringIdentifier
{
    public ChannelConfigurationIdentifier()
    {
        this(null);
    }

    public ChannelConfigurationIdentifier(String value)
    {
        super(value, Form.CHANNEL);
    }

    @Override
    public boolean isValid()
    {
        return getValue() != null && !getValue().isEmpty();
    }

    public static ChannelConfigurationIdentifier create(String value)
    {
        return new ChannelConfigurationIdentifier(value);
    }
}
