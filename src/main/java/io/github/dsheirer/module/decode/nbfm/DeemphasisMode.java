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

package io.github.dsheirer.module.decode.nbfm;

/**
 * FM de-emphasis
 *
 * Per TIA-603-E, all NBFM use a -6 dB per octave roll off from 300 Hz to 3000 Hz.
 * It also specifies an additional -12 dB above 2500 (not implemented to save on filter passes, the resampler
 * takes care of a lot of that), and an additional -6 dB below 500 (not implemented to save on filter passes, the
 * existing high pass filter takes care of most of that). European standard has same specifications (unlike
 * commercial FM, which the search engines struggle with).
 */
public enum DeemphasisMode
{
    NONE("None", 0),
    //OTHER_166US("166 µs (Other)", 6024),
    NBFM_300("-6dB/octave @ 300-3KHz", 300);

    private final String mLabel;
    private final int mCutoff;

    DeemphasisMode(String label, int cutoffFreq)
    {
        mLabel = label;
        mCutoff = cutoffFreq;
    }

    public int getCutoff()
    {
        return mCutoff;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
