/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.audio.convert;

import net.sourceforge.lame.mp3.Lame;

/**
 * MP3 encoder settings.
 *
 * A curated list of LAME audio encoder settings that are optimized for mono-channel voice that is originally
 * sampled at 8,000 Hz.
 *
 * See: https://svn.code.sf.net/p/lame/svn/trunk/lame/doc/html/usage.html
 */
public enum MP3Setting
{
    CBR_16(false, 16, "CBR - Constant Bit Rate 16 kbps (default)"),
    CBR_32(false, 32, "CBR - Constant Bit Rate 32 kbps"),
    ABR_56(true, 56, "ABR - Average Bit Rate 56 kbps"),
    VBR_5(true, Lame.QUALITY_MIDDLE, "VBR - Middle Quality (5)"),
    VBR_7(true, Lame.QUALITY_LOW, "VBR - Low Quality (7)");

    private boolean mVariableBitRate;
    private int mSetting;
    private String mLabel;

    /**
     * Constructs an instance
     * @param variableBitRate flag
     * @param setting for the entry
     * @param label to display
     */
    MP3Setting(boolean variableBitRate, int setting, String label)
    {
        mVariableBitRate = variableBitRate;
        mSetting = setting;
        mLabel = label;
    }

    /**
     * Default setting
     */
    public static MP3Setting getDefault()
    {
        return MP3Setting.CBR_16;
    }

    /**
     * Indicates if the entry is Constant Bit Rate (CBR), or Average/Variable Bit Rate (ABR or VBR)
     * @return true if CBR
     */
    public boolean isVariableBitRate()
    {
        return mVariableBitRate;
    }

    /**
     * LAME encoder quality value
     */
    public int getSetting()
    {
        return mSetting;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
