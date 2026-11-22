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

package io.github.dsheirer.module.decode.nxdn.layer3.type;

/**
 * Transmission mode, symbol rate and outer (+/-3) symbol deviation per NXDN TS 1-A Table 3.3-1.
 */
public enum TransmissionMode
{
    M4800("4800", 2400, 1050),
    M9600("9600", 4800, 2400),
    TYPE_D("TYPE D", 2400, 1050);

    private final String mLabel;
    private final int mSymbolRate;
    private final int mOuterSymbolDeviation;

    /**
     * Constructs an instance
     *
     * @param label to display
     * @param symbolRate in symbols per second
     * @param outerSymbolDeviation frequency deviation of the +/-3 symbols in Hertz
     */
    TransmissionMode(String label, int symbolRate, int outerSymbolDeviation)
    {
        mLabel = label;
        mSymbolRate = symbolRate;
        mOuterSymbolDeviation = outerSymbolDeviation;
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

    /**
     * Frequency deviation of the outer (+/-3) symbols in Hertz.
     */
    public int getOuterSymbolDeviation()
    {
        return mOuterSymbolDeviation;
    }

    /**
     * Indicates if this mode is Type D
     */
    public boolean isTypeD()
    {
        return this == TransmissionMode.TYPE_D;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
