/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.module.decode.mpt1327.identifier;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.preference.identifier.talkgroup.MPT1327TalkgroupFormatter;
import io.github.dsheirer.protocol.Protocol;

/**
 * MPT-1327 Talkgroup Identifier.
 *
 * Note: the isGroup() method of the talkgroup identifier parent class does not apply here since we cannot determine
 * if a talkgroup is a group or a unit identifier.
 */
public class MPT1327Talkgroup extends TalkgroupIdentifier
{
    public MPT1327Talkgroup(Integer value, Role role)
    {
        super(value, role, false);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.MPT1327;
    }

    /**
     * Prefix for this talkgroup
     */
    public int getPrefix()
    {
        return MPT1327TalkgroupFormatter.getPrefix(getValue());
    }

    /**
     * Ident for this talkgroup
     */
    public int getIdent()
    {
        return MPT1327TalkgroupFormatter.getIdent(getValue());
    }

    /**
     * Creates a FROM role talkgroup identifier
     */
    public static MPT1327Talkgroup createFrom(int prefix, int ident)
    {
        if(0 < ident && ident <= 8100)
        {
            return new MPT1327Talkgroup(((prefix << 13) + ident), Role.FROM);
        }
        else
        {
            //Mask the prefix for ident values of 0, 8101-8192
            return new MPT1327Talkgroup(ident, Role.FROM);
        }
    }

    /**
     * Creates a TO role talkgroup identifier
     */
    public static MPT1327Talkgroup createTo(int prefix, int ident)
    {
        if(0 < ident && ident <= 8100)
        {
            return new MPT1327Talkgroup(((prefix << 13) + ident), Role.TO);
        }
        else
        {
            //Mask the prefix for ident values of 0, 8101-8192
            return new MPT1327Talkgroup(ident, Role.TO);
        }
    }
}
