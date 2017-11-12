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
package source.tuner.test;

import dsp.filter.channelizer.PolyphaseChannelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;
import sample.complex.ComplexBuffer;
import source.Source;
import source.SourceException;
import source.tuner.Tuner;
import source.tuner.TunerChannel;
import source.tuner.TunerChannelSource;
import source.tuner.TunerClass;
import source.tuner.TunerType;

import java.util.concurrent.RejectedExecutionException;

/**
 * Testing tuner that implements an internal oscillator to output a unity gain tone at a specified frequency offset
 * from a configurable center tune frequency and sample rate
 */
public class TestTuner extends Tuner
{
    private final static Logger mLog = LoggerFactory.getLogger(TestTuner.class);
    private static int mInstanceCounter = 1;
    private final int mInstanceID = mInstanceCounter++;

    private PolyphaseChannelManager mPolyphaseChannelManager;

    public TestTuner()
    {
        super("Test Tuner", new TestTunerController());

        mLog.debug("Initializing test tuner.  Tuner Controller's sample rate is:" + getTunerController().getSampleRate());
        mLog.debug("Initializing test tuner.  Test Tuner Controller's sample rate is:" + getTestTunerController().getSampleRate());

        mPolyphaseChannelManager = new PolyphaseChannelManager(this);
    }

    /**
     * Returns the tuner controller cast as a test tuner controller.
     *
     * Note: this is a temporary method until the tuner controller interface can be updated with the start(listener)
     * and stop() methods.
     */
    private TestTunerController getTestTunerController()
    {
        return (TestTunerController)getTunerController();
    }

    /**
     * Sets the center tuned frequency for this tuner
     * @param frequency
     * @throws SourceException
     */
    public void setFrequency(long frequency) throws SourceException
    {
        getTunerController().setFrequency(frequency);
    }

    public void setToneFrequency(long frequency)
    {
        getTestTunerController().setToneFrequency(frequency);
    }

    public long getToneFrequency()
    {
        return getTestTunerController().getToneFrequency();
    }

    public void setSampleRate(int sampleRate) throws SourceException
    {
        getTestTunerController().setSampleRate(sampleRate);
    }

    public int getSampleRate()
    {
        return getTestTunerController().getSampleRate();
    }

    @Override
    public String getUniqueID()
    {
        return getName() + "-" + mInstanceID;
    }

    @Override
    public TunerClass getTunerClass()
    {
        return TunerClass.TEST_TUNER;
    }

    @Override
    public TunerType getTunerType()
    {
        return TunerClass.TEST_TUNER.getTunerType();
    }

    @Override
    public double getSampleSize()
    {
        return 16.0;
    }

    /**
     * Creates a polyphase channel source.  The provided channel is a one-use channel that can be commanded to start
     * and stop processing via the source event interface.  Once stopped, the channel is deregistered from this tuner
     * and is no longer usable -- a new channel source must be requested from the tuner.
     *
     * @param tunerChannel identifying the channel center frequency and bandwidth
     * @return channel source or null if the channel source cannot be provided by this tuner
     */
    public Source getChannelSource(TunerChannel tunerChannel)
    {
        return mPolyphaseChannelManager.getChannel(tunerChannel);
    }

    @Override
    public TunerChannelSource getChannel(TunerChannel channel) throws RejectedExecutionException, SourceException
    {
        mLog.error("This method is deprecated in this test tuner implementation");
        throw new IllegalStateException("Method deprecated - this test tuner cannot provide a TunerChannelSource");
    }

    @Override
    public void releaseChannel(TunerChannelSource source)
    {
        mLog.error("This method is deprecated in this test tuner implementation");
    }

    /**
     * Adds the listener to receive complex sample buffers and auto-starts the sample stream if this is the first
     * registered listener.
     */
    @Override
    public void addListener(Listener<ComplexBuffer> listener)
    {
        boolean isRunning = hasListeners();

        super.addListener(listener);

        if(!isRunning)
        {
            getTestTunerController().start(getSampleBroadcaster());
        }
    }

    /**
     * Removes the listener from receiving complex sample buffers and auto-stops the sample stream when there are
     * no more listeners.
     */
    @Override
    public void removeListener(Listener<ComplexBuffer> listener)
    {
        super.removeListener(listener);

        if(!hasListeners())
        {
            getTestTunerController().stop();
        }
    }
}
