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

public enum StandbyTimer
{
    SECONDS_RESERVED(0),
    SECONDS_10(10),
    SECONDS_30(30),
    MINUTES_1(60),
    MINUTES_5(600),
    MINUTES_10(1200),
    MINUTES_30(1800),
    HOURS_1(3600),
    HOURS_2(7200),
    HOURS_4(14400),
    HOURS_8(28800),
    HOURS_12(43200),
    HOURS_24(86400),
    HOURS_48(172800),
    HOURS_72(259200),
    ALWAYS(1000000), //Infinity
    UNKNOWN(0);

    private int mSeconds;

    StandbyTimer(int seconds)
    {
        mSeconds = seconds;
    }

    public int getSeconds()
    {
        return mSeconds;
    }

    public static StandbyTimer fromValue(int value)
    {
        if(0 <= value && value <= 15)
        {
            return StandbyTimer.values()[value];
        }

        return UNKNOWN;
    }
}