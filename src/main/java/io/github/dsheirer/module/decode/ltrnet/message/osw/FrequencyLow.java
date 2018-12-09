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
public abstract class FrequencyLow extends Frequency
{
    private FrequencyHigh mFrequencyHigh;

    /**
     * Constructs a message
     */
    public FrequencyLow(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    public void setFrequencyHigh(FrequencyHigh frequencyHigh)
    {
        mFrequencyHigh = frequencyHigh;
    }

    /**
     * Returns the value of the low order (11 - 0) frequency bits which are
     * contained in the Group and Free fields representing the frequency value
     * units of .00125 MHz
     */
    public int getLowChannelUnits()
    {
        return getMessage().getInt(21, 32);
    }

    @Override
    protected FrequencyHigh getFrequencyHigh()
    {
        return mFrequencyHigh;
    }

    @Override
    protected FrequencyLow getFrequencyLow()
    {
        return this;
    }

    @Override
    protected boolean hasFrequencyHigh()
    {
        return mFrequencyHigh != null;
    }

    @Override
    protected boolean hasFrequencyLow()
    {
        return true;
    }
}
