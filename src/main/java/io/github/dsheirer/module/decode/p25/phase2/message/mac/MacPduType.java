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

package io.github.dsheirer.module.decode.p25.phase2.message.mac;

/**
 * MAC opcode is used to convey the type of MAC PDU for a MAC message
 */
public enum MacPduType
{
    MAC_0_SIGNAL("SIGNAL"),
    MAC_1_PTT("PUSH-TO-TALK"),
    MAC_2_END_PTT("END PUSH-TO-TALK"),
    MAC_3_IDLE("IDLE"),
    MAC_4_ACTIVE("ACTIVE"),
    MAC_5_RESERVED("RESERVED-5"),
    MAC_6_HANGTIME("HANGTIME"),
    MAC_7_RESERVED("RESERVED-7"),
    MAC_UNKNOWN("UNKNOWN");

    private String mLabel;

    MacPduType(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    public static MacPduType fromValue(int value)
    {
        if(0 <= value && value <= 7)
        {
            return MacPduType.values()[value];
        }

        return MAC_UNKNOWN;
    }
}
