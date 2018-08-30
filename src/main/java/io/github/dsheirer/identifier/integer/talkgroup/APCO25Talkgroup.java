/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.identifier.integer.talkgroup;

import io.github.dsheirer.protocol.Protocol;

/**
 * Abstract APCO25 integer talkgroup identifier
 */
public abstract class APCO25Talkgroup extends AbstractTalkgroup
{
    /**
     * Constructs the APCO25 talkgroup identifier
     * @param value of talkgroup
     */
    public APCO25Talkgroup(int value)
    {
        super(value);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    /**
     * Indicates if this is a group (true) or individual (false) identifier
     * @return true if this is a group identifier
     */
    abstract boolean isGroup();

    @Override
    public boolean equals(Object other)
    {
        if(other instanceof APCO25Talkgroup)
        {
            APCO25Talkgroup otherTalkgroup = (APCO25Talkgroup)other;

            return !(isGroup() ^ otherTalkgroup.isGroup()) && getValue() == otherTalkgroup.getValue();
        }

        return false;
    }
}
