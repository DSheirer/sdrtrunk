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

package io.github.dsheirer.module.decode.nxdn.identifier;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * NXDN Talkgroup Identifier
 */
public class NXDNTalkgroupIdentifier extends TalkgroupIdentifier
{
    /**
     * Constructs an instance
     *
     * @param value for the talkgroup
     * @param role  for the talkgroup
     */
    public NXDNTalkgroupIdentifier(Integer value, Role role)
    {
        super(value, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.NXDN;
    }

    @Override
    public String toString()
    {
        return switch(getValue())
        {
            case 0x0 -> "0x0000 NULL GROUP";
            case 0xFFF0 -> "0xFFF0 RESERVED GROUP";
            case 0xFFFF -> "0xFFFF ALL GROUPS";
            default -> super.toString();
        };
    }

    /**
     * Creates an NXDN talkgroup identifier with the TO role.
     * @param value of the talkgroup
     * @return identifier
     */
    public static NXDNTalkgroupIdentifier createTo(int value)
    {
        return new NXDNTalkgroupIdentifier(value, Role.TO);
    }
}
