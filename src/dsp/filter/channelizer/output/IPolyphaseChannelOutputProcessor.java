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
package dsp.filter.channelizer.output;

import sample.complex.ComplexSampleListener;

import java.util.List;

public interface IPolyphaseChannelOutputProcessor
{
    /**
     * Receive and enqueue output results from the polyphase analysis channelizer
     * @param channelResults to enqueue
     */
    void receiveChannelResults(float[] channelResults);

    /**
     * Process the channel output channel results queue and deliver the output to the listener
     *
     * @param listener to receive the processed channel results
     */
    void processChannelResults(ComplexSampleListener listener);

    /**
     * Specifies the frequency correction (+/-) that should be applied to samples extracted from the polyphase
     * channelizer output results.
     *
     * @param frequencyCorrection correction value
     */
    void setFrequencyCorrection(long frequencyCorrection);

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
}
