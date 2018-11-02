/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.p25.reference;

public enum ReadyTimer
{
    SECONDS_RESERVED(0),
    SECONDS_1(1),
    SECONDS_2(2),
    SECONDS_4(4),
    SECONDS_6(6),
    SECONDS_8(8),
    SECONDS_10(10),
    SECONDS_15(15),
    SECONDS_20(20),
    SECONDS_25(25),
    SECONDS_30(30),
    SECONDS_60(60),
    SECONDS_120(120),
    SECONDS_180(180),
    SECONDS_300(300),
    SECONDS_ALWAYS(86400), //Infinity
    UNKNOWN(0);

    private int mSeconds;

    ReadyTimer(int seconds)
    {
        mSeconds = seconds;
    }

    public int getSeconds()
    {
        return mSeconds;
    }

    public static ReadyTimer fromValue(int value)
    {
        if(0 <= value && value <= 15)
        {
            return ReadyTimer.values()[value];
        }

        return UNKNOWN;
    }
}