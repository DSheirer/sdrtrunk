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

import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.heartbeat.HeartbeatManager;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OneChannelOutputProcessor extends ChannelOutputProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(OneChannelOutputProcessor.class);
    private final OneChannelMixerAssembler mMixerAssembler;
    private int mChannelOffset;

    /**
     * Processor to extract a single channel from a polyphase channelizer and produce an output I/Q complex sample
     * from each polyphase channelizer output results array.
     *
     * @param sampleRate of the output sample stream.
     * @param channelIndexes containing a single channel index.
     * @param gain value to apply.  This is typically the same as the channelizer's channel count.
     * @param heartbeatManager to receive heartbeats on the dispatch thread
     */
    public OneChannelOutputProcessor(double sampleRate, List<Integer> channelIndexes, float gain,
                                     HeartbeatManager heartbeatManager)
    {
        super(1, sampleRate, heartbeatManager);
        setPolyphaseChannelIndices(channelIndexes);
        mMixerAssembler = new OneChannelMixerAssembler(gain);
        mMixerAssembler.getMixer().setSampleRate(sampleRate);
    }

    @Override
    public String getStateDescription()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("One Channel Output Processor");
        sb.append("\n\tIndex (doubled) [").append(mChannelOffset).append("]");
        sb.append("\n\tMixer Assembler: none");
        return sb.toString();
    }

    @Override
    public void setSynthesisFilter(float[] filter)
    {
        throw new IllegalArgumentException("The one channel output processor does not support filter updates");
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
                "process - provided indexes " + indexes);
        }

        //Set the channelized output results offset to twice the channel index to account for each channel having
        //an I/Q pair
        mChannelOffset = indexes.get(0) * 2;
    }

    @Override
    public void setFrequencyOffset(long frequency)
    {
        mMixerAssembler.getMixer().setFrequency(frequency);
    }

    /**
     * Extract the channel from the channel results array and pass to the assembler.  The assembler will
     * apply frequency translation and gain and indicate when a buffer is fully assembled.
     *
     * @param channelResultsList to process containing a list of a list of channel array of I/Q sample pairs (I0,Q0,I1,Q1...In,Qn)
     */
    @Override
    public void process(List<float[]> channelResultsList)
    {
        for(float[] channelResults: channelResultsList)
        {
            mMixerAssembler.receive(channelResults[mChannelOffset], channelResults[mChannelOffset + 1]);

            if(mMixerAssembler.hasBuffer())
            {
                ComplexSamples buffer = mMixerAssembler.getBuffer(getCurrentSampleTimestamp());

                if(mComplexSamplesListener != null)
                {
                    try
                    {
                        mComplexSamplesListener.receive(buffer);
                    }
                    catch(NullPointerException npe)
                    {
                        //Ignore ... can happen when the listener is nullified on another thread
                    }
                    catch(Exception e)
                    {
                        mLog.error("Error extracting channel samples from one polyphase channel results buffer", e);
                    }
                }
            }
        }
    }
}
