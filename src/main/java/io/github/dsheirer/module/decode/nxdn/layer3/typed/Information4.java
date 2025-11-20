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

package io.github.dsheirer.module.decode.nxdn.layer3.typed;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;

/**
 * Information 4 message
 */
public abstract class Information4 extends SCCH
{
    protected static final int GROUP_UNIT_FLAG = 24;
    protected static final int TYPE_REPEATER_IDLE = 2046;
    protected static final int TYPE_REPEATER_HALT = 2045;
    protected static final int TYPE_REPEATER_FREE = 2044;
    protected static final int TYPE_SITE_ID = 2041;
    protected static final int END_CALL = 31;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran from the frame
     * @param lich from the frame
     */
    public Information4(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    /**
     * Utility method to identify the information 4 message type
     * @param message with Info 4 content
     * @param lich to determine inbound vs outbound
     * @return type
     */
    public static NXDNMessageType getMessageType(CorrectedBinaryMessage message, LICH lich)
    {
        if(lich.isOutbound())
        {
            return switch (getIdentifier(message))
            {
                case TYPE_REPEATER_IDLE -> NXDNMessageType.SCCH_OUT_INFO_4_REPEATER_IDLE;
                case TYPE_REPEATER_HALT -> NXDNMessageType.SCCH_OUT_INFO_4_REPEATER_HALT;
                case TYPE_REPEATER_FREE -> NXDNMessageType.SCCH_OUT_INFO_4_REPEATER_FREE;
                case TYPE_SITE_ID -> NXDNMessageType.SCCH_OUT_INFO_4_SITE_ID;
                default -> getRepeater(message) == END_CALL ? NXDNMessageType.SCCH_OUT_INFO_4_CALL_COMPLETE_DESTINATION :
                        NXDNMessageType.SCCH_OUT_INFO_4_CALL_IN_PROGRESS_DESTINATION;
            };
        }
        else
        {
            return getRepeater(message) == END_CALL ? NXDNMessageType.SCCH_IN_INFO_4_CALL_COMPLETE_DESTINATION :
                    NXDNMessageType.SCCH_IN_INFO_4_CALL_IN_PROGRESS_DESTINATION;
        }
    }

    /**
     * Flag indicating if the destination value is a Group (true) or Unit (false)
     */
    public boolean getGroupUnitFlag()
    {
        return getMessage().get(GROUP_UNIT_FLAG);
    }
}
