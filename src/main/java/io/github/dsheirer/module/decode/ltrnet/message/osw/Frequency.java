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
public abstract class Frequency extends LtrNetOswMessage
{
    /**
     * Constructs a message
     */
    public Frequency(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    protected abstract FrequencyHigh getFrequencyHigh();
    protected abstract FrequencyLow getFrequencyLow();

    protected abstract boolean hasFrequencyHigh();
    protected abstract boolean hasFrequencyLow();

    /**
     * Channel number for the frequency
     */
    public int getChannel()
    {
        return getHomeRepeater(getMessage());
    }

    /**
     * Frequency for the channel when both the high and low frequency messages are available
     */
    public long getFrequency()
    {
        if(hasFrequencyHigh() && hasFrequencyLow())
        {
            return 150000000 + ((getFrequencyHigh().getHighChannelUnits() + getFrequencyLow().getLowChannelUnits()) * 1250);
        }

        return 0;
    }
}
