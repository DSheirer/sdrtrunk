/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.dsp.filter.channelizer.output;

import io.github.dsheirer.dsp.filter.channelizer.PolyphaseChannelResultsBuffer;
import io.github.dsheirer.sample.complex.TimestampedBufferAssembler;
import io.github.dsheirer.source.Source;

import java.util.List;

public interface IPolyphaseChannelOutputProcessor
{
    /**
     * Receive and enqueue output results from the polyphase analysis channelizer
     * @param channelResultsBuffer to enqueue
     */
    void receiveChannelResults(PolyphaseChannelResultsBuffer channelResultsBuffer);

    /**
     * Process the channel output channel results queue and deliver the output to the listener
     *
     * @param timestampedBufferAssembler to receive the processed channel results
     */
    void processChannelResults(TimestampedBufferAssembler timestampedBufferAssembler);

    /**
     * Sets the desired frequency offset from center.  The samples will be mixed with an oscillator set to this offset
     * frequency to produce an output where the desired signal is centered in the passband.
     *
     * @param frequency in hertz
     */
    void setFrequencyOffset(long frequency);

    /**
     * Indicates the number of input channels processed by this output processor
     * @return
     */
    int getInputChannelCount();

    /**
     * Updates the input polyphase channel index(es) used by this output processor
     * @param indexes
     */
    void setPolyphaseChannelIndices(List<Integer> indexes);

    /**
     * Sets the listener to receive buffer overflow notifications
     * @param source to receive overflow notifications
     */
    void setSourceOverflowListener(Source source);
}
