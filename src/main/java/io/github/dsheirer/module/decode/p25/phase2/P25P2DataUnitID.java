/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
package io.github.dsheirer.module.decode.p25.phase2;

/**
 * P25 Phase 2 Data Unit ID (DUID) enumeration
 */
public enum P25P2DataUnitID
{
    UNKNOWN(-1, -1, false, "UNKN ");

    private int mValue;
    private int mMessageLength;
    private boolean mTrailingStatusDibit;
    private String mLabel;

    P25P2DataUnitID(int value, int length, boolean trailingStatusDibit, String label)
    {
        mValue = value;
        mMessageLength = length;
        mTrailingStatusDibit = trailingStatusDibit;
        mLabel = label;
    }

    /**
     * Data Unit ID value
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Length of the message in bits
     */
    public int getMessageLength()
    {
        return mMessageLength;
    }

    /**
     * Short display label
     */
    public String getLabel()
    {
        return mLabel;
    }

    /**
     * Indicates if the message has a trailing status dibit that must be processed
     */
    public boolean hasTrailingStatusDibit()
    {
        return mTrailingStatusDibit;
    }

    /**
     * Lookup the Data Unit ID from an integer value
     */
    public static P25P2DataUnitID fromValue(int value)
    {
        switch(value)
        {
            default:
                throw new IllegalArgumentException("Data Unit ID must be in range 0 - 15");
        }
    }
}
