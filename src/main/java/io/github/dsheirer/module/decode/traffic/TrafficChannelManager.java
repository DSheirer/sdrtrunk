/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.traffic;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.module.Module;

/**
 * Traffic Channel Manager base class
 */
public abstract class TrafficChannelManager extends Module
{
    private long mCurrentControlFrequency;

    /**
     * Constructs an instance.
     */
    public TrafficChannelManager()
    {
    }

    /**
     * Current control frequency
     * @return frequency in hertz
     */
    protected long getCurrentControlFrequency()
    {
        return mCurrentControlFrequency;
    }

    /**
     * Sets the current control frequency.
     * @param frequency that is now the control frequency
     * @param parentChannel channel configuration
     */
    public void setCurrentControlFrequency(long frequency, Channel parentChannel)
    {
        long previous = mCurrentControlFrequency;
        mCurrentControlFrequency = frequency;
        processControlFrequencyUpdate(previous, frequency, parentChannel);
    }

    /**
     * Subclass implementation to receive notification that the control channel frequency has changed when the source
     * is set for multiple frequencies, or in the case of DMR when the REST channel changes.  Subclass should remove
     * any traffic channels currently allocated against the new control frequency.
     *
     * @param previous frequency for the control channel (to remove from allocated channels)
     * @param current frequency for the control channel (to add to allocated channels)
     * @param channel for the current control channel
     */
    protected abstract void processControlFrequencyUpdate(long previous, long current, Channel channel);
}
