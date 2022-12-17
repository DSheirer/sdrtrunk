/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.dsp.filter.channelizer;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates and caches channel output processor synthesis filters.
 */
public class SynthesisFilterManager
{
    private static final int POLYPHASE_SYNTHESIZER_TAPS_PER_CHANNEL = 9;
    private static final String SEPARATOR = "-";
    private Map<String,float[]> mFilterMap = new HashMap<>();

    /**
     * Design or retrieve a previously cached output processor synthesis filter.
     * @param sampleRate of the tuner
     * @param channelBandwidth per channel
     * @param channelCount as the number of channels being synthesized/aggregated for the output processor (1 or 2)
     * @return filter
     * @throws FilterDesignException if the filter cannot be designed based on the supplied parameters.
     */
    public float[] getFilter(double sampleRate, double channelBandwidth, int channelCount) throws FilterDesignException
    {
        String key = sampleRate + SEPARATOR + channelBandwidth + SEPARATOR + channelCount;

        if(mFilterMap.containsKey(key))
        {
            return mFilterMap.get(key);
        }

        float[] taps = FilterFactory.getSincM2Synthesizer(sampleRate, channelBandwidth, channelCount,
                POLYPHASE_SYNTHESIZER_TAPS_PER_CHANNEL);
        mFilterMap.put(key, taps);
        return taps;
    }
}
