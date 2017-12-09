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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.OverflowableTransferQueue;
import sample.complex.Complex;
import sample.complex.ComplexSampleListener;
import sample.real.IOverflowListener;
import source.Source;

import java.util.ArrayList;
import java.util.List;

public abstract class ChannelOutputProcessor implements IPolyphaseChannelOutputProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelOutputProcessor.class);

    private OverflowableTransferQueue<float[]> mChannelResultsQueue;
    private List<float[]> mChannelResultsToProcess = new ArrayList<>();
    private int mMaxResultsToProcess;

    private int mInputChannelCount;
//TODO: swap this out and use the LowPhaseNoiseOscillator
    private Oscillator mFrequencyCorrectionMixer;
    private boolean mFrequencyCorrectionEnabled;

    /**
     * Base class for polyphase channelizer output channel processing.  Provides built-in frequency translation
     * oscillator support to apply frequency correction to the channel sample stream as requested by sample consumer.
     *
     * @param inputChannelCount is the number of input channels for this output processor
     * @param sampleRate of the output channel.  This is used to match the oscillator's sample rate to the output
     * channel sample rate for frequency translation/correction.
     */
    public ChannelOutputProcessor(int inputChannelCount, double sampleRate)
    {
        mInputChannelCount = inputChannelCount;
        mFrequencyCorrectionMixer = new Oscillator(0, sampleRate);
        mMaxResultsToProcess = (int)(sampleRate / 10) * 2;  //process at 100 millis interval, twice the expected inflow rate

        mChannelResultsQueue = new OverflowableTransferQueue<>((int)(sampleRate * 3), (int)(sampleRate * 0.5));
    }

    @Override
    public void setFrequency(long frequency)
    {

    }

    @Override
    public long getFrequencyCorrection()
    {
        return 0;
    }

    @Override
    public void receiveChannelResults(float[] channelResults)
    {
        mChannelResultsQueue.offer(channelResults);
    }

    /**
     * Processes all enqueued polyphase channelizer results until the internal queue is empty
     * @param listener to receive the processed channel results
     */
    @Override
    public void processChannelResults(ComplexSampleListener listener)
    {
        try
        {
            int count = mChannelResultsQueue.drainTo(mChannelResultsToProcess, mMaxResultsToProcess);

            if(count > 0)
            {
                process(mChannelResultsToProcess, listener);
            }
        }
        catch(Throwable throwable)
        {
            mLog.error("Error while processing polyphase channel samples", throwable);
        }

        mChannelResultsToProcess.clear();
    }

    /**
     * Sub-class implementation to process one polyphase channelizer result array.
     * @param channelResults to process
     * @param listener to receive the channel complex sample
     */
    public abstract void process(List<float[]> channelResults, ComplexSampleListener listener);


    /**
     * Sets the overflow listener to monitor the internal channelizer channel results queue overflow state
     */
    public void setSourceOverflowListener(Source source)
    {
        mChannelResultsQueue.setSourceOverflowListener(source);
    }

    /**
     * Removes the overflow listener from monitoring the internal channelizer channel results queue overflow state
     */
    public void removeOverflowListener(IOverflowListener listener)
    {
        mChannelResultsQueue.setOverflowListener(null);
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
        return Complex.multiplyInphase(inphase, quadrature, mFrequencyCorrectionMixer.inphase(),
            mFrequencyCorrectionMixer.quadrature());
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
        return Complex.multiplyQuadrature(inphase, quadrature, mFrequencyCorrectionMixer.inphase(),
            mFrequencyCorrectionMixer.quadrature());
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

        mFrequencyCorrectionEnabled = (frequencyCorrection != 0);
    }

    /**
     * Indicates if this channel has a frequency correction offset
     */
    public boolean hasFrequencyCorrection()
    {
        return mFrequencyCorrectionEnabled;
    }

}
