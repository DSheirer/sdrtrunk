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
package io.github.dsheirer.module.decode.ltrnet.message.osw;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * Base frequency message
 */
public abstract class FrequencyHigh extends Frequency
{
    private FrequencyLow mFrequencyLow;

    /**
     * Constructs a message
     */
    public FrequencyHigh(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    /**
     * Sets the corresponding low frequency message so that the correct frequency can be calculated
     */
    public void setFrequencyLow(FrequencyLow frequencyLow)
    {
        mFrequencyLow = frequencyLow;
    }

    /**
     * Returns the value of the high order (15 - 12) frequency bits which are
     * the lower 4 bits of the Free field, left shifted by 12, representing the
     * frequency value in units of .00125 MHz
     */
    public int getHighChannelUnits()
    {
        return Integer.rotateLeft(getMessage().getInt(29, 32), 12);
    }

    @Override
    protected FrequencyHigh getFrequencyHigh()
    {
        return this;
    }

    @Override
    protected FrequencyLow getFrequencyLow()
    {
        return mFrequencyLow;
    }

    @Override
    protected boolean hasFrequencyHigh()
    {
        return true;
    }

    @Override
    protected boolean hasFrequencyLow()
    {
        return mFrequencyLow != null;
    }
}
