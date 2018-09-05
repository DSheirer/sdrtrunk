/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.source.tuner;

import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.TunerEvent.Event;
import io.github.dsheirer.source.tuner.manager.ChannelSourceManager;
import io.github.dsheirer.source.tuner.manager.HeterodyneChannelSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tuner provides an interface to a software or hardware tuner controller that provides I/Q sample data coupled with a
 * channel source manager to provide access to Digital Drop Channel (DDC) resources.
 */
public abstract class Tuner implements ISourceEventProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(Tuner.class);

    private Broadcaster<TunerEvent> mTunerEventBroadcaster = new Broadcaster<>();
    private ChannelSourceManager mChannelSourceManager;
    private TunerController mTunerController;
    private TunerFrequencyErrorMonitor mTunerFrequencyErrorMonitor;
    private String mName;

    /**
     * Abstract tuner class.
     * @param name of the tuner
     * @param tunerController for the tuner
     */
    public Tuner(String name, TunerController tunerController)
    {
        mName = name;
        mTunerController = tunerController;
        //Register to receive frequency and sample rate change notifications
        mTunerController.addListener(this::process);

//        mChannelSourceManager = new PolyphaseChannelSourceManager(mTunerController);
        mChannelSourceManager = new HeterodyneChannelSourceManager(mTunerController);

        //Register to receive channel count change notifications
        mChannelSourceManager.addSourceEventListener(this::process);

        mTunerFrequencyErrorMonitor = new TunerFrequencyErrorMonitor(this);
        mTunerFrequencyErrorMonitor.start();
    }

    @Override
    public void process(SourceEvent event)
    {
        switch(event.getEvent())
        {
            case NOTIFICATION_CHANNEL_COUNT_CHANGE:
                broadcast(new TunerEvent(Tuner.this, Event.CHANNEL_COUNT));
                break;
            case NOTIFICATION_FREQUENCY_CHANGE:
                broadcast(new TunerEvent(Tuner.this, Event.FREQUENCY_UPDATED));
                break;
            case NOTIFICATION_FREQUENCY_CORRECTION_CHANGE:
                broadcast(new TunerEvent(Tuner.this, Event.FREQUENCY_ERROR_UPDATED));
                break;
            case NOTIFICATION_SAMPLE_RATE_CHANGE:
                broadcast(new TunerEvent(Tuner.this, Event.SAMPLE_RATE_UPDATED));
                break;
            case NOTIFICATION_FREQUENCY_AND_SAMPLE_RATE_LOCKED:
            case NOTIFICATION_FREQUENCY_AND_SAMPLE_RATE_UNLOCKED:
                broadcast(new TunerEvent(Tuner.this, Event.LOCK_STATE_CHANGE));
                break;
            case NOTIFICATION_MEASURED_FREQUENCY_ERROR_SYNC_LOCKED:
                mTunerFrequencyErrorMonitor.receive(event);
                break;
            default:
                mLog.debug("Unrecognized Source Event: " + event.toString());
                break;
        }
    }

    /**
     * Source Manager.  Provides access to registering for complex buffer samples and source event notifications.
     */
    public ChannelSourceManager getChannelSourceManager()
    {
        return mChannelSourceManager;
    }

    /**
     * Tuner controller.
     */
    public TunerController getTunerController()
    {
        return mTunerController;
    }

    /**
     * Name for this tuner
     */
    public String toString()
    {
        return mName;
    }

    /**
     * Dispose and prepare for shutdown
     */
    public void dispose()
    {
        getTunerController().dispose();
    }

    /**
     * Sets the name for this tuner
     */
    public void setName(String name)
    {
        mName = name;
    }

    /**
     * Unique identifier for this tuner, used to lookup a tuner configuration from the settings manager.
     *
     * @return - unique identifier like a serial number, or a usb bus location or ip address and port.  Return some
     * form of unique identification that allows this tuner to be identified from among the same types of tuners.
     */
    public abstract String getUniqueID();

    /**
     * @return - tuner class enum entry
     */
    public abstract TunerClass getTunerClass();

    /**
     * @return - tuner type enum entry
     */
    public abstract TunerType getTunerType();

    /**
     * Name of this tuner object
     *
     * @return - string name of this tuner object
     */
    public String getName()
    {
        return mName;
    }

    /**
     * Sample size in bits
     */
    public abstract double getSampleSize();

    /**
     * Registers the listener
     */
    public void addTunerChangeListener(Listener<TunerEvent> listener)
    {
        mTunerEventBroadcaster.addListener(listener);
    }

    /**
     * Removes the registered listener
     */
    public void removeTunerChangeListener(Listener<TunerEvent> listener)
    {
        mTunerEventBroadcaster.removeListener(listener);
    }

    /**
     * Broadcasts the tuner change event
     */
    protected void broadcast(TunerEvent tunerEvent)
    {
        mTunerEventBroadcaster.broadcast(tunerEvent);
    }
}