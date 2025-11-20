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
    private static final long TIMER_INTERVAL_MILLISECONDS = 500;
    private final FloatAveragingBuffer mRequestedCorrectAveragingBuffer = new FloatAveragingBuffer(6, 2);
    private final TunerChannelSource mTunerChannelSource;
    private final TunerFrequencyErrorManager mParent;
    private ScheduledFuture<?> mScheduledFuture;
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
    public long getFrequencyError()
    {
        return mAppliedFrequencyCorrection;
    }

    /**
     * Receive a notification from the parent tuner is updating the PPM and frequency correction change value.
     *
     * Note: on receiving this change notification, the tuner manager will expose the current PPM and correction values.
     * @param change in Hertz resulting from the tuner's PPM update.
     */
    public void receive(long change)
    {
        //The TunerFrequencyErrorManager notifies this channel of the correction applied at the tuner
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

            if(partialCorrection != 0)
            {
                mAppliedFrequencyCorrection += partialCorrection;
                mTunerChannelSource.setFrequencyCorrection(mAppliedFrequencyCorrection);
                mRequestedCorrectAveragingBuffer.reset();
            }

            ChannelFrequencyCorrectionStatusNotification notification = ChannelFrequencyCorrectionStatusNotification
                    .create(mTunerChannelSource, mAverageRequestedCorrection, mAppliedFrequencyCorrection, mParent.getTunerPPM(),
                            mParent.getTunerFrequencyCorrection(), mParent.isEnabled());
            mTunerChannelSource.broadcastConsumerSourceEvent(notification);
            mAverageRequestedCorrection = 0;
            mCurrentForTunerProcessing = true;
            mCurrentForChannelProcessing = false;
        }
        else
        {
            ChannelFrequencyCorrectionStatusNotification notification = ChannelFrequencyCorrectionStatusNotification
                    .create(mTunerChannelSource, 0, mAppliedFrequencyCorrection, mParent.getTunerPPM(),
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
