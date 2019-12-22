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

package io.github.dsheirer.module.decode.passport.identifier;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * Passport Talkgroup
 */
public class PassportTalkgroup extends TalkgroupIdentifier implements Comparable<PassportTalkgroup>
{
    /**
     * Constructs a Passport talkgroup with a TO role.
     */
    public PassportTalkgroup(Integer value, Role role)
    {
        super(value, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.PASSPORT;
    }

    /**
     * Creates a Passport talkgroup with the value as the TO role
     */
    public static PassportTalkgroup createTo(int value)
    {
        return new PassportTalkgroup(value, Role.TO);
    }

    /**
     * Creates a Passport talkgroup (ie mobile id) with the value as the TO role
     */
    public static PassportTalkgroup createFrom(int value)
    {
        return new PassportTalkgroup(value, Role.FROM);
    }

    @Override
    public int compareTo(PassportTalkgroup o)
    {
        return Integer.compare(getValue(), o.getValue());
    }
}
