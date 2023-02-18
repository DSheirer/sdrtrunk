/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.identifier;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * DMR Talkgroup identifier
 */
public class DMRTalkgroup extends TalkgroupIdentifier
{
    private boolean mCompressed = false;
    private int mOriginalValue;

    /**
     * Constructs an instance
     * @param value of the talkgroup
     */
    public DMRTalkgroup(int value)
    {
        super(value, Role.TO);
        mOriginalValue = value;
    }

    /**
     * Constructs an instance
     * @param value of the talkgroup
     * @param role of the talkgroup
     */
    public DMRTalkgroup(int value, Role role)
    {
        super(value, role);
        mOriginalValue = value;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.DMR;
    }

    @Override
    public String toString()
    {
        if(mCompressed)
        {
            return super.toString() + "(c)";
        }
        else
        {
            return super.toString();
        }
    }

    /**
     * Sets the compressed flag for this value.
     *
     * Talkgroup compression is an alternate format for DMR systems that allows the end user to dial a 7-digit number
     * on their mobile keypad using just the digits 0 - 9, to address a talkgroup.  The downside to this is that the
     * total available talkgroup address space is reduced to just those dialable talkgroups.
     *
     * @param compressed true when using compressed talkgroup format.
     */
    public void setCompressed(boolean compressed)
    {
        mCompressed = compressed;

        if(mCompressed)
        {
            setValue(getCompressed(mOriginalValue));
        }
        else
        {
            setValue(mOriginalValue);
        }
    }

    /**
     * Converts the air interface value to a compressed value described in ETSI 102 361-2, V.2.4.1, Paragraph C2.1.2
     *
     * This has been observed in use on Hytera Tier III systems.
     *
     * See: https://cwh050.blogspot.com/2021/03/converting-flat-number-to-dmr-id.html?m=1
     *
     * @param original value to convert
     * @return converted value.
     */
    public static int getCompressed(int original)
    {
        int converted = 0;
        int units = original / 1_464_100;
        converted += (units * 1_000_000);
        original -= (units * 1_464_100);

        units = original / 146_410;
        converted += (units * 100_000);
        original -= (units * 146_410);

        units = original / 14_641;
        converted += (units * 10_000);
        original -= (units * 14_641);

        units = original / 1_331;
        converted += (units * 1_000);
        original -= (units * 1_331);

        units = original / 121;
        converted += (units * 100);
        original -= (units * 121);

        units = original / 11;
        converted += (units * 10);
        original -= (units * 11);

        converted += original;

        return converted;
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
