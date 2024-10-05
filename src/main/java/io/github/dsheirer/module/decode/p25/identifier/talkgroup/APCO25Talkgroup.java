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

package io.github.dsheirer.module.decode.p25.identifier.talkgroup;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.protocol.Protocol;

public class APCO25Talkgroup extends TalkgroupIdentifier
{
    public APCO25Talkgroup(Integer value)
    {
        super(value, Role.TO);
    }

    public APCO25Talkgroup(Integer value, Role role)
    {
        super(value, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    /**
     * Creates an APCO-25 TO talkgroup identifier
     */
    public static APCO25Talkgroup create(int talkgroup)
    {
        return new APCO25Talkgroup(talkgroup);
    }

    /**
     * Creates an APCO-25 talkgroup identifier with ANY role
     */
    public static TalkgroupIdentifier createAny(int talkgroup)
    {
        return new APCO25Talkgroup(talkgroup, Role.ANY);
    }
}
