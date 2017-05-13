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

import sample.complex.IComplexSampleListener;

public interface IPolyphaseChannelOutputProcessor
{
    /**
     * Process the channel output channel results from the polyphase channelizer filter
     *
     * @param channelResults to process
     * @param listener to receive the processed channel results
     */
    void process(float[] channelResults, IComplexSampleListener listener);

    /**
     * Specifies the positive or negative frequency correction that should be applied to samples extracted from
     * the polyphase channelizer output results.
     * @param frequency
     */
    void setFrequencyCorrection(long frequency);
}
