/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.source.tuner.channel;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.source.Source;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.heartbeat.Heartbeat;
import io.github.dsheirer.source.tuner.TunerModel;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Multiple-frequency tuner channel source.  Provides a wrapper around a tuner channel source and listens for external
 * source events requests to change frequency.  Maintains an ordered list of frequencies and automatically tears down
 * an existing tuner channel source and obtains a new one with the next frequency from the list, on request.
 */
public class MultiFrequencyTunerChannelSource extends TunerChannelSource
{
    private final static Logger mLog = LoggerFactory.getLogger(MultiFrequencyTunerChannelSource.class);

    private TunerModel mTunerModel;
    private TunerChannelSource mTunerChannelSource;
    private List<Long> mFrequencies;
    private int mFrequencyListPointer = 0;
    private ChannelSpecification mChannelSpecification;
    private Listener<ReusableComplexBuffer> mReusableComplexBufferListener;
    private Listener<Heartbeat> mHeartbeatListener;
    private String mPreferredTuner;
    private AtomicBoolean mChangingChannels = new AtomicBoolean();
    private boolean mStarted;
    private ConsumerSourceEventAdapter mConsumerSourceEventAdapter = new ConsumerSourceEventAdapter();

    public MultiFrequencyTunerChannelSource(TunerModel tunerModel, TunerChannelSource tunerChannelSource,
                                            List<Long> frequencies, ChannelSpecification channelSpecification,
                                            String preferredTuner)
    {
        super(null, tunerChannelSource.getTunerChannel());
        mTunerModel = tunerModel;
        mTunerChannelSource = tunerChannelSource;
        mTunerChannelSource.setSourceEventListener(mConsumerSourceEventAdapter);
        mFrequencies = frequencies;
        mChannelSpecification = channelSpecification;
        mPreferredTuner = preferredTuner;
    }

    /**
     * Cycles this source to use the next frequency in the list
     */
    private void nextFrequency()
    {
        if(mChangingChannels.compareAndSet(false, true))
        {
            //Only change if we have more than one frequency specified
            if(mFrequencies.size() > 0)
            {
                if(mTunerChannelSource != null)
                {
                    //Shutdown the existing tuner channel source
                    mTunerChannelSource.stop();
                    mTunerChannelSource.removeListener(mReusableComplexBufferListener);
                    mTunerChannelSource.removeSourceEventListener();
                    mTunerChannelSource.removeHeartbeatListener(mHeartbeatListener);
                    mTunerChannelSource.dispose();
                    mTunerChannelSource = null;
                }

                //Request the next tuner channel source
                getNextSource();
            }
        }
    }

    /**
     * Persistently attempts to get the next tuner channel source using the next frequency in the list.  This method
     * should only be invoked by the nextFrequency() method that protects access via the mChangingChannels flag.
     *
     * Note: if this channel source is shutdown by the external consumer, the mStarted flag will be set to false and
     * the persistent attempts will stop.
     */
    private void getNextSource()
    {
        if(mStarted)
        {
            TunerChannel nextChannel = getNextChannel();
            Source source = mTunerModel.getSource(nextChannel, mChannelSpecification, mPreferredTuner);

            if(source instanceof TunerChannelSource)
            {
                mTunerChannelSource = (TunerChannelSource)source;
                mTunerChannelSource.setSourceEventListener(mConsumerSourceEventAdapter);
                mTunerChannelSource.setListener(mReusableComplexBufferListener);
                mTunerChannelSource.addHeartbeatListener(mHeartbeatListener);
                mTunerChannelSource.start();
                mChangingChannels.set(false);
                getSourceEventListener().receive(SourceEvent.frequencyRotationSuccessNotification(this, nextChannel.getFrequency()));
            }

            //If we don't get a channel source because a tuner is not available or none of the available tuners can
            //support the frequency, then persistently attempt to get a source by iterating the frequency list
            if(mTunerChannelSource == null)
            {
                getSourceEventListener().receive(SourceEvent.frequencyRotationFailureNotification(this, nextChannel.getFrequency()));
                ThreadPool.SCHEDULED.schedule(() -> getNextSource(), 1, TimeUnit.SECONDS);
            }
        }
    }

    @Override
    public void start()
    {
        //The initial source should not be null
        if(mTunerChannelSource != null)
        {
            mTunerChannelSource.start();
            mStarted = true;
        }
    }

    @Override
    public void stop()
    {
        mStarted = false;

        if(mTunerChannelSource != null)
        {
            mTunerChannelSource.stop();
            mTunerChannelSource.removeSourceEventListener();
        }
    }

    /**
     * Provides the next tuner channel from the frequency list.
     */
    private TunerChannel getNextChannel()
    {
        mFrequencyListPointer++;

        if(mFrequencyListPointer >= mFrequencies.size())
        {
            mFrequencyListPointer = 0;
        }

        long frequency = mFrequencies.get(mFrequencyListPointer);

        return new TunerChannel(frequency, mChannelSpecification.getBandwidth());
    }

    /**
     * Adds the heartbeat listener to receive heartbeats from the enclosed tuner channel source.
     */
    @Override
    public void addHeartbeatListener(Listener<Heartbeat> listener)
    {
        mHeartbeatListener = listener;

        if(mTunerChannelSource != null)
        {
            mTunerChannelSource.addHeartbeatListener(listener);
        }
    }

    /**
     * Removes the heartbeat listener from receiving heartbeats from the enclosed tuner channel source
     */
    @Override
    public void removeHeartbeatListener(Listener<Heartbeat> listener)
    {
        mHeartbeatListener = null;

        if(mTunerChannelSource != null)
        {
            mTunerChannelSource.removeHeartbeatListener(listener);
        }
    }

    @Override
    public void setFrequency(long frequency)
    {
        if(mTunerChannelSource != null)
        {
            mTunerChannelSource.setFrequency(frequency);
        }
    }

    @Override
    protected void setSampleRate(double sampleRate)
    {
        if(mTunerChannelSource != null)
        {
            mTunerChannelSource.setSampleRate(sampleRate);
        }
    }

    @Override
    protected void setChannelFrequencyCorrection(long correction)
    {
        if(mTunerChannelSource != null)
        {
            mTunerChannelSource.setChannelFrequencyCorrection(correction);
        }
    }

    @Override
    public long getChannelFrequencyCorrection()
    {
        if(mTunerChannelSource != null)
        {
            return mTunerChannelSource.getChannelFrequencyCorrection();
        }

        return 0;
    }

    @Override
    protected void processSamples()
    {
        if(mTunerChannelSource != null)
        {
            mTunerChannelSource.processSamples();
        }
    }

    @Override
    public void setListener(Listener<ReusableComplexBuffer> complexBufferListener)
    {
        mReusableComplexBufferListener = complexBufferListener;

        if(mTunerChannelSource != null)
        {
            mTunerChannelSource.setListener(complexBufferListener);
        }
    }

    @Override
    public void removeListener(Listener<ReusableComplexBuffer> listener)
    {
        if(mTunerChannelSource != null)
        {
            mTunerChannelSource.removeListener(listener);
        }

        mReusableComplexBufferListener = null;
    }

    @Override
    public double getSampleRate()
    {
        if(mTunerChannelSource != null)
        {
            return mTunerChannelSource.getSampleRate();
        }

        return 0;
    }

    /**
     * Overrides the parent method so that we can intercept external requests to change to the next frequency in the
     * frequency list.
     *
     * @param sourceEvent to process
     * @throws SourceException
     */
    @Override
    public void process(SourceEvent sourceEvent) throws SourceException
    {
        if(sourceEvent.getEvent() == SourceEvent.Event.REQUEST_FREQUENCY_ROTATION)
        {
            nextFrequency();
        }
        else if(mTunerChannelSource != null)
        {
            mTunerChannelSource.process(sourceEvent);
        }
    }

    /**
     * Listener for source events produced by the enclosed tuner channel source.  This adapter receives events from
     * the wrapped tuner channel source and rebroadcasts them as consumer source events from this enclosing multi-frequency
     * tuner channel source and to the registered external consumer source event listener.
     */
    public class ConsumerSourceEventAdapter implements Listener<SourceEvent>
    {
        @Override
        public void receive(SourceEvent sourceEvent)
        {
            broadcastConsumerSourceEvent(sourceEvent);
        }
    }
}
