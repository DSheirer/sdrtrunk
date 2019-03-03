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

package io.github.dsheirer.module.decode.p25.phase2.message.mac;

/**
 * MAC opcode is used to convey the type of MAC PDU for a MAC message
 */
public enum MacOpcode
{
    MAC_0_RESERVED,
    MAC_1_PTT,
    MAC_2_END_PTT,
    MAC_3_IDLE,
    MAC_4_ACTIVE,
    MAC_5_RESERVED,
    MAC_6_HANGTIME,
    MAC_7_RESERVED,
    MAC_UNKNOWN;

    public static MacOpcode fromValue(int value)
    {
        if(0 <= value && value <= 7)
        {
            return MacOpcode.values()[value];
        }

        return MAC_UNKNOWN;
    }
}
