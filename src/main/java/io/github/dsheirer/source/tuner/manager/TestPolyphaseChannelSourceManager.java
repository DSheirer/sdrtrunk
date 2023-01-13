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

package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import java.util.SortedSet;

public class TestPolyphaseChannelSourceManager extends PolyphaseChannelSourceManager
{
    private boolean mLockCenterFrequency = true;

    /**
     * Constructs an instance
     *
     * @param tunerController with a center tuned frequency that will be managed by this instance
     */
    public TestPolyphaseChannelSourceManager(TunerController tunerController)
    {
        super(tunerController);
    }

    @Override
    protected long getCenterFrequency(SortedSet<TunerChannel> channels, long currentCenterFrequency) throws IllegalArgumentException
    {
        if(mLockCenterFrequency)
        {
            return currentCenterFrequency;
        }

        return super.getCenterFrequency(channels, currentCenterFrequency);
    }
}
