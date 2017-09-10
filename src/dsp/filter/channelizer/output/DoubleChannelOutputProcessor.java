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

import java.util.List;

public class DoubleChannelOutputProcessor extends ChannelOutputProcessor
{
    private int mChannelOffset1;
    private int mChannelOffset2;

    /**
     * Processor to extract two channels from a polyphase channelizer, synthesize/recombine the channels, apply
     * frequency translation and frequency correction, down-sample by a factor of two and output an I/Q complex sample.
     *
     * @param sampleRate of the output sample stream.
     * @param channelIndexes containing two channel indices.
     */
    public DoubleChannelOutputProcessor(int sampleRate, List<Integer> channelIndexes)
    {
        super(sampleRate);
        setPolyphaseChannelIndices(channelIndexes);
    }

    /**
     * Updates this processor to extract the two specified channel indexes.
     *
     * @param indexes containing a single channel index value.
     * @throws IllegalArgumentException if the list of indexes does not contain a single channel index.
     */
    public void setPolyphaseChannelIndices(List<Integer> indexes)
    {
        if(indexes.size() != 2)
        {
            throw new IllegalArgumentException("Double channel output processor requires two indexes to " +
                "process - provided indexes " + indexes.toString());
        }

        //Set the channelized output results offsets to twice the channel index to account for each channel having
        //an I/Q pair
        mChannelOffset1 = indexes.get(0) * 2;
        mChannelOffset2 = indexes.get(1) * 2;
    }

    /**
     * Extract the channel from the channel results array, apply frequency translation, and deliver the
     * extracted frequency-corrected channel I/Q sample set to the complex sample listener.
     *
     * @param channels to process containing an array of channel I/Q sample pairs (I0,Q0,I1,Q1...In,Qn)
     * @param listener to receive the extracted, frequency-translated channel results
     */
    @Override
    public void process(float[] channels, IComplexSampleListener listener)
    {
//        if(channels.length < mChannelOffset + 1)
//        {
//            throw new IllegalArgumentException("Polyphase channelizer output channels array is not large enough to " +
//                "cover this single channel output processor with channel offset [" + mChannelOffset + "]");
//        }
//
//        float i = channels[mChannelOffset];
//        float q = channels[mChannelOffset + 1];

        listener.receive(getFrequencyCorrectedInphase(i, q), getFrequencyCorrectedQuadrature(i, q));
    }
}
