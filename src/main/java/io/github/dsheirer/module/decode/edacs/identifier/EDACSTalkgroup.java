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

package io.github.dsheirer.module.decode.edacs.identifier;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * EDACS talkgroup identifier. Talkgroup values are 16-bit (0-65535)
 * with 0 used for the system all-call.
 */
public class EDACSTalkgroup extends TalkgroupIdentifier
{
    public static final int ALLCALL_TALKGROUP = 0;

    public EDACSTalkgroup(int value)
    {
        super(value, Role.TO);
    }

    public EDACSTalkgroup(int value, Role role)
    {
        super(value, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.EDACS;
    }
}
