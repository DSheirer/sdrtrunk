/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.source.tuner;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Monitors frequency error measurements received from certain decoders (e.g. P25) and averages those
 * measurements over an interval period.  Averaged measurement values are applied to the tuner controller
 * and broadcast as a TunerEvent for visual display.
 */
public class TunerFrequencyErrorMonitor implements Listener<SourceEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(TunerFrequencyErrorMonitor.class);
    private static final int PROCESSING_INTERVAL_SECONDS = 5;
    private LinkedTransferQueue<Integer> mMeasurementsQueue = new LinkedTransferQueue<>();
    private List<Integer> mProcessingMeasurements = new ArrayList();
    private ScheduledFuture<?> mTimerHandle;
    private Tuner mTuner;

    /**
     * Constructs a monitor for the tuner argument.
     * @param tuner to monitor for frequency error measurements
     */
    public TunerFrequencyErrorMonitor(Tuner tuner)
    {
        mTuner = tuner;
    }

    /**
     * Primary method for receiving frequency error measurements from downstream decoders.
     */
    @Override
    public void receive(SourceEvent sourceEvent)
    {
        if(sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_MEASURED_FREQUENCY_ERROR_SYNC_LOCKED)
        {
            mMeasurementsQueue.add(sourceEvent.getValue().intValue());
        }
    }

    /**
     * Calculates the average frequency error measurement value from discrete measurements received from
     * the decoders over the interval period.
     */
    private void process()
    {
        mMeasurementsQueue.drainTo(mProcessingMeasurements);

        if(mProcessingMeasurements.size() > 0)
        {
            int sum = 0;

            for(Integer measurement: mProcessingMeasurements)
            {
                sum += measurement;
            }

            //Note: the measurements are the frequency correction being applied to each channel to compensate
            //for the error int the channel.  So, we report the negated value as the current error measurement
            broadcast(-(sum / mProcessingMeasurements.size()));
        }
        else
        {
            broadcast(0);
        }

        mProcessingMeasurements.clear();
    }

    private void broadcast(int averageError)
    {
        mTuner.getTunerController().setMeasuredFrequencyError(averageError);
        mTuner.broadcast(new TunerEvent(mTuner, TunerEvent.Event.UPDATE_MEASURED_FREQUENCY_ERROR));
    }

    /**
     * Starts this monitor processing frequency error measurements received from select channel decoders
     * and averaging those values over a 1 second interval for transmission as a tuner event.
     */
    public void start()
    {
        if(mTimerHandle == null)
        {
            mTimerHandle = ThreadPool.SCHEDULED.scheduleAtFixedRate(new Processor(), 0,
                PROCESSING_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
    }

    /**
     * Stops this monitor
     */
    public void stop()
    {
        if(mTimerHandle != null)
        {
            mTimerHandle.cancel(true);
            mTimerHandle = null;
            broadcast(0);
        }
    }

    /**
     * Runnable implementation to invoke the process() method when the timer fires.
     */
    public class Processor implements Runnable
    {
        @Override
        public void run()
        {
            process();
        }
    }
}
