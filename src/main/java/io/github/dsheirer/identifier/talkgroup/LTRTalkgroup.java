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
    private static final int AREA_MASK = 0x2000;
    private static final int HOME_MASK = 0x1F00;
    private static final int GROUP_MASK = 0xFF;

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

    public int getArea()
    {
        return (getValue() & AREA_MASK) >> 13;
    }

    /**
     * Home channel for the identifier
     */
    public int getHomeChannel()
    {
        return (getValue() & HOME_MASK) >> 8;
    }

    /**
     * Talkgroup
     */
    public int getTalkgroup()
    {
        return getValue() & GROUP_MASK;
    }

    /**
     * Creates an LTR identifier from the integer value that contains both the fleet and the ident with a TO role
     */
    public static LTRTalkgroup create(int talkgroup)
    {
        return new LTRTalkgroup(talkgroup, Role.TO);
    }

    public static int create(int area, int home, int group)
    {
        int value = (area << 13);
        value += (home << 8);
        value += group;

        return value;
    }

    @Override
    public int compareTo(LTRTalkgroup o)
    {
        return getValue().compareTo(o.getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LTRTalkgroup)) return false;
        return compareTo((LTRTalkgroup) o) == 0;
    }
}
