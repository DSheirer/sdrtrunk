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
package io.github.dsheirer.dsp.filter.channelizer.ols;

import io.github.dsheirer.buffer.FloatNativeBuffer;
import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.controller.channel.event.ChannelStopProcessingRequest;
import io.github.dsheirer.dsp.fft.ComplexFFTFactory;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.log.LoggingSuppressor;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import io.github.dsheirer.util.Dispatcher;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Overlap-save channel manager: provides overlap-save tuner channel sources for a tuner, and runs the channelizer on
 * the tuner's sample buffers.  The buffer processor thread converts each native buffer, runs the forward FFT for each
 * block, and hands each channel its bins (OverlapSaveChannelSource), which the channel processes on its own thread.
 *
 * Tuner center frequency changes are applied to every channel at the next block boundary.  The sample rate can only
 * change while no channels are sourced.
 *
 * Note: add this manager as a source event listener to the tuner controller so that it receives frequency and sample
 * rate changes.
 *
 * See the package documentation (package-info.java) for the theory of operation and references.
 */
public class OverlapSaveChannelManager implements ISourceEventProcessor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(OverlapSaveChannelManager.class);
    private static final LoggingSuppressor LOGGING_SUPPRESSOR = new LoggingSuppressor(LOGGER);
    /**
     * Maximum duration of queued native buffers awaiting the channelizer, as for the polyphase channel manager.
     */
    private static final double MAXIMUM_QUEUED_SECONDS = 1.0;

    private final Broadcaster<SourceEvent> mSourceEventBroadcaster = new Broadcaster<>();
    private final TunerController mTunerController;
    private final List<OverlapSaveChannelSource> mChannelSources = new CopyOnWriteArrayList<>();
    private final ChannelSourceEventListener mChannelSourceEventListener = new ChannelSourceEventListener();
    private final Dispatcher<INativeBuffer> mBufferDispatcher;
    private final AtomicBoolean mFrequencyUpdateRequired = new AtomicBoolean();
    private final Object mLock = new Object();
    private final Object mProcessLock = new Object();
    private volatile OverlapSaveChannelizer mChannelizer;
    private volatile long mCenterFrequency;
    private volatile double mSampleRate;
    private volatile boolean mRunning = true;

    /**
     * Constructs an instance
     * @param tunerController providing the sample buffers
     */
    public OverlapSaveChannelManager(TunerController tunerController)
    {
        mTunerController = tunerController;
        mCenterFrequency = tunerController.getFrequency();
        mSampleRate = tunerController.getSampleRate();
        mBufferDispatcher = new Dispatcher<>("sdrtrunk overlap-save buffer processor", 10, INativeBuffer::sampleCount,
            getMaximumQueuedSamples(mSampleRate));
        mBufferDispatcher.setListener(this::process);
    }

    private static long getMaximumQueuedSamples(double sampleRate)
    {
        return Math.max(1, (long)(sampleRate * MAXIMUM_QUEUED_SECONDS));
    }

    /**
     * Indicates if the tuner's current sample rate is supported.
     */
    public boolean isSupportedSampleRate()
    {
        return OverlapSaveChannelizer.isSupported(mSampleRate);
    }

    /**
     * Current tuner sample rate.
     */
    public double getSampleRate()
    {
        return mSampleRate;
    }

    /**
     * Provides a channel source for the tuner channel, or null if it can't be provided.  The tuner must already be
     * tuned so that the channel fits its bandwidth.
     * @param tunerChannel to source
     * @param channelSpecification of the consumer
     * @param threadName for the channel's dispatcher
     * @return source or null
     */
    public TunerChannelSource getChannel(TunerChannel tunerChannel, ChannelSpecification channelSpecification,
                                         String threadName)
    {
        if(!mRunning)
        {
            return null;
        }

        synchronized(mLock)
        {
            try
            {
                OverlapSaveChannelizer channelizer = getChannelizer();
                OverlapSaveChannelSource source = new OverlapSaveChannelSource(mChannelSourceEventListener,
                    tunerChannel, channelSpecification, channelizer, mTunerController.getFrequency(), threadName,
                    mTunerController.getTunerFrequencyErrorManager());
                mChannelSources.add(source);
                return source;
            }
            catch(IllegalArgumentException iae)
            {
                LOGGING_SUPPRESSOR.error(iae.getMessage(), 3, "Couldn't allocate channel. " + iae.getMessage());
                return null;
            }
        }
    }

    /**
     * Channelizer for the current sample rate, created when needed.
     * @throws IllegalArgumentException if the sample rate isn't supported
     * @throws IllegalStateException if the sample rate changed while channels are sourced
     */
    private OverlapSaveChannelizer getChannelizer()
    {
        double sampleRate = mSampleRate;
        OverlapSaveChannelizer channelizer = mChannelizer;

        if(channelizer == null || Math.abs(channelizer.getSampleRate() - sampleRate) > 0.5)
        {
            if(channelizer != null && channelizer.getChannelCount() > 0)
            {
                throw new IllegalStateException("Overlap-save channelizer can't change sample rate while channels " +
                    "are sourced");
            }

            if(!OverlapSaveChannelizer.isSupported(sampleRate))
            {
                throw new IllegalArgumentException("Overlap-save channelizer doesn't support sample rate [" +
                    sampleRate + "]");
            }

            int size = OverlapSaveChannelizer.getDefaultFFTSize(sampleRate);
            channelizer = OverlapSaveChannelizer.create(sampleRate, ComplexFFTFactory.getComplexFFT(size));
            channelizer.setBlockBoundaryTask(this::applyFrequencyUpdate);
            LOGGER.info("Overlap-save channelizer for sample rate [" + sampleRate + "] FFT size [" + size + "] hop [" +
                channelizer.getHop() + "] using " + channelizer.getFFT().getClass().getSimpleName());
            mChannelizer = channelizer;
        }

        return channelizer;
    }

    /**
     * Applies a tuner center frequency change to every channel.  Runs on the buffer processor thread at a block
     * boundary.
     */
    private void applyFrequencyUpdate()
    {
        if(mFrequencyUpdateRequired.compareAndSet(true, false))
        {
            long frequency = mCenterFrequency;

            for(OverlapSaveChannelSource source : mChannelSources)
            {
                source.updateTunerFrequency(frequency);
            }
        }
    }

    /**
     * Converts the native buffer and runs the channelizer.  Runs on the buffer processor thread.
     */
    private void process(INativeBuffer nativeBuffer)
    {
        //The channelizer is single-threaded.  The lock keeps a buffer still in process on a stopping dispatcher thread
        //from overlapping the restarted dispatcher's thread or the reset at a restart.  It is uncontended otherwise.
        synchronized(mProcessLock)
        {
            OverlapSaveChannelizer channelizer = mChannelizer;

            if(channelizer != null)
            {
                receive(channelizer, nativeBuffer);
            }
        }
    }

    /**
     * Runs the channelizer on a native buffer.  A float buffer's samples go to the channelizer in place, since it
     * copies them into its own block without keeping a reference.  Other buffers are converted in chunks.
     * @param channelizer to receive the samples
     * @param nativeBuffer of samples
     */
    static void receive(OverlapSaveChannelizer channelizer, INativeBuffer nativeBuffer)
    {
        if(nativeBuffer instanceof FloatNativeBuffer floatBuffer)
        {
            try
            {
                channelizer.receive(floatBuffer.getInterleavedSamples(), 0, floatBuffer.sampleCount(),
                    floatBuffer.getTimestamp());
            }
            catch(Throwable throwable)
            {
                LOGGER.error("Error", throwable);
            }

            return;
        }

        Iterator<InterleavedComplexSamples> iterator = nativeBuffer.iteratorInterleaved();

        while(iterator.hasNext())
        {
            try
            {
                InterleavedComplexSamples samples = iterator.next();
                channelizer.receive(samples.samples(), samples.timestamp());
            }
            catch(Throwable throwable)
            {
                LOGGER.error("Error", throwable);
            }
        }
    }

    private void startChannelSource(OverlapSaveChannelSource source)
    {
        synchronized(mLock)
        {
            OverlapSaveChannelizer channelizer;

            try
            {
                channelizer = getChannelizer();
            }
            catch(IllegalArgumentException | IllegalStateException e)
            {
                failChannelSource(source, e.getMessage());
                return;
            }

            //The source's channel was designed for the channelizer that existed when the source was created.  If the
            //tuner sample rate changed since, the source can't use this channelizer.
            if(Math.abs(channelizer.getSampleRate() - source.getTunerSampleRate()) > 0.5)
            {
                failChannelSource(source, "Tuner sample rate changed from [" + source.getTunerSampleRate() +
                    "] to [" + channelizer.getSampleRate() + "] before the channel started");
                return;
            }

            if(channelizer.getChannelCount() == 0)
            {
                //A restart of the sample stream, or the first start
                synchronized(mProcessLock)
                {
                    channelizer.reset();
                }
            }

            //Bring the channel up to date with any frequency change that's still pending, which can fail the source
            source.updateTunerFrequency(mCenterFrequency);

            if(source.isFailed())
            {
                return;
            }

            channelizer.addChannel(source);
            mSourceEventBroadcaster.broadcast(SourceEvent.channelCountChange(getTunerChannelCount()));

            if(channelizer.getChannelCount() == 1)
            {
                mBufferDispatcher.start();
                mTunerController.addBufferListener(mBufferDispatcher);
            }
        }
    }

    /**
     * Fails a source that can't start.
     */
    private void failChannelSource(OverlapSaveChannelSource source, String message)
    {
        source.fail("Overlap-save channel [" + source.getTunerChannel().getFrequency() + "] can't start - " + message);
    }

    private void stopChannelSource(OverlapSaveChannelSource source)
    {
        synchronized(mLock)
        {
            mChannelSources.remove(source);
            OverlapSaveChannelizer channelizer = mChannelizer;

            if(channelizer != null)
            {
                channelizer.removeChannel(source);
            }

            mSourceEventBroadcaster.broadcast(SourceEvent.channelCountChange(getTunerChannelCount()));

            if(channelizer != null && channelizer.getChannelCount() == 0)
            {
                mTunerController.removeBufferListener(mBufferDispatcher);
                mBufferDispatcher.stop();
            }
        }

        try
        {
            //Broadcast a stop sample stream notification in case this was a forced-stop so consumers are aware
            source.process(SourceEvent.stopSampleStreamNotification(source));
        }
        catch(SourceException se)
        {
            //Do nothing
        }
    }

    /**
     * Processes frequency and sample rate change notifications from the tuner controller.
     */
    @Override
    public void process(SourceEvent sourceEvent) throws SourceException
    {
        switch(sourceEvent.getEvent())
        {
            case NOTIFICATION_FREQUENCY_CHANGE:
                mCenterFrequency = sourceEvent.getValue().longValue();
                mFrequencyUpdateRequired.set(true);
                break;
            case NOTIFICATION_SAMPLE_RATE_CHANGE:
                mSampleRate = sourceEvent.getValue().doubleValue();
                mBufferDispatcher.setMaximumQueuedWeight(getMaximumQueuedSamples(mSampleRate));
                break;
            case NOTIFICATION_FREQUENCY_CORRECTION_CHANGE:
                //Re-broadcast this event to each channel so that the decoders can reset error tracking function(s).
                for(TunerChannelSource channel : new ArrayList<>(mChannelSources))
                {
                    channel.broadcastConsumerSourceEvent(sourceEvent);
                }
                break;
            case NOTIFICATION_FREQUENCY_AND_SAMPLE_RATE_LOCKED:
            case NOTIFICATION_FREQUENCY_AND_SAMPLE_RATE_UNLOCKED:
            case NOTIFICATION_RECORDING_FILE_LOADED:
            case NOTIFICATION_MEASURED_FREQUENCY_ERROR:
                break;
            default:
                LOGGER.info("Unrecognized source event: " + sourceEvent);
                break;
        }
    }

    /**
     * Requests that every channel stop processing, to prepare for tuner shutdown.
     */
    public void stopAllChannels()
    {
        mRunning = false;

        for(TunerChannelSource source : new ArrayList<>(mChannelSources))
        {
            MyEventBus.getGlobalEventBus().post(new ChannelStopProcessingRequest(source));
        }
    }

    /**
     * Signals the tuner error state to each channel.
     */
    public void setErrorMessage(String errorMessage)
    {
        for(TunerChannelSource source : mChannelSources)
        {
            source.setError(errorMessage);
        }
    }

    /**
     * Sorted set of currently sourced tuner channels.
     */
    public SortedSet<TunerChannel> getTunerChannels()
    {
        SortedSet<TunerChannel> tunerChannels = new TreeSet<>();

        for(OverlapSaveChannelSource source : mChannelSources)
        {
            tunerChannels.add(source.getTunerChannel());
        }

        return tunerChannels;
    }

    public int getTunerChannelCount()
    {
        return mChannelSources.size();
    }

    public String getStateDescription()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Overlap-Save Channel Manager Providing [").append(mChannelSources.size()).append("] Channels");
        OverlapSaveChannelizer channelizer = mChannelizer;

        if(channelizer != null)
        {
            sb.append(" FFT size [").append(channelizer.getFFTSize()).append("] hop [").append(channelizer.getHop())
                .append("] blocks [").append(channelizer.getBlockCount()).append("]");
        }

        for(OverlapSaveChannelSource source : mChannelSources)
        {
            sb.append("\n\t").append(source.getStateDescription());
        }

        return sb.toString();
    }

    public void addSourceEventListener(Listener<SourceEvent> listener)
    {
        mSourceEventBroadcaster.addListener(listener);
    }

    public void removeSourceEventListener(Listener<SourceEvent> listener)
    {
        mSourceEventBroadcaster.removeListener(listener);
    }

    /**
     * Handles start and stop sample stream requests from the channel sources.
     */
    private class ChannelSourceEventListener implements Listener<SourceEvent>
    {
        @Override
        public void receive(SourceEvent sourceEvent)
        {
            switch(sourceEvent.getEvent())
            {
                case REQUEST_START_SAMPLE_STREAM:
                    if(sourceEvent.getSource() instanceof OverlapSaveChannelSource source)
                    {
                        startChannelSource(source);
                    }
                    break;
                case REQUEST_STOP_SAMPLE_STREAM:
                    if(sourceEvent.getSource() instanceof OverlapSaveChannelSource source)
                    {
                        stopChannelSource(source);
                        source.dispose();
                    }
                    break;
                case REQUEST_FREQUENCY_CORRECTION:
                case NOTIFICATION_MEASURED_FREQUENCY_ERROR:
                    break;
                default:
                    LOGGER.error("Received unrecognized source event from overlap-save channel source [" +
                        sourceEvent.getEvent() + "]");
                    break;
            }
        }
    }
}
