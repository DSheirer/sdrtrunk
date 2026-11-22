/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TwoChannelOutputProcessor extends ChannelOutputProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(TwoChannelOutputProcessor.class);
    private TwoChannelMixerAssembler mMixerAssembler;

    /**
     * Processor to extract two channels from a polyphase channelizer, synthesize/recombine the channels, apply
     * frequency translation and frequency correction, down-sample by a factor of two and output an I/Q complex sample.
     *
     * @param sampleRate of the output sample stream.
     * @param channelIndexes containing two channel indices.
     * @param filter to use
     * @param gain to apply to output.  Typically this is equal to the channelizer's channel count.
     * @param heartbeatManager to be pinged on the dispatcher thread
     * @param threadName for the dispatcher
     */
    public TwoChannelOutputProcessor(double sampleRate, List<Integer> channelIndexes, float[] filter, float gain,
                                     HeartbeatManager heartbeatManager, String threadName)
    {
        //Set the frequency correction oscillator to 2 x output sample rate since we'll be correcting the frequency
        //after synthesizing both input channels
        super(2, heartbeatManager, threadName);
        setPolyphaseChannelIndices(channelIndexes);
        mMixerAssembler = new TwoChannelMixerAssembler(gain);
        mMixerAssembler.getMixer().setSampleRate(sampleRate);
        mMixerAssembler.setSynthesisFilter(filter);
    }

    @Override
    public String getStateDescription()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Two Channel Output Processor");
        sb.append("\n\tIndices (doubled) ").append(Arrays.toString(getChannelOffsets()));
        sb.append("\n\tMixer Assembler: ").append(mMixerAssembler.getStateDescription());
        return sb.toString();
    }

    /**
     * Sets the frequency offset to apply to the incoming samples to mix the desired signal to baseband.
     *
     * @param frequencyOffset in hertz
     */
    @Override
    public void setFrequencyOffset(long frequencyOffset)
    {
        enqueueUpdate(() -> mMixerAssembler.getMixer().setFrequency(frequencyOffset));
    }

    @Override
    public void setSynthesisFilter(float[] filter)
    {
        enqueueUpdate(() -> mMixerAssembler.setSynthesisFilter(filter));
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
                "process - provided indexes " + indexes);
        }

        //Set the channelized output results offsets to twice the channel index to account for each channel having
        //an I/Q pair
        setChannelOffsets(indexes.get(0) * 2, indexes.get(1) * 2);
    }

    /**
     * Synthesizes the two copied channels, applies frequency translation, and delivers the frequency-corrected channel
     * I/Q sample set to the complex sample listener.
     *
     * @param samples containing an I/Q pair for each of the two channels per block (I1,Q1,I2,Q2 ...)
     * @param blockCount number of blocks
     */
    @Override
    public void process(float[] samples, int blockCount)
    {
        //Translate channel 2 up by FS/2 (multiply by (-1)^n) so that it sits adjacent to channel 1 for synthesis.  The
        //channelizer dispatches even-sized batches, so the index within the batch carries the absolute block parity.
        for(int x = 0; x < blockCount; x++)
        {
            int block = x * 4;

            if((x & 1) == 0)
            {
                mMixerAssembler.receive(samples[block], samples[block + 1], samples[block + 2], samples[block + 3]);
            }
            else
            {
                mMixerAssembler.receive(samples[block], samples[block + 1], -samples[block + 2], -samples[block + 3]);
            }

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
                        mLog.error("Error extracting channel samples from two polyphase channel results buffer", e);
                    }
                }
            }
        }
    }
}
