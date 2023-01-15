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

import io.github.dsheirer.dsp.filter.channelizer.TwoChannelSynthesizerM2;
import io.github.dsheirer.dsp.oscillator.FS4DownConverter;
import io.github.dsheirer.sample.complex.ComplexSamples;

/**
 * Sample assembler to extract a single channel from the channel results array of a polyphase channel results array.
 */
public class TwoChannelMixerAssembler extends MixerAssembler
{
    private SampleAssembler mChannel1SampleAssembler = new SampleAssembler(BUFFER_SIZE);
    private SampleAssembler mChannel2SampleAssembler = new SampleAssembler(BUFFER_SIZE);
    private FS4DownConverter mFS4DownConverter = new FS4DownConverter();
    private TwoChannelSynthesizerM2 mTwoChannelSynthesizer;

    /**
     * Constructs an instance
     * @param gain to apply to fully assembled buffers.
     */
    public TwoChannelMixerAssembler(float gain)
    {
        super(gain);
    }

    /**
     * Description of the state/configuration of this mixer assembler.
     */
    public String getStateDescription()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Two Channel Mixer Assembler - State Description:");
        sb.append("\n\tTwo-Channel Synthesizer: ").append(mTwoChannelSynthesizer.getStateDescription());
        sb.append("\n\tMixer Frequency: ").append(getMixer().getFrequency());
        sb.append("\n\tGain: ").append(getGain().getGain());
        return sb.toString();
    }

    /**
     * Sets the filter coefficients to use for two-channel synthesis
     * @param filter
     */
    public void setSynthesisFilter(float[] filter)
    {
        mTwoChannelSynthesizer = new TwoChannelSynthesizerM2(filter);
    }

    /**
     * Receive a complex I & Q sample pair.
     * @param iChannel1 inphase
     * @param qChannel1 quadrature
     * @param iChannel2 inphase
     * @param qChannel2 quadrature
     */
    public void receive(float iChannel1, float qChannel1, float iChannel2, float qChannel2)
    {
        mChannel1SampleAssembler.receive(iChannel1, qChannel1);
        mChannel2SampleAssembler.receive(iChannel2, qChannel2);
    }

    /**
     * Indicates if there is a fully assembled buffer ready.
     */
    @Override
    public boolean hasBuffer()
    {
        //Both channel 1 and channel 2 assemblers work in parallel, so we only need to check one of them.
        return mChannel1SampleAssembler.hasBuffer();
    }

    /**
     * Access the assembled buffer.  Frequency offset and gain are applied to the returned buffer.
     * @param timestamp to use for the assembled buffer
     * @return assembled buffer.
     */
    @Override
    public ComplexSamples getBuffer(long timestamp)
    {
        ComplexSamples channel1 = mChannel1SampleAssembler.getBufferAndReset(timestamp);
        ComplexSamples channel2 = mChannel2SampleAssembler.getBufferAndReset(timestamp);

        //Synthesize two channels into one, reducing sample rate by 2 so it's same as single channel rate
        ComplexSamples buffer = mTwoChannelSynthesizer.process(channel1, channel2);

        //Mix to baseband
        buffer = mFS4DownConverter.mixComplex(buffer);

        //Apply frequency correction if necessary
        if(getMixer().hasFrequency())
        {
            buffer = getMixer().mix(buffer);
        }

        //Apply gain
        buffer = getGain().apply(buffer);

        return buffer;
    }
}
