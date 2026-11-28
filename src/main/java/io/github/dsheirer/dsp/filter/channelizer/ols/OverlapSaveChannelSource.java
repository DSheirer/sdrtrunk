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

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import io.github.dsheirer.source.tuner.frequency.TunerFrequencyErrorManager;
import io.github.dsheirer.util.Dispatcher;
import io.github.dsheirer.util.ThreadPool;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Overlap-save channelizer's tuner channel source.  On the channelizer's thread it copies its bins out of each block's
 * spectrum and queues them for its own dispatcher thread, which filters, inverse transforms and mixes them
 * (OverlapSaveChannel) and delivers one buffer per block to the consumer.  A slow consumer only drops its own blocks.
 *
 * When the consumer's channel specification has a channel filter (ChannelSpecification.getChannelFilter()) that the
 * channelizer can apply (getDecoderReadyOutputSize()), this source applies that filter and delivers the channel at a
 * rate that the decoder uses without decimating, normally the filter's own, and reports isChannelFiltered() so that the
 * decoder can skip its own baseband filter.  Otherwise it
 * delivers at least 50 kHz, the rate a polyphase channel source provides, with a wide channel filter, and the decoder
 * filters as usual.
 *
 * See the package documentation (package-info.java) for the theory of operation and references.
 */
public class OverlapSaveChannelSource extends TunerChannelSource implements OverlapSaveChannelizer.BlockListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(OverlapSaveChannelSource.class);
    public static final float ATTENUATION = 90.0f;
    /**
     * Minimum output sample rate for a channel without a channel filter, the same as the polyphase channel rate.
     */
    public static final double MINIMUM_SAMPLE_RATE = 50000.0;
    /**
     * Pass and stop band edges of the wide filter for a channel without a channel filter, relative to the output rate.
     */
    private static final double WIDE_FILTER_PASS = 0.3;
    private static final double WIDE_FILTER_STOP = 0.45;
    /**
     * Largest stop band gain of a decoder's filter, relative to its gain at 0 Hz, for a decoder-ready channel: -30 dB.
     * The decoders' own filters are about -40 dB or better wherever their design converges.
     */
    private static final double MINIMUM_STOP_BAND_GAIN = Math.pow(10.0, -30.0 / 20.0);
    /**
     * Lowest decoder-ready rate, relative to the decoder filter's own sample rate.
     */
    private static final double LOWEST_RATE = 0.8;
    /**
     * Maximum queued blocks, one second at the default 20 ms hop.
     */
    private static final int MAXIMUM_QUEUED_BLOCKS = 50;
    /**
     * Interval of the channel's dispatcher, the same as the polyphase channel output processor's.
     */
    private static final long DISPATCH_INTERVAL_MS = 50;

    private final OverlapSaveChannel mChannel;
    private final boolean mChannelFiltered;
    private final double mTunerSampleRate;
    private final Dispatcher<QueuedBlock> mDispatcher;
    /**
     * Blocks that the dispatcher thread has processed, for reuse on the channelizer thread.  Holds every block that can
     * be in flight: the queue, one being processed and the spare.
     */
    private final BlockPool mBlockPool = new BlockPool(MAXIMUM_QUEUED_BLOCKS + 2);
    /**
     * Block that wasn't queued because the dispatcher's queue was full, reused for the next block.  Channelizer thread
     * only.
     */
    private QueuedBlock mSpareBlock;
    private volatile Listener<ComplexSamples> mListener;
    private volatile long mFrequencyCorrection;
    private volatile long mTunerFrequency;
    private volatile boolean mFailed;
    private final Object mRunLock = new Object();
    private boolean mStopped;

    /**
     * Constructs an instance.
     * @param producerSourceEventListener to receive source event requests (e.g. start/stop sample stream)
     * @param tunerChannel describing the desired channel frequency and bandwidth
     * @param channelSpecification of the consumer, or null
     * @param channelizer providing the block parameters
     * @param tunerFrequency current tuner center frequency
     * @param threadName for the channel's dispatcher
     * @param tunerFrequencyErrorManager to connect with the channel frequency error manager
     * @throws IllegalArgumentException if the channel doesn't fit the channelizer's bandwidth at the tuner frequency
     */
    public OverlapSaveChannelSource(Listener<SourceEvent> producerSourceEventListener, TunerChannel tunerChannel,
                                    ChannelSpecification channelSpecification, OverlapSaveChannelizer channelizer,
                                    long tunerFrequency, String threadName,
                                    TunerFrequencyErrorManager tunerFrequencyErrorManager)
    {
        super(producerSourceEventListener, tunerChannel, threadName, tunerFrequencyErrorManager);
        mTunerSampleRate = channelizer.getSampleRate();

        int decoderReadySize = getDecoderReadyOutputSize(channelizer, channelSpecification);
        mChannelFiltered = decoderReadySize > 0;

        if(!mChannelFiltered && channelSpecification != null && channelSpecification.hasChannelFilter())
        {
            LOGGER.info("Overlap-save channel [" + tunerChannel.getFrequency() + "] can't apply the decoder's " +
                "channel filter at any rate that the decoder uses without decimating - using a wide channel filter " +
                "and the decoder's own filter");
        }

        mChannel = createChannel(channelizer, tunerChannel.getFrequency() - tunerFrequency, channelSpecification,
            decoderReadySize);
        mTunerFrequency = tunerFrequency;

        if(!fits(tunerFrequency))
        {
            throw new IllegalArgumentException("Channel [" + tunerChannel.getFrequency() + "] doesn't fit the tuner " +
                "bandwidth at center frequency [" + tunerFrequency + "]");
        }

        //An array queue, so that queueing a block doesn't allocate a node
        mDispatcher = new Dispatcher<>(threadName, DISPATCH_INTERVAL_MS, getHeartbeatManager(),
            new ArrayBlockingQueue<>(MAXIMUM_QUEUED_BLOCKS));
        mDispatcher.setListener(this::process);
    }

    /**
     * Indicates if the channelizer can deliver a decoder-ready channel for the specification
     * (getDecoderReadyOutputSize()).
     * @param channelizer providing the block parameters
     * @param channelSpecification of the consumer, or null
     * @return true if createChannel() applies the specification's channel filter
     */
    public static boolean isDecoderReady(OverlapSaveChannelizer channelizer, ChannelSpecification channelSpecification)
    {
        return getDecoderReadyOutputSize(channelizer, channelSpecification) > 0;
    }

    /**
     * Output size of a decoder-ready channel for the specification.  The specification must have a channel filter, and
     * the channel's sample rate must be one that the channelizer provides (the output size must divide the FFT size
     * into a decimation that divides the hop and the overlap, so not every rate is available at every tuner sample
     * rate), one that the decoder uses without decimating (ChannelFilter.isUndecimated()), and one at which the
     * decoder's filter is no longer than the overlap allows and is a working low-pass: its stop band is below the
     * output Nyquist edge and at least 30 dB down.  The rate is the filter's own sample rate when that qualifies,
     * otherwise the lowest qualifying rate above it, otherwise the highest qualifying rate below it down to 80% of the
     * filter's own.  Lower rates aren't used: a decoder can accept them, but with far fewer samples per symbol than it
     * was tuned for.
     * @param channelizer providing the block parameters
     * @param channelSpecification of the consumer, or null
     * @return output size, in bins, or 0 if the channelizer can't deliver a decoder-ready channel
     */
    public static int getDecoderReadyOutputSize(OverlapSaveChannelizer channelizer,
                                                ChannelSpecification channelSpecification)
    {
        ChannelSpecification.ChannelFilter filter = channelSpecification != null ?
            channelSpecification.getChannelFilter() : null;

        if(filter == null)
        {
            return 0;
        }

        int preferred = Math.max(1, (int)Math.ceil(filter.sampleRate() / channelizer.getBinWidth() - 1E-9));

        for(int size = preferred; size <= channelizer.getFFTSize(); size++)
        {
            if(isDecoderReady(channelizer, filter, size))
            {
                return size;
            }
        }

        int lowest = Math.max(1, (int)Math.ceil(filter.sampleRate() * LOWEST_RATE / channelizer.getBinWidth() - 1E-9));

        for(int size = preferred - 1; size >= lowest; size--)
        {
            if(isDecoderReady(channelizer, filter, size))
            {
                return size;
            }
        }

        return 0;
    }

    /**
     * Indicates if a channel of the output size can apply the decoder's channel filter.  Designs the decoder's filter
     * once, at the output size's rate.
     */
    private static boolean isDecoderReady(OverlapSaveChannelizer channelizer, ChannelSpecification.ChannelFilter filter,
                                          int outputSize)
    {
        if(!OverlapSaveChannel.isValidOutputSize(channelizer, outputSize))
        {
            return false;
        }

        double rate = outputSize * channelizer.getBinWidth();

        if(filter.stopFrequency() >= rate / 2.0 || !filter.undecimated().test(rate))
        {
            return false;
        }

        float[] taps;

        try
        {
            taps = filter.getTaps(rate);
        }
        catch(IllegalArgumentException | IllegalStateException e)
        {
            //The decoder rejects a rate where its filter design fails
            return false;
        }

        return taps.length <= OverlapSaveChannel.getMaximumFilterLength(channelizer, outputSize) &&
            getStopBandGain(taps, filter.stopFrequency(), rate) <= MINIMUM_STOP_BAND_GAIN;
    }

    /**
     * Largest gain of the filter from the stop frequency to the Nyquist edge, relative to its gain at 0 Hz.  A filter
     * design that didn't converge can have a stop band that's barely down, or above 0 dB.
     */
    private static double getStopBandGain(float[] taps, double stopFrequency, double sampleRate)
    {
        double dc = 0.0;

        for(float tap : taps)
        {
            dc += tap;
        }

        double worst = 0.0;
        int points = 64;

        for(int x = 0; x <= points; x++)
        {
            double frequency = stopFrequency + (sampleRate / 2.0 - stopFrequency) * x / points;
            double re = 0.0, im = 0.0;

            for(int k = 0; k < taps.length; k++)
            {
                double phase = 2.0 * Math.PI * frequency * k / sampleRate;
                re += taps[k] * Math.cos(phase);
                im -= taps[k] * Math.sin(phase);
            }

            worst = Math.max(worst, Math.hypot(re, im));
        }

        return worst / Math.abs(dc);
    }

    /**
     * Creates the channel slice for a consumer's channel specification.  When the channelizer can apply the
     * specification's channel filter (getDecoderReadyOutputSize()), the channel is at that output size's sample rate
     * and its filter is the decoder's own filter, designed at that rate, cascaded with a Kaiser low-pass just outside
     * the decoder's pass band, which keeps the response that the decoder is tuned to and adds stop band attenuation.
     * Otherwise the channel has a wide filter at a sample rate of at least 50 kHz.
     * @param channelizer providing the block parameters
     * @param frequencyOffset of the channel from the tuner center in Hertz
     * @param channelSpecification of the consumer, or null
     * @return channel
     */
    public static OverlapSaveChannel createChannel(OverlapSaveChannelizer channelizer, double frequencyOffset,
                                                   ChannelSpecification channelSpecification)
    {
        return createChannel(channelizer, frequencyOffset, channelSpecification,
            getDecoderReadyOutputSize(channelizer, channelSpecification));
    }

    /**
     * Creates the channel slice, for the output size from getDecoderReadyOutputSize().
     */
    private static OverlapSaveChannel createChannel(OverlapSaveChannelizer channelizer, double frequencyOffset,
                                                    ChannelSpecification channelSpecification, int decoderReadySize)
    {
        ChannelSpecification.ChannelFilter filter = decoderReadySize > 0 ? channelSpecification.getChannelFilter() :
            null;
        int outputSize = decoderReadySize > 0 ? decoderReadySize : OverlapSaveChannel.getOutputSize(channelizer,
            Math.max(MINIMUM_SAMPLE_RATE, channelSpecification != null ?
                channelSpecification.getMinimumSampleRate() : 0.0));
        double outputRate = outputSize * channelizer.getBinWidth();
        int maximumLength = OverlapSaveChannel.getMaximumFilterLength(channelizer, outputSize);
        float[] taps;

        if(filter != null)
        {
            //The decoder's own filter, so that the decoder sees the pass band response that it is tuned to, cascaded
            //with a Kaiser low-pass, within the overlap's length limit, for stop band attenuation.  The Kaiser filter's
            //transition starts a quarter of the decoder's transition band above the decoder's pass edge and ends half
            //a transition band above its stop edge, so it adds no pass band ripple.
            float[] decoderTaps = filter.getTaps(outputRate);
            int kaiserLength = maximumLength - decoderTaps.length + 1;
            double transition = filter.stopFrequency() - filter.passFrequency();
            double kaiserPass = filter.passFrequency() + transition / 4.0;
            double kaiserStop = Math.min(filter.stopFrequency() + transition / 2.0, outputRate / 2.0);

            //When the decoder's filter leaves the Kaiser filter too few taps for that transition band, the band widens
            //about its center.  Where that would reach into the decoder's pass band, the band starts at the decoder's
            //pass edge instead and widens upward, and if that puts the cutoff past the output Nyquist edge there's no
            //Kaiser filter, only the decoder's own.  A widened Kaiser filter adds little attenuation near the decoder's
            //stop edge and its full attenuation only further out.
            double widest = getKaiserTransition(outputRate, kaiserLength);
            double center = (kaiserPass + kaiserStop) / 2.0;

            if(center - widest / 2.0 < filter.passFrequency())
            {
                kaiserPass = filter.passFrequency();
                kaiserStop = kaiserPass + widest;
            }

            taps = kaiserLength >= 3 && (kaiserPass + kaiserStop) / 2.0 < outputRate / 2.0 ?
                OverlapSaveChannel.convolve(decoderTaps, OverlapSaveChannel.designFilter(outputRate, kaiserPass,
                kaiserStop, ATTENUATION, kaiserLength)) : decoderTaps;
        }
        else
        {
            taps = OverlapSaveChannel.designFilter(outputRate, outputRate * WIDE_FILTER_PASS,
                outputRate * WIDE_FILTER_STOP, ATTENUATION, maximumLength);
        }

        return new OverlapSaveChannel(channelizer, frequencyOffset, outputSize, taps, 1.0f);
    }

    /**
     * Narrowest transition band, in Hertz, that a Kaiser filter of the length meets the attenuation over (the length
     * estimate of OverlapSaveChannel.designFilter(), solved for the transition at the odd length it uses).
     */
    private static double getKaiserTransition(double sampleRate, int length)
    {
        int odd = length % 2 == 0 ? length - 1 : length;
        return odd > 1 ? (ATTENUATION - 7.95) / (2.285 * (odd - 1)) * sampleRate / (2.0 * Math.PI) :
            Double.POSITIVE_INFINITY;
    }

    /**
     * Indicates if the channel's bandwidth (its minimum and maximum frequencies) lies within the tuner's sampled
     * bandwidth at the tuner frequency.  The output band can extend past the tuner's band edge: those bins wrap around
     * from the opposite band edge, and since they are at or beyond the channel's own edge, the channel filter treats
     * them like an adjacent channel, as with the polyphase channelizer.  The channel source manager validates the tuner
     * center frequency with the same minimum and maximum frequencies and the tuner's usable bandwidth, which is within
     * this.
     */
    private boolean fits(long tunerFrequency)
    {
        double half = mTunerSampleRate / 2.0;
        return getTunerChannel().getMinFrequency() >= tunerFrequency - half &&
            getTunerChannel().getMaxFrequency() <= tunerFrequency + half;
    }

    /**
     * Indicates if this source failed (see fail()).
     */
    boolean isFailed()
    {
        return mFailed;
    }

    @Override
    public boolean isChannelFiltered()
    {
        return mChannelFiltered;
    }

    @Override
    public double getSampleRate()
    {
        return mChannel.getOutputSampleRate();
    }

    /**
     * The output sample rate is set by the channel specification and doesn't change.
     */
    @Override
    protected void setSampleRate(double sampleRate)
    {
    }

    /**
     * Sample rate of the tuner providing input to the channelizer.
     */
    public double getTunerSampleRate()
    {
        return mTunerSampleRate;
    }

    /**
     * Tuner center frequency that the channel's bins are selected for.
     */
    public long getTunerFrequency()
    {
        return mTunerFrequency;
    }

    /**
     * Updates the channel for a new tuner center frequency.  Invoke this on the channelizer's thread at a block
     * boundary (OverlapSaveChannelizer.setBlockBoundaryTask()) so that it applies from the next block.
     * @param tunerFrequency new tuner center frequency
     */
    public void updateTunerFrequency(long tunerFrequency)
    {
        if(mFailed || tunerFrequency == mTunerFrequency)
        {
            return;
        }

        mTunerFrequency = tunerFrequency;

        if(fits(tunerFrequency))
        {
            mChannel.setFrequencyOffset(getTunerChannel().getFrequency() - tunerFrequency);
        }
        else
        {
            fail("Tuner center frequency [" + tunerFrequency + "] no longer provides channel [" +
                getTunerChannel().getFrequency() + "]");
        }
    }

    /**
     * Marks this source failed, so that it gathers no more blocks and doesn't start its dispatcher, and sets its error
     * state.  Stopping the channel is a blocking operation, so the error state is set on another thread, not on the
     * caller's (the channelizer's thread, or the channel manager's while it holds its lock).
     * @param message describing the failure
     */
    void fail(String message)
    {
        mFailed = true;
        LOGGER.error(message);
        ThreadPool.CACHED.execute(() -> setError(message));
    }

    /**
     * Sets the tuner center frequency.  Prefer updateTunerFrequency() on the channelizer's thread.
     */
    @Override
    public void setFrequency(long frequency)
    {
        updateTunerFrequency(frequency);
    }

    @Override
    public void setFrequencyCorrection(long correction)
    {
        mFrequencyCorrection = correction;
    }

    /**
     * Frequency offset that the residual mixer applies after the bin selection: the bin center's offset from the
     * channel plus the frequency correction.
     */
    public double getMixerFrequency()
    {
        return mFrequencyCorrection - mChannel.getResidualFrequency();
    }

    @Override
    public void setListener(Listener<ComplexSamples> listener)
    {
        mListener = listener;
    }

    @Override
    public void start()
    {
        //Start the dispatcher once the channel manager has accepted the channel (see fail()), so that a failed start
        //doesn't leave the dispatcher thread running.  Blocks before then are skipped, as before any start.
        super.start();

        synchronized(mRunLock)
        {
            if(!mFailed && !mStopped)
            {
                mDispatcher.start();
            }
        }
    }

    @Override
    public void stop()
    {
        super.stop();

        //A failure can stop this source on another thread while start() is still running, so start() and stop()
        //agree on the dispatcher under its lock
        synchronized(mRunLock)
        {
            mStopped = true;
            mDispatcher.stop();
        }
    }

    @Override
    public void dispose()
    {
        mListener = null;
    }

    /**
     * Copies this channel's bins from the block's spectrum and queues them for the dispatcher thread.  Invoked on the
     * channelizer's thread.
     */
    @Override
    public void processBlock(float[] spectrumI, float[] spectrumQ, long block, long timestamp)
    {
        if(mFailed || !mDispatcher.isRunning())
        {
            return;
        }

        QueuedBlock queued = mSpareBlock;
        mSpareBlock = null;

        if(queued == null)
        {
            queued = mBlockPool.acquire();
        }

        if(queued == null)
        {
            queued = new QueuedBlock(mChannel.getOutputSize() * 2);
        }

        queued.mTuning = mChannel.gather(spectrumI, spectrumQ, queued.mBins);
        queued.mBlock = block;
        queued.mTimestamp = timestamp;

        //A block that isn't queued never reaches the dispatcher thread, so this thread keeps it.  Blocks dropped by a
        //dispatcher stop aren't returned and are replaced as needed.
        if(!mDispatcher.offer(queued))
        {
            mSpareBlock = queued;
        }
    }

    /**
     * Processes a queued block on the dispatcher thread.
     */
    private void process(QueuedBlock queued)
    {
        try
        {
            mChannel.setFrequencyCorrection(mFrequencyCorrection);
            ComplexSamples samples = mChannel.process(queued.mBins, queued.mTuning, queued.mBlock, queued.mTimestamp);
            Listener<ComplexSamples> listener = mListener;

            if(listener != null)
            {
                listener.receive(samples);
            }
        }
        catch(Throwable t)
        {
            LOGGER.error("Error processing channel samples", t);
        }
        finally
        {
            mBlockPool.release(queued);
        }
    }

    /**
     * Description of the state of this source.
     */
    public String getStateDescription()
    {
        return "Overlap-Save | Tuner SR:" + mTunerSampleRate + " CF:" + mTunerFrequency + " | Channel CF:" +
            getTunerChannel().getFrequency() + " SR:" + getSampleRate() + " BIN:" + mChannel.getCenterBin() +
            " MIXER:" + getMixerFrequency() + (mChannelFiltered ? " CHANNEL FILTERED" : "");
    }

    @Override
    public String toString()
    {
        return "OVERLAP-SAVE " + getTunerChannel().getFrequency();
    }

    /**
     * Gathered bins for one block, reused through the block pool.  The channelizer thread fills it before queueing it
     * and the dispatcher thread reads it after taking it from the queue, so the queue orders the accesses.
     */
    static final class QueuedBlock
    {
        final float[] mBins;
        OverlapSaveChannel.Tuning mTuning;
        long mBlock;
        long mTimestamp;

        QueuedBlock(int length)
        {
            mBins = new float[length];
        }
    }

    /**
     * Lock-free pool of processed blocks for one releasing thread (the dispatcher) and one acquiring thread (the
     * channelizer).  A block is released only after it's processed, so the pool never hands out a block in use.  A
     * block released into a full pool is left to the garbage collector.
     */
    static final class BlockPool
    {
        private final QueuedBlock[] mBlocks;
        private final AtomicLong mAcquired = new AtomicLong();
        private final AtomicLong mReleased = new AtomicLong();

        BlockPool(int capacity)
        {
            mBlocks = new QueuedBlock[capacity];
        }

        /**
         * Takes a block from the pool.  Acquiring thread only.
         * @return block, or null if the pool is empty
         */
        QueuedBlock acquire()
        {
            long acquired = mAcquired.get();

            if(acquired == mReleased.get())
            {
                return null;
            }

            int index = (int)(acquired % mBlocks.length);
            QueuedBlock block = mBlocks[index];
            mBlocks[index] = null;
            mAcquired.set(acquired + 1);
            return block;
        }

        /**
         * Returns a processed block to the pool.  Releasing thread only.
         * @param block that is no longer in use
         * @return true if pooled, false if the pool is full
         */
        boolean release(QueuedBlock block)
        {
            long released = mReleased.get();

            if(released - mAcquired.get() == mBlocks.length)
            {
                return false;
            }

            mBlocks[(int)(released % mBlocks.length)] = block;
            mReleased.set(released + 1);
            return true;
        }

        /**
         * Number of pooled blocks.
         */
        int size()
        {
            return (int)(mReleased.get() - mAcquired.get());
        }
    }
}
