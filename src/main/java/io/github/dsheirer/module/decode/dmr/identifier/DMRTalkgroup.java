/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.module.decode.dmr.identifier;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.protocol.Protocol;

public class DMRTalkgroup extends TalkgroupIdentifier
{
    public DMRTalkgroup(Integer value)
    {
        super(value, Role.TO);
    }

    public DMRTalkgroup(Integer value, Role role)
    {
        super(value, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.DMR;
    }

    /**
     * Creates a DMR TO talkgroup identifier
     */
    public static TalkgroupIdentifier create(int talkgroup)
    {
        return new DMRTalkgroup(talkgroup);
    }

    /**
     * Creates a DMR talkgroup identifier with ANY role
     */
    public static TalkgroupIdentifier createAny(int talkgroup)
    {
        return new DMRTalkgroup(talkgroup, Role.ANY);
    }
}
