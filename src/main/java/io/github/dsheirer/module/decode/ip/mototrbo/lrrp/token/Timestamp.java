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

package io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * LRRP Timestamp Token
 *
 * Start Token: 0x34
 * Total Length: 6 bytes
 */
public class Timestamp extends Token
{
    private static final int[] YEAR = new int[]{8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21};
    private static final int[] MONTH = new int[]{22, 23, 24, 25};
    private static final int[] DAY = new int[]{26, 27, 28, 29, 30};
    private static final int[] HOUR = new int[]{31, 32, 33, 34, 35};
    private static final int[] MINUTE = new int[]{36, 37, 38, 39, 40, 41};
    private static final int[] SECOND = new int[]{42, 43, 44, 45, 46, 47};

    /**
     * Constructs an instance of a timestamp token.
     *
     * @param message containing the timestamp
     * @param offset to the start of the token
     */
    public Timestamp(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    @Override
    public TokenType getTokenType()
    {
        return TokenType.TIMESTAMP;
    }

    /**
     * Timestamp value in milliseconds
     */
    public long getTimestamp()
    {
        int year = getMessage().getInt(YEAR, getOffset());
        int month = getMessage().getInt(MONTH, getOffset()) - 1;
        int day = getMessage().getInt(DAY, getOffset());
        int hour = getMessage().getInt(HOUR, getOffset());
        int minute = getMessage().getInt(MINUTE, getOffset());
        int second = getMessage().getInt(SECOND, getOffset());

        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(year, month, day, hour, minute, second);
        return calendar.getTimeInMillis();
    }

    @Override
    public String toString()
    {
        return "TIME:" + new Date(getTimestamp()).toString().toUpperCase();
    }
}
