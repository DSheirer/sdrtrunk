/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.reference;

public enum ExtendedFunction
{
    RADIO_CHECK(0x0000, "RADIO CHECK FROM RADIO:", ArgumentType.SOURCE_RADIO),
    RADIO_DETACH(0x007D, "RADIO DETACH FROM RADIO:", ArgumentType.SOURCE_RADIO ),
    RADIO_UNINHIBIT(0x007E, "RADIO UNINHIBIT FROM RADIO:", ArgumentType.SOURCE_RADIO),
    RADIO_INHIBIT(0x007F, "RADIO INHIBIT FROM RADIO:", ArgumentType.SOURCE_RADIO),
    RADIO_CHECK_ACK(0x0080, "RADIO CHECK ACK TO RADIO:", ArgumentType.TARGET_RADIO),
    RADIO_DETACH_ACK(0x00FD, "RADIO DETACH ACK TO RADIO:", ArgumentType.TARGET_RADIO),
    RADIO_UNINHIBIT_ACK(0x00FE, "RADIO UNINHIBIT ACK TO RADIO:", ArgumentType.TARGET_RADIO),
    RADIO_INHIBIT_ACK(0x00FF, "RADIO INHIBIT ACK TO RADIO:", ArgumentType.TARGET_RADIO),
    GROUP_REGROUP_CREATE_SUPERGROUP(0x0200, "GROUP REGROUP CREATE SUPERGROUP:", ArgumentType.TALKGROUP),
    GROUP_REGROUP_CANCEL_SUPERGROUP(0x0201, "GROUP REGROUP CANCEL SUPERGROUP", ArgumentType.NONE),
    GROUP_REGROUP_ACK_CREATE_SUPERGROUP(0x0280, "GROUP REGROUP ACK CREATE SUPERGROUP:", ArgumentType.TALKGROUP),
    GROUP_REGROUP_ACK_CANCEL_SUPERGROUP(0x0281, "GROUP REGROUP ACK CANCEL SUPERGROUP:", ArgumentType.TALKGROUP),
    UNKNOWN(-1, "UNKNOWN", ArgumentType.NONE);

    private int mFunction;
    private String mLabel;
    private ArgumentType mArgumentType;

    ExtendedFunction(int function, String label, ArgumentType argumentType)
    {
        mFunction = function;
        mLabel = label;
        mArgumentType = argumentType;
    }

    public String getLabel()
    {
        return mLabel;
    }

    /**
     * Indicates the type of address carried in the argument field.
     */
    public ArgumentType getArgumentType()
    {
        return mArgumentType;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    public static ExtendedFunction fromValue(int function)
    {
        switch(function)
        {
            case 0x0000:
                return RADIO_CHECK;
            case 0x007D:
                return RADIO_DETACH;
            case 0x007E:
                return RADIO_UNINHIBIT;
            case 0x007F:
                return RADIO_INHIBIT;
            case 0x0080:
                return RADIO_CHECK_ACK;
            case 0x00FD:
                return RADIO_DETACH_ACK;
            case 0x00FE:
                return RADIO_UNINHIBIT_ACK;
            case 0x00FF:
                return RADIO_INHIBIT_ACK;
            case 0x0200:
                return GROUP_REGROUP_CREATE_SUPERGROUP;
            case 0x0201:
                return GROUP_REGROUP_CANCEL_SUPERGROUP;
            case 0x0280:
                return GROUP_REGROUP_ACK_CREATE_SUPERGROUP;
            case 0x0281:
                return GROUP_REGROUP_ACK_CANCEL_SUPERGROUP;
        }

        return UNKNOWN;
    }
}
