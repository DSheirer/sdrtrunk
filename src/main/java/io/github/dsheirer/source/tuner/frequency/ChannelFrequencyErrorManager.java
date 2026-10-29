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

package io.github.dsheirer.source.tuner.frequency;

import io.github.dsheirer.buffer.FloatAveragingBuffer;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.ChannelFrequencyCorrectionStatusNotification;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import io.github.dsheirer.util.ThreadPool;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Channel frequency error controller works in tandem with the TunerFrequencyErrorController to track and send error
 * reports from the decoder and apply an oscillator to remove the tracked error incrementally over time.
 */
public class ChannelFrequencyErrorManager implements ISourceEventListener, Listener<SourceEvent>
{
    private static final long MAXIMUM_CHANNEL_ERROR_CORRECTION_PER_INTERVAL = 10; //Hertz
    private static final long TIMER_INTERVAL_MILLISECONDS = 1000;
    private static final float ERROR_PROCESSING_GAIN = 0.3f;
    private final FloatAveragingBuffer mRequestedCorrectAveragingBuffer = new FloatAveragingBuffer(12, 2);
    private final TunerChannelSource mTunerChannelSource;
    private final TunerFrequencyErrorManager mParent;
    private ScheduledFuture<?> mScheduledFuture;
    //Guarded by this instance: updated by this manager's timer and by the tuner's PPM change notifications
    private long mAppliedFrequencyCorrection = 0;
    private long mAverageRequestedCorrection = 0;
    private boolean mCurrentForTunerProcessing = false;
    private boolean mCurrentForChannelProcessing = false;

    /**
     * Constructs an instance.
     * @param tunerChannelSource to receive frequency correction requests
     * @param parent to register for tuner frequency correction events
     */
    public ChannelFrequencyErrorManager(TunerChannelSource tunerChannelSource, TunerFrequencyErrorManager parent)
    {
        mTunerChannelSource = tunerChannelSource;
        mParent = parent;

        if(mParent != null)
        {
            mParent.add(this);
        }
    }

    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return this;
    }

    /**
     * Snapshot of the current applied frequency correction for the channel.
     * @return total frequency error in Hertz.
     */
    public synchronized long getFrequencyError()
    {
        return mAppliedFrequencyCorrection;
    }

    /**
     * Receive a notification from the parent tuner is updating the PPM and frequency correction change value.
     *
     * The tuner PPM change shifts every channel by the change: a positive PPM change lowers the tuned frequency, which
     * raises each channel's signal frequency by the change, the same as a positive channel frequency correction.  The
     * tuner absorbs the change from the average of the channel corrections, so remove the change from this channel's
     * applied correction to keep the channel's net correction (and the decoder's view of the signal) unchanged.
     * Otherwise, each PPM change appears to the decoder as a sudden carrier offset step that the channel correction
     * removes over the following correction intervals, while the tuner continues to absorb the stale channel
     * correction.
     *
     * Note: invoked by the tuner's frequency error manager while holding the tuner controller lock, so this method
     * must not call back into the tuner.
     * @param change in Hertz resulting from the tuner's PPM update.
     */
    public synchronized void receive(long change)
    {
        mAppliedFrequencyCorrection -= change;
        mTunerChannelSource.setFrequencyCorrection(mAppliedFrequencyCorrection);
    }

    /**
     * Receive error measurements from the channel demodulator and average them and update the actual frequency error
     * so that when the timer fires, it sends a correction update to the channel source to incrementally drive the
     * difference between the actual and corrected frequency error values.
     * @param sourceEvent from the FeedbackDecoder.
     */
    @Override
    public void receive(SourceEvent sourceEvent)
    {
        if(sourceEvent.getEvent().equals(SourceEvent.Event.REQUEST_FREQUENCY_CORRECTION))
        {
            long requestedCorrection = sourceEvent.getValue().longValue();
            mAverageRequestedCorrection = (long) mRequestedCorrectAveragingBuffer.get(requestedCorrection);
            mCurrentForChannelProcessing = true;
        }
    }

    /**
     * Indicates if this manager has received an error measurement from the decoder since the last time the current
     * flag was cleared, when the last error measurement was read.
     * @return true if there is current error value available.
     */
    public boolean isCurrentForTunerProcessing()
    {
        return mCurrentForTunerProcessing;
    }

    /**
     * Clears the current flag once the error value is read
     */
    public void clearCurrentFlag()
    {
        mCurrentForTunerProcessing = false;
    }

    /**
     * Processes the frequency error and sends an updated correction request to the TunerChannelSource.  This method
     * is invoked on a timer so that it is independent of the decoder and sync pattern rate.
     */
    private void process()
    {
        if(mCurrentForChannelProcessing)
        {
            long partialCorrection = Math.clamp(mAverageRequestedCorrection, -MAXIMUM_CHANNEL_ERROR_CORRECTION_PER_INTERVAL, MAXIMUM_CHANNEL_ERROR_CORRECTION_PER_INTERVAL);
            long appliedFrequencyCorrection;

            //receive(long change) holds the tuner controller lock while it locks this instance, so the status
            //notification below, which calls into the tuner, is created outside of this block to avoid a lock order
            //inversion.
            synchronized(this)
            {
                if(partialCorrection != 0)
                {
                    mAppliedFrequencyCorrection += (long)(partialCorrection * ERROR_PROCESSING_GAIN);
                    mTunerChannelSource.setFrequencyCorrection(mAppliedFrequencyCorrection);
                    mRequestedCorrectAveragingBuffer.reset();
                }

                appliedFrequencyCorrection = mAppliedFrequencyCorrection;
            }

            ChannelFrequencyCorrectionStatusNotification notification = ChannelFrequencyCorrectionStatusNotification
                    .create(mTunerChannelSource, mAverageRequestedCorrection, appliedFrequencyCorrection, mParent.getTunerPPM(),
                            mParent.getTunerFrequencyCorrection(), mParent.isEnabled());
            mTunerChannelSource.broadcastConsumerSourceEvent(notification);
            mAverageRequestedCorrection = 0;
            mCurrentForTunerProcessing = true;
            mCurrentForChannelProcessing = false;
        }
        else
        {
            ChannelFrequencyCorrectionStatusNotification notification = ChannelFrequencyCorrectionStatusNotification
                    .create(mTunerChannelSource, 0, getFrequencyError(), mParent.getTunerPPM(),
                            mParent.getTunerFrequencyCorrection(), mParent.isEnabled());
            mTunerChannelSource.broadcastConsumerSourceEvent(notification);
        }
    }

    /**
     * Starts the timer processing of frequency error correction.
     */
    public void start()
    {
        if(mParent != null)
        {
            mParent.add(this);

            if(mScheduledFuture == null)
            {
                mScheduledFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::process, TIMER_INTERVAL_MILLISECONDS,
                        TIMER_INTERVAL_MILLISECONDS, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Cancels the timer processing of frequency error correction.
     */
    public void stop()
    {
        if(mScheduledFuture != null)
        {
            mScheduledFuture.cancel(true);
        }

        if(mParent != null)
        {
            mParent.remove(this);
        }

        mScheduledFuture = null;
    }
}
