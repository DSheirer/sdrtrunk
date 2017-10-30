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

public abstract class ChannelOutputProcessor implements IPolyphaseChannelOutputProcessor
{
    private int mInputChannelCount;
    private Oscillator mFrequencyCorrectionMixer;

    /**
     * Base class for polyphase channelizer output channel processing.  Provides built-in frequency translation
     * oscillator support to apply frequency correction to the channel sample stream as requested by sample consumer.
     *
     * @param inputChannelCount is the number of input channels for this output processor
     * @param sampleRate of the output channel.  This is used to match the oscillator's sample rate to the output
     * channel sample rate for frequency translation/correction.
     */
    public ChannelOutputProcessor(int inputChannelCount, int sampleRate)
    {
        mInputChannelCount = inputChannelCount;
        mFrequencyCorrectionMixer = new Oscillator(0, sampleRate);
    }

    @Override
    public int getInputChannelCount()
    {
        return mInputChannelCount;
    }

    /**
     * Provides frequency correction of the inphase component or simply returns the inphase argument value when no
     * frequency correction value has been specified.
     *
     * @param inphase value of the sample to translate/correct
     * @param quadrature value of the sample to translate/correct
     * @return corrected inphase value
     */
    protected float getFrequencyCorrectedInphase(float inphase, float quadrature)
    {
        //Only apply frequency correction if the oscillator is set to a non-zero frequency offset
        if(mFrequencyCorrectionMixer.enabled())
        {
            return Complex.multiplyInphase(inphase, quadrature, mFrequencyCorrectionMixer.inphase(),
                mFrequencyCorrectionMixer.quadrature());
        }
        else
        {
            return inphase;
        }
    }

    /**
     * Provides frequency correction of the quadrature component or simply returns the quadrature argument value when no
     * frequency correction value has been applied,
     *
     * @param inphase value of the sample to translate/correct
     * @param quadrature value of the sample to translate/correct
     * @return translated inphase value
     */
    protected float getFrequencyCorrectedQuadrature(float inphase, float quadrature)
    {
        //Only apply frequency correction if the oscillator is set to a non-zero frequency offset
        if(mFrequencyCorrectionMixer.enabled())
        {
            return Complex.multiplyQuadrature(inphase, quadrature, mFrequencyCorrectionMixer.inphase(),
                mFrequencyCorrectionMixer.quadrature());
        }
        else
        {
            return quadrature;
        }
    }

    /**
     * Specifies a frequency correction value to be applied to channel samples output from this polyphase channelizer
     * output processor.
     *
     * @param frequencyCorrection offset value
     */
    @Override
    public void setFrequencyCorrection(long frequencyCorrection)
    {
        mFrequencyCorrectionMixer.setFrequency(frequencyCorrection);
    }

}
