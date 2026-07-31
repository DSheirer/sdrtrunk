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

package io.github.dsheirer.module.decode.nxdn.layer3.scch;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;

/**
 * Information 3 message
 */
public abstract class Information3 extends SCCH
{
    protected static final int TYPE_INITIALIZATION_VECTOR = 31;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran from the frame
     * @param lich from the frame
     */
    public Information3(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    /**
     * Utility method to identify the information 4 message type
     * @return type
     */
    public static NXDNMessageType getMessageType(CorrectedBinaryMessage message, LICH lich)
    {
        if(getHomeRepeater(message) == TYPE_INITIALIZATION_VECTOR)
        {
            return lich.isOutbound() ? NXDNMessageType.TYPE_D_SCCH_OUT_INFO_3_INITIALIZATION_VECTOR_PART2 :
                    NXDNMessageType.TYPE_D_SCCH_IN_INFO_3_INITIALIZATION_VECTOR_PART2;
        }

        return lich.isOutbound() ? NXDNMessageType.TYPE_D_SCCH_OUT_INFO_3_CALL_IN_PROGRESS_SOURCE :
                NXDNMessageType.TYPE_D_SCCH_IN_INFO_3_CALL_IN_PROGRESS_SOURCE;
    }
}
