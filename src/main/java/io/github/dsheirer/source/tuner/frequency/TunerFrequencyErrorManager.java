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

import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.util.ThreadPool;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controls tuner frequency error when Auto-PPM is enabled.  Works in tandem with the ChannelFrequencyErrorController
 * to receive error measurement feedback from each channel's decoder and calculate an average frequency error across
 * all channels and adjusts the tuner's PPM setting to compensate for tuner frequency drift over time.
 */
public class TunerFrequencyErrorManager implements ISourceEventProcessor
{
    private static final Logger LOG = LoggerFactory.getLogger(TunerFrequencyErrorManager.class);
    private static final double PPM_DIVISOR = 1.0 / 1_000_000.0;
    private static final long MAXIMUM_TUNER_ERROR_CORRECTION_PER_INTERVAL_HZ = 10;
    private static final long MINIMUM_CORRECTION_THRESHOLD_HZ = 50;
    private static final long TIMER_INTERVAL_SECONDS = 5;
    private static final DecimalFormat DF = new DecimalFormat("0.00000");
    private final List<ChannelFrequencyErrorManager> mChannelManagers = new ArrayList<>();
    private final TunerController mTunerController;
    private ScheduledFuture<?> mScheduledFuture;
    private boolean mEnabled = true;
    private boolean mShutdown = false;
    private double mTunerPPM;
    private long mTunerCorrection;

    /**
     * Constructs an instance
     * @param tunerController to receive automatic PPM adjustments.
     */
    public TunerFrequencyErrorManager(TunerController tunerController)
    {
        mTunerController = tunerController;
        //Register to receive frequency and PPM change notifications from the tuner
        mTunerController.addListener(this);
        mTunerPPM = mTunerController.getFrequencyCorrection();
        mTunerCorrection = (long) (mTunerController.getFrequency() * mTunerPPM * PPM_DIVISOR);
    }

    public void dispose()
    {
        mShutdown = true;
        stop();
    }

    @Override
    public void process(SourceEvent event) throws SourceException
    {
        //Receive tuner controller frequency change events to detect when the PPM has changed
        if(event.getEvent().equals(SourceEvent.Event.NOTIFICATION_FREQUENCY_CHANGE))
        {
            double ppm = mTunerController.getFrequencyCorrection();

            if(ppm != mTunerPPM)
            {
                double adjustment = ppm - mTunerPPM;
                long changeHz = (long)(mTunerController.getFrequency() * adjustment * PPM_DIVISOR);
                broadcast(changeHz);
                mTunerPPM = ppm;
                mTunerCorrection = (long)(mTunerController.getFrequency() * PPM_DIVISOR * mTunerPPM);
            }
        }
    }

    /**
     * Toggles the enabled state of this manager.  When enabled, applies PPM corrections to the tuner controller.
     * @param enabled
     */
    public void setEnabled(boolean enabled)
    {
        mEnabled = enabled;
    }

    /**
     * Indicates if tuner Auto-PPM is enabled.
     */
    public boolean isEnabled()
    {
        return mEnabled;
    }

    /**
     * Processes channel frequency error values and updates the tuner to apply a correction.
     */
    private void process()
    {
        mTunerController.getLock().lock();

        try
        {
            if(!mChannelManagers.isEmpty())
            {
                long requestedChangeHz = 0;
                int count = 0;

                for(ChannelFrequencyErrorManager manager : mChannelManagers)
                {
                    if(manager.isCurrentForTunerProcessing())
                    {
                        requestedChangeHz += manager.getFrequencyError();
                        count++;
                        manager.clearCurrentFlag();
                    }
                }

                requestedChangeHz /= count;
                mTunerController.setMeasuredFrequencyError((int)requestedChangeHz);

                if(Math.abs(requestedChangeHz) > MINIMUM_CORRECTION_THRESHOLD_HZ)
                {
                    requestedChangeHz = Math.clamp(requestedChangeHz, -MAXIMUM_TUNER_ERROR_CORRECTION_PER_INTERVAL_HZ, MAXIMUM_TUNER_ERROR_CORRECTION_PER_INTERVAL_HZ);

                    if(mEnabled)
                    {
                        double adjustment = requestedChangeHz / (mTunerController.getFrequency() * PPM_DIVISOR);

                        try
                        {
                            mTunerController.setFrequencyCorrection(mTunerController.getFrequencyCorrection() + adjustment);
                        }
                        catch(SourceException e)
                        {
                            LOG.error("Error while adjusting PPM value", e);
                        }
                    }
                }
            }
        }
        finally
        {
            mTunerController.getLock().unlock();
        }
    }

    /**
     * Tuner frequency error correction in parts-per-million (ppm).
     */
    public double getTunerPPM()
    {
        return mTunerController.getFrequencyCorrection();
    }

    /**
     * Current tuner frequency correction value.
     * @return correction in Hertz
     */
    public long getTunerFrequencyCorrection()
    {
        return mTunerCorrection;
    }

    /**
     * Notifies each of the channel managers of the amount of frequency change resulting from a PPM automatic adjustment
     * or a PPM manual adjustment.
     * @param change (ie correction) in Hertz that was applied at the tuner
     */
    private void broadcast(long change)
    {
        mTunerController.getLock().lock();

        try
        {
            for(ChannelFrequencyErrorManager channelManager: mChannelManagers)
            {
                channelManager.receive(change);
            }
        }
        finally
        {
            mTunerController.getLock().unlock();
        }
    }

    /**
     * Adds the channel manager to receive and provide error updates and automatically starts the timer thread
     * if it is not already running.
     * @param channelFrequencyErrorManager to add
     */
    public void add(ChannelFrequencyErrorManager channelFrequencyErrorManager)
    {
        boolean startRequired = false;

        mTunerController.getLock().lock();

        try
        {
            if(!mChannelManagers.contains(channelFrequencyErrorManager))
            {
                mChannelManagers.add(channelFrequencyErrorManager);
                startRequired = true;
            }
        }
        finally
        {
            mTunerController.getLock().unlock();
        }

        if(startRequired)
        {
            start();
        }
    }

    /**
     * Removes the channel error manager and stops the timer thread if all channels are removed.
     * @param channelFrequencyErrorManager to remove
     */
    public void remove(ChannelFrequencyErrorManager channelFrequencyErrorManager)
    {
        boolean stopRequired = false;

        mTunerController.getLock().lock();

        try
        {
            mChannelManagers.remove(channelFrequencyErrorManager);
            stopRequired = mChannelManagers.isEmpty();
        }
        finally
        {
            mTunerController.getLock().unlock();
        }

        if(stopRequired)
        {
            stop();
        }
    }

    /**
     * Starts the timer processing of frequency error correction.
     */
    public void start()
    {
        if(!mShutdown && mScheduledFuture == null)
        {
            mScheduledFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::process, TIMER_INTERVAL_SECONDS,
                    TIMER_INTERVAL_SECONDS, TimeUnit.SECONDS);
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

        mScheduledFuture = null;
    }
}
