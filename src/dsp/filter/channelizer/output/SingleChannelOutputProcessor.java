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

import dsp.mixer.Oscillator;
import sample.complex.Complex;
import sample.complex.IComplexSampleListener;

import java.util.List;

public class SingleChannelOutputProcessor implements IPolyphaseChannelOutputProcessor
{
    private int mChannelOffset;
    private Oscillator mFrequencyCorrectionMixer;

    /**
     * Processor to extract a single channel from a polyphase channelizer and produce an output I/Q complex sample
     * from each polyphase channelizer output results array.
     *
     * @param sampleRate of the output sample stream.
     * @param channelIndexes containing a single channel index.
     */
    public SingleChannelOutputProcessor(int sampleRate, List<Integer> channelIndexes)
    {
        mFrequencyCorrectionMixer = new Oscillator(0, sampleRate);
        setPolyphaseChannelIndices(channelIndexes);
    }

    /**
     * Updates this processor to extract a specified channel index.
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
     * Extracts the channel from the channel results array, applies frequency translation, and delivers the
     * extracted frequency-translated channel sample to the complex sample listener.
     *
     * @param channels to process containing an array of channel I/Q sample pairs (I0,Q0,I1,Q1...In,Qn)
     * @param listener to receive the extracted, frequency-translated channel results
     */
    @Override
    public void process(float[] channels, IComplexSampleListener listener)
    {
        if(channels.length >= (mChannelOffset + 1))
        {
            //Only perform frequency translation if the frequency correction mixer has a non-zero frequency value
            if(mFrequencyCorrectionMixer.isEnabled())
            {
                mFrequencyCorrectionMixer.rotate();

                float i = Complex.multiplyInphase(channels[mChannelOffset], channels[mChannelOffset + 1],
                    mFrequencyCorrectionMixer.inphase(), mFrequencyCorrectionMixer.quadrature());

                float q = Complex.multiplyQuadrature(channels[mChannelOffset], channels[mChannelOffset + 1],
                    mFrequencyCorrectionMixer.inphase(), mFrequencyCorrectionMixer.quadrature());

                listener.receive(i, q);
            }
            else
            {
                listener.receive(channels[mChannelOffset], channels[mChannelOffset + 1]);
            }
        }
    }

    /**
     * Specifies a frequency correction/translation value to be applied to channel samples output from the
     * polyphase channelizer.
     * @param frequency
     */
    @Override
    public void setFrequencyCorrection(long frequency)
    {
        mFrequencyCorrectionMixer.setFrequency(frequency);
    }
}
