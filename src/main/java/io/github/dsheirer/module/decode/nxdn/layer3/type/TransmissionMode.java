/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3.type;

/**
 * Transmission mode and symbol rate
 */
public enum TransmissionMode
{
    M4800("4800", 2400),
    M9600("9600", 4800);

    private final String mLabel;
    private final int mSymbolRate;

    /**
     * Constructs an instance
     *
     * @param label to display
     */
    TransmissionMode(String label, int symbolRate)
    {
        mLabel = label;
        mSymbolRate = symbolRate;
    }

    public String getLabel()
    {
        return mLabel;
    }

    /**
     * Symbol rate for the transmission mode.
     */
    public int getSymbolRate()
    {
        return mSymbolRate;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
