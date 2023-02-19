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

package io.github.dsheirer.source.tuner.sdrplay.api.device;

/**
 * Software decimation rates supported by the API for SDRPlay RSP devices
 */
public enum Decimate
{
    X1(1, 16),
    X2(2, 16),
    X4(4, 17),
    X8(8, 17),
    X16(16, 18),
    X32(32, 20);

    private int mValue;
    private int mSampleSize;

    /**
     * Constructs an instance
     * @param value
     * @param sampleSize in bits to represent effective dynamic range.
     */
    Decimate(int value, int sampleSize)
    {
        mValue = value;
        mSampleSize = sampleSize;
    }

    /**
     * Decimation value.
     * @return
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Sample size in bits to represent effective dynamic range for the decimation value.
     */
    public int getSampleSize()
    {
        return mSampleSize;
    }

    /**
     * Indicates if software decimation is enabled.
     * @return true for all values except X1.
     */
    public boolean isEnabled()
    {
        return !this.equals(X1);
    }

    @Override
    public String toString()
    {
        if(this.equals(X1))
        {
            return "None";
        }

        return "x" + getValue();
    }
}
