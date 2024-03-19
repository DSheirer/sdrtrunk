/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase2.message.mac;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;

/**
 * Channel grant details
 */
public interface IP25ChannelGrantDetailProvider
{
    /**
     * Channel where the call will take place
     */
    APCO25Channel getChannel();

    /**
     * Optional from radio unit.  This can be null.
     */
    Identifier getSourceAddress();

    /**
     * Target radio unit or talkgroup
     */
    Identifier getTargetAddress();

    /**
     * Service options for the call.
     */
    ServiceOptions getServiceOptions();
}
