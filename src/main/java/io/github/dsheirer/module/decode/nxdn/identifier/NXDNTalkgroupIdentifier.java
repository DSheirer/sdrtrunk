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
import java.text.DecimalFormat;

/**
 * NXDN Talkgroup Identifier
 */
public class NXDNTalkgroupIdentifier extends TalkgroupIdentifier
{
    private static final DecimalFormat REPEATER_FORMAT = new DecimalFormat("00");
    private static final DecimalFormat TALKGROUP_FORMAT = new DecimalFormat("0000");
    private boolean mTypeD = false;

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

    /**
     * Indicates if the identifier is for a Type-D system.
     */
    public boolean isTypeD()
    {
        return mTypeD;
    }

    /**
     * Sets the Type-D flag for this ID.
     * @param typeD true to indicate that this is a Type-D identifier.
     */
    public void setTypeD(boolean typeD)
    {
        mTypeD = typeD;
    }

    /**
     * Home repeater value for a Type-D identifier.
     */
    public int getTypeDHomeRepeater()
    {
        return (getValue() >> 11) & 0x1F;
    }

    /**
     * Talkgroup ID for a Type-D identifier.
     * @return radio ID.
     */
    public int getTypeDTalkgroup()
    {
        return getValue() & 0x7FF;
    }


    @Override
    public String toString()
    {
        if(isTypeD())
        {
            return REPEATER_FORMAT.format(getTypeDHomeRepeater()) + "-" + TALKGROUP_FORMAT.format(getTypeDTalkgroup());
        }

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

    /**
     * Creates a Type-D talkgroup identifier with the TO role.
     * @param repeater that is home for the ID
     * @param talkgroup value
     * @return identifier
     */
    public static NXDNTalkgroupIdentifier createTypeDTo(int repeater, int talkgroup)
    {
        NXDNTalkgroupIdentifier identifier = new NXDNTalkgroupIdentifier((repeater << 11) + talkgroup, Role.TO);
        identifier.setTypeD(true);
        return identifier;
    }
}
