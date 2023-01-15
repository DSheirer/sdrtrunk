/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

import io.github.dsheirer.preference.source.ChannelizerType;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerEvent.Event;
import io.github.dsheirer.source.tuner.manager.ChannelSourceManager;
import io.github.dsheirer.source.tuner.manager.HeterodyneChannelSourceManager;
import io.github.dsheirer.source.tuner.manager.PolyphaseChannelSourceManager;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tuner provides an interface to a software or hardware tuner controller that provides I/Q sample data coupled with a
 * channel source manager to provide access to Digital Drop Channel (DDC) resources.
 */
public abstract class Tuner implements ISourceEventProcessor, ITunerErrorListener
{
    private final static Logger mLog = LoggerFactory.getLogger(Tuner.class);

    private Broadcaster<TunerEvent> mTunerEventBroadcaster = new Broadcaster<>();
    private ChannelSourceManager mChannelSourceManager;
    private TunerController mTunerController;
    private TunerFrequencyErrorMonitor mTunerFrequencyErrorMonitor;
    private ITunerErrorListener mTunerErrorListener;
    private AtomicBoolean mRunning = new AtomicBoolean();

    public Tuner(TunerController tunerController, ITunerErrorListener tunerErrorListener)
    {
        mTunerController = tunerController;
        mTunerErrorListener = tunerErrorListener;
        //Register to receive frequency and sample rate change notifications
        mTunerController.addListener(this::process);
        mTunerFrequencyErrorMonitor = new TunerFrequencyErrorMonitor(this);
        mTunerFrequencyErrorMonitor.start();
    }

    /**
     * Abstract tuner class.
     * @param tunerController for the tuner
     * @param tunerErrorListener to listen for tuner errors
     * @param channelizerType for the channelizer
     */
    public Tuner(TunerController tunerController, ITunerErrorListener tunerErrorListener, ChannelizerType channelizerType)
    {
        this(tunerController, tunerErrorListener);

        if(channelizerType == ChannelizerType.POLYPHASE)
        {
            setChannelSourceManager(new PolyphaseChannelSourceManager(mTunerController));
        }
        else if(channelizerType == ChannelizerType.HETERODYNE)
        {
            setChannelSourceManager(new HeterodyneChannelSourceManager(mTunerController));
        }
        else
        {
            throw new IllegalArgumentException("Unrecognized channelizer type: " + channelizerType);
        }
    }

    /**
     * Perform startup operations
     * @throws SourceException if there is an error that makes this tuner unusable
     */
    public void start() throws SourceException
    {
        if(mRunning.compareAndSet(false, true))
        {
            try
            {
                getTunerController().start();
            }
            catch(SourceException se)
            {
                mRunning.set(false);
                //Rethrow the source exception
                throw se;
            }
            catch(Exception e)
            {
                mRunning.set(false);
                //Wrap any other exceptions in a new source exception
                mLog.error("Error starting " + getTunerClass() + " tuner", e);
                throw new SourceException("Unable to start " + getTunerClass() + " tuner", e);
            }
        }
    }

    /**
     * Perform shutdown and disposal operations.
     */
    public void stop()
    {
        if(mRunning.compareAndSet(true, false))
        {
            broadcast(new TunerEvent(this, Event.NOTIFICATION_SHUTTING_DOWN));

            if(getChannelSourceManager() != null)
            {
                getChannelSourceManager().stopAllChannels();
                getChannelSourceManager().dispose();
                mChannelSourceManager = null;
            }

            getTunerController().stop();
            getTunerController().dispose();

            mTunerEventBroadcaster.clear();
            mTunerFrequencyErrorMonitor = null;
            mTunerErrorListener = null;
        }
    }

    /**
     * Sets an unrecoverable error state for this tuner and propagates the error to an external listener
     * @param errorMessage to set
     */
    @Override
    public void setErrorMessage(String errorMessage)
    {
        broadcast(new TunerEvent(this, Event.NOTIFICATION_ERROR_STATE));

        if(mTunerErrorListener != null)
        {
            mTunerErrorListener.setErrorMessage(errorMessage);
        }
    }

    /**
     * Process tuner removal error from tuner controller and propagate to an external listener.
     */
    @Override
    public void tunerRemoved()
    {
        stop();

        if(mTunerErrorListener != null)
        {
            mTunerErrorListener.tunerRemoved();
        }
    }

    /**
     * Sets the channel source manager
     * @param manager to use
     */
    protected void setChannelSourceManager(ChannelSourceManager manager)
    {
        mChannelSourceManager = manager;

        //Register to receive channel count change notifications
        mChannelSourceManager.addSourceEventListener(this::process);
    }

    /**
     * Maximum number of bytes per second (MBps) produced by this tuner.
     * @return Bytes Per Second (Bps)
     */
    public abstract int getMaximumUSBBitsPerSecond();

    @Override
    public void process(SourceEvent event)
    {
        switch(event.getEvent())
        {
            case NOTIFICATION_CHANNEL_COUNT_CHANGE:
                broadcast(new TunerEvent(Tuner.this, Event.UPDATE_CHANNEL_COUNT));
                break;
            case NOTIFICATION_FREQUENCY_CHANGE:
                broadcast(new TunerEvent(Tuner.this, Event.UPDATE_FREQUENCY));
                break;
            case NOTIFICATION_FREQUENCY_CORRECTION_CHANGE:
                broadcast(new TunerEvent(Tuner.this, Event.UPDATE_FREQUENCY_ERROR));
                break;
            case NOTIFICATION_SAMPLE_RATE_CHANGE:
                broadcast(new TunerEvent(Tuner.this, Event.UPDATE_SAMPLE_RATE));
                break;
            case NOTIFICATION_FREQUENCY_AND_SAMPLE_RATE_LOCKED:
            case NOTIFICATION_FREQUENCY_AND_SAMPLE_RATE_UNLOCKED:
                broadcast(new TunerEvent(Tuner.this, Event.UPDATE_LOCK_STATE));
                break;
            case NOTIFICATION_MEASURED_FREQUENCY_ERROR_SYNC_LOCKED:
                mTunerFrequencyErrorMonitor.receive(event);
                break;
            case NOTIFICATION_RECORDING_FILE_LOADED:
                //ignore
                break;
            default:
                mLog.debug("Unrecognized Source Event: " + event);
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
        return getPreferredName();
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
    public TunerType getTunerType()
    {
        return getTunerController().getTunerType();
    }

    /**
     * Preferred name for this.  This should be unique and will be used to find this tuner (e.g. channel preferred tuner)
     *
     * @return - string name of this tuner object
     */
    public abstract String getPreferredName();

    /**
     * Sample size in bits
     */
    public abstract double getSampleSize();

    /**
     * Registers the listener
     */
    public void addTunerEventListener(Listener<TunerEvent> listener)
    {
        mTunerEventBroadcaster.addListener(listener);
    }

    /**
     * Removes the registered listener
     */
    public void removeTunerEventListener(Listener<TunerEvent> listener)
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