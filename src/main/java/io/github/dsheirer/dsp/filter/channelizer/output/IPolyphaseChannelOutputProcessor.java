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
package io.github.dsheirer.dsp.filter.channelizer.output;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import java.util.List;

public interface IPolyphaseChannelOutputProcessor
{
    /**
     * Description of the state/configuration of this output processor
     */
    String getStateDescription();

    /**
     * Start processing channel results
     */
    void start();

    /**
     * Stop processing channel results.
     */
    void stop();
    /**
     * Receive and enqueue output results from the polyphase analysis channelizer
     * @param channelResults to enqueue
     * @param timestamp for the first channel results buffer
     */
    void receiveChannelResults(List<float[]> channelResults, long timestamp);

    /**
     * Listener to receive assembled complex samples buffers
     */
    void setListener(Listener<ComplexSamples> listener);

    /**
     * Sets the desired frequency offset from center.  The samples will be mixed with an oscillator set to this offset
     * frequency to produce an output where the desired signal is centered in the passband.
     *
     * @param frequency in hertz
     */
    void setFrequencyOffset(long frequency);

    /**
     * Indicates the number of input channels processed by this output processor
     */
    int getInputChannelCount();

    /**
     * Updates the input polyphase channel index(es) used by this output processor
     * @param indexes for the channel
     */
    void setPolyphaseChannelIndices(List<Integer> indexes);

    /**
     * List of current polyphase channel indices for the output processor
     */
    int getPolyphaseChannelIndexCount();

    /**
     * Updates the synthesis filter taps for this output processor
     * @param filter for the output processor
     */
    void setSynthesisFilter(float[] filter);

    void dispose();
}
