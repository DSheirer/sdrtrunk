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

package io.github.dsheirer.identifier.talkgroup;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.protocol.Protocol;

/**
 * LTR talkgroup identifier
 */
public class LTRTalkgroup extends TalkgroupIdentifier implements Comparable<LTRTalkgroup>
{
    public LTRTalkgroup(Integer talkgroup, Role role)
    {
        super(talkgroup, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.LTR;
    }

    /**
     * This identifier formatted as HH:GGG where H=home channel and G=group
     */
    public String formatted()
    {
        return String.format("%02d-%03d", getHomeChannel(), getTalkgroup());
    }

    /**
     * Home channel for the identifier
     */
    public int getHomeChannel()
    {
        return (getValue() >> 8) & 0x1F;
    }

    /**
     * Talkgroup
     */
    public int getTalkgroup()
    {
        return getValue() & 0xFF;
    }

    /**
     * Creates an LTR-Net identifier from the integer value that contains both the fleet and the ident with a TO role
     */
    public static LTRTalkgroup create(int talkgroup)
    {
        return new LTRTalkgroup(talkgroup, Role.TO);
    }

    @Override
    public int compareTo(LTRTalkgroup o)
    {
        return getValue().compareTo(o.getValue());
    }
}
