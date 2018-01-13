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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class OneChannelOutputProcessor extends ChannelOutputProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(OneChannelOutputProcessor.class);

    private int mChannelOffset;

    /**
     * Processor to extract a single channel from a polyphase channelizer and produce an output I/Q complex sample
     * from each polyphase channelizer output results array.
     *
     * @param sampleRate of the output sample stream.
     * @param channelIndexes containing a single channel index.
     */
    public OneChannelOutputProcessor(double sampleRate, List<Integer> channelIndexes)
    {
        super(1, sampleRate);
        setPolyphaseChannelIndices(channelIndexes);
        mLog.debug("Sample Rate:" + sampleRate + " PolyPhase Channel Indexes:" + channelIndexes);
    }

    /**
     * Updates this processor to extract a single, specified channel index.
     * @param indexes containing a single channel index value.
     * @throws IllegalArgumentException if the list of indexes does not contain a single channel index.
     */
    public void setPolyphaseChannelIndices(List<Integer> indexes)
    {
        if(indexes.size() != 1)
        {
            throw new IllegalArgumentException("Single channel output processor requires a single index to " +
                "process - provided indexes " + indexes.toString());
        }

        //Set the channelized output results offset to twice the channel index to account for each channel having
        //an I/Q pair
        mChannelOffset = indexes.get(0) * 2;
    }

    /**
     * Extract the channel from the channel results array, apply frequency translation, and deliver the
     * extracted frequency-corrected channel I/Q sample set to the complex sample listener.
     *
     * @param channelResults to process containing a list of channel array of I/Q sample pairs (I0,Q0,I1,Q1...In,Qn)
     * @param timestampedBufferAssembler to receive the extracted, frequency-translated channel results
     */
    @Override
    public void process(List<PolyphaseChannelResultsBuffer> channelResults,
                        TimestampedBufferAssembler timestampedBufferAssembler)
    {
        for(PolyphaseChannelResultsBuffer channelResultsBuffer: channelResults)
        {
            try
            {
                float[] samples = channelResultsBuffer.getChannel(mChannelOffset);

                if(hasFrequencyCorrection())
                {
                    getFrequencyCorrectionMixer().mixComplex(samples);
                }

                timestampedBufferAssembler.receive(samples);
            }
            catch(IllegalArgumentException iae)
            {
                mLog.error("Error extracting channel samples from polyphase channel results buffer");
            }
        }
    }
}
