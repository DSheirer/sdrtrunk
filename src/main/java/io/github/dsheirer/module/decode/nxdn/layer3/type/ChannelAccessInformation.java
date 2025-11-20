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

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;

/**
 * Channel access information field.
 */
public class ChannelAccessInformation
{
    private static final int RCN = 0;
    private static final IntField STEP = IntField.length2(1);
    private static final IntField BASE_FREQUENCY = IntField.length3(3);

    private final CorrectedBinaryMessage mMessage;
    private final int mOffset;

    /**
     * Constructs an instance
     * @param message with this field
     * @param offset to this field
     */
    public ChannelAccessInformation(CorrectedBinaryMessage message, int offset)
    {
        mMessage = message;
        mOffset = offset;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("CHANNEL ACCESS: ");
        if(isChannel())
        {
            sb.append("CHANNEL-MODE");
        }
        else
        {
            sb.append("DFA-MODE BASE:").append(getBaseFrequency()).append("HZ STEP SIZE:").append(getStepSize());
        }

        return sb.toString();
    }

    /**
     * Calculates the DFA frequency from the channel number.
     * @param channelNumber for the channel
     * @return calculated frequency.
     */
    public long getFrequency(int channelNumber)
    {
        if(isDFA() && channelNumber > 0)
        {
            return getBaseFrequency() + (getStepSize() * channelNumber);
        }

        return 0;
    }

    /**
     * Indicates format for channel notation, either channel (ie calculated) or direct frequency assignment.
     */
    public RadioChannelNotation getChannelNotation()
    {
        return isDFA() ? RadioChannelNotation.DIRECT_FREQUENCY_ASSIGNMENT : RadioChannelNotation.CHANNEL;
    }

    /**
     * Indicates if the channel notation is Direct Frequency Assignment (DFA)
     */
    public boolean isDFA()
    {
        return mMessage.get(mOffset + RCN);
    }

    /**
     * Indicates if the channel notation is calculated.
     */
    public boolean isChannel()
    {
        return !isDFA();
    }

    /**
     * Step size in Hertz for calculating the channel frequency.
     */
    public long getStepSize()
    {
        if(isDFA())
        {
            int step = mMessage.getInt(STEP, mOffset);

            if(step == 2)
            {
                return 1250;
            }
            else if(step == 3)
            {
                return 3125;
            }
        }

        return 0;
    }

    /**
     * Base frequency for calculating channel frequencies.
     */
    public long getBaseFrequency()
    {
        int base = mMessage.getInt(BASE_FREQUENCY, mOffset);

        return switch(base)
        {
            case 1 -> 100_000_000;
            case 2 -> 330_000_000;
            case 3 -> 400_000_000;
            default -> 450_000_000;
        };
    }
}
