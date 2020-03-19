/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.gui.preference.playback;

public enum ToneFrequency
{
    F300(300),
    F400(400),
    F500(500),
    F600(600),
    F700(700),
    F800(800),
    F900(900),
    F1000(1000),
    F1100(1100),
    F1200(1200),
    F1300(1300),
    F1400(1400),
    F1500(1500),
    F1600(1600),
    F1700(1700),
    F1800(1800),
    F1900(1900),
    F2000(2000),
    F2100(2100),
    F2200(2200),
    F2300(2300),
    F2400(2400),
    F2500(2500);

    private int mValue;

    ToneFrequency(int value)
    {
        mValue = value;
    }

    public int getValue()
    {
        return mValue;
    }

    @Override
    public String toString()
    {
        return String.valueOf(mValue);
    }

    public static ToneFrequency fromValue(int value)
    {
        for(ToneFrequency toneFrequency: ToneFrequency.values())
        {
            if(toneFrequency.getValue() == value)
            {
                return toneFrequency;
            }
        }

        return F1200; //default
    }
}
