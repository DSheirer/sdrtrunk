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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;

import java.util.Collections;
import java.util.List;

/**
 * Date and time announcement
 *
 * NOTE: no ICD - parser is incomplete
 */
public class DateAndTimeAnnouncement extends MacStructure
{
    private static final int VD_FLAG = 8;
    private static final int VT_FLAG = 9;
    private static final int VL_FLAG = 10;
    private static final int[] LOCAL_TIME_OFFSET = {12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] DATE = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43,
        44, 45, 46, 47};
    private static final int[] TIME = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67,
        68, 69, 70, 71};

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public DateAndTimeAnnouncement(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getOpcode());
        sb.append(" DATE:").append(getMessage().getInt(DATE, getOffset()));
        sb.append(" TIME:").append(getMessage().getInt(TIME, getOffset()));
        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
