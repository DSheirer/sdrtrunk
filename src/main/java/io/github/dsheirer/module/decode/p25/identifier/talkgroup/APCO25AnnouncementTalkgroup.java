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
package io.github.dsheirer.module.decode.p25.identifier.talkgroup;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.Role;

/**
 * APCO25 Announcement Talkgroup - grouping of talkgroups within a system.
 */
public class APCO25AnnouncementTalkgroup extends APCO25Talkgroup
{
    /**
     * Constructs an APCO25 Talkgroup Identifier with a FROM role.
     *
     * @param value of the talkgroup
     */
    public APCO25AnnouncementTalkgroup(Integer value)
    {
        super(value, Role.BROADCAST, true);
    }

    /**
     * Creates an APCO-25 announcement group talkgroup identifier
     */
    public static Identifier create(int talkgroup)
    {
        return new APCO25AnnouncementTalkgroup(talkgroup);
    }
}
