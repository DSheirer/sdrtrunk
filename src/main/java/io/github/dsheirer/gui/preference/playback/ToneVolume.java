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

public enum ToneVolume
{
    V1(1),
    V2(2),
    V3(3),
    V4(4),
    V5(5),
    V6(6),
    V7(7),
    V8(8),
    V9(9),
    V10(10);

    private int mValue;

    ToneVolume(int value)
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

    public static ToneVolume fromValue(int value)
    {
        for(ToneVolume toneVolume: ToneVolume.values())
        {
            if(toneVolume.getValue() == value)
            {
                return toneVolume;
            }
        }

        return V3; //default
    }
}
