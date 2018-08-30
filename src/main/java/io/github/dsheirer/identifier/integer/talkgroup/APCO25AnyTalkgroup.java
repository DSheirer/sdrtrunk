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

import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.Role;

/**
 * APCO25 Talkgroup Identifier with an ANY role.
 */
public class APCO25AnyTalkgroup extends APCO25Talkgroup
{
    /**
     * Constructs an APCO25 Talkgroup Identifier with an ANY role.
     *
     * @param value of the talkgroup
     */
    public APCO25AnyTalkgroup(int value)
    {
        super(value);
    }

    @Override
    public Role getRole()
    {
        return Role.ANY;
    }

    @Override
    boolean isGroup()
    {
        return false;
    }

    /**
     * Creates an ANY APCO-25 talkgroup identifier
     */
    public static IIdentifier create(int talkgroup)
    {
        return new APCO25AnyTalkgroup(talkgroup);
    }
}
