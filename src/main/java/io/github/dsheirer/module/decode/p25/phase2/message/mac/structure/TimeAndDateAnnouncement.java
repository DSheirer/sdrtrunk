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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

/**
 * Time and date announcement
 */
public class TimeAndDateAnnouncement extends MacStructure
{
    private static final int VD_FLAG = 8;
    private static final int VT_FLAG = 9;
    private static final int VL_FLAG = 10;
    private static final int LOCAL_TIME_OFFSET_SIGN = 11;
    private static final IntField LOCAL_TIME_OFFSET = IntField.range(12, 23);
    private static final IntField MONTH = IntField.range(24, 27);
    private static final IntField DAY = IntField.range(28, 32);
    private static final IntField YEAR = IntField.range(33, 45);
    private static final IntField HOURS = IntField.range(48, 52);
    private static final IntField MINUTES = IntField.range(53, 58);
    private static final IntField SECONDS = IntField.range(59, 64);

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public TimeAndDateAnnouncement(CorrectedBinaryMessage message, int offset)
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
        sb.append(" ").append(getDateAndTime());
        return sb.toString();
    }

    /**
     * Date and time value
     * @return date in milliseconds since epoch (1970).
     */
    public OffsetDateTime getDateAndTime()
    {
        boolean hasDate = getMessage().get(VD_FLAG + getOffset());
        boolean hasTime = getMessage().get(VT_FLAG + getOffset());
        boolean hasOffset = getMessage().get(VL_FLAG + getOffset());
        int year = hasDate ? getInt(YEAR) : 0;
        int month = hasDate ? getInt(MONTH) : 0;
        int day = hasDate ? getInt(DAY) : 0;
        int hours = hasTime ? getInt(HOURS) : 0;
        int minutes = hasTime ? getInt(MINUTES) : 0;
        int seconds = hasTime ? getInt(SECONDS) : 0;
        int offsetMinutes = hasOffset ? getInt(LOCAL_TIME_OFFSET) : 0;
        offsetMinutes *= getMessage().get(LOCAL_TIME_OFFSET_SIGN + getOffset()) ? -1 : 1;
        return OffsetDateTime.of(year, month, day, hours, minutes, seconds, 0,
                ZoneOffset.ofHoursMinutes(0, offsetMinutes));
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
