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

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.protocol.Protocol;

/**
 * Configuration identifier that identifies a channel descriptor.   This is primarily used to broadcast the channel
 * descriptor for a traffic channel so that it is aware of what channel number and frequency it is decoding.
 */
public class ChannelDescriptorConfigurationIdentifier extends Identifier<IChannelDescriptor>
{
    /**
     * Constructs a channel descriptor configuration identifier
     */
    public ChannelDescriptorConfigurationIdentifier(IChannelDescriptor value)
    {
        super(value, IdentifierClass.CONFIGURATION, Form.CHANNEL_DESCRIPTOR, Role.ANY);
    }

    /**
     * Protocol for the channel descriptor and the enclosing identifier instance.
     */
    @Override
    public Protocol getProtocol()
    {
        if(getValue() != null)
        {
            return getValue().getProtocol();
        }

        return Protocol.UNKNOWN;
    }
}
