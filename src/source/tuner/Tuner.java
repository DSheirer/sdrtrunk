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
package source.tuner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Broadcaster;
import sample.Listener;
import sample.complex.ComplexBuffer;
import source.ISourceEventProcessor;
import source.SourceEvent;
import source.SourceException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;

/**
 * Tuner - provides tuner channel sources, representing a channel frequency
 */
public abstract class Tuner implements ITunerChannelProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(Tuner.class);

    private String mName;
    private TunerController mTunerController;
    /**
     * Sample Listeners - these will typically be the DFT processor for spectral
     * display, or it will be one or more tuner channel sources
     */
    protected List<Listener<ComplexBuffer>> mSampleListeners = new CopyOnWriteArrayList<>();
    protected List<Listener<TunerEvent>> mTunerChangeListeners = new ArrayList<>();
    protected Broadcaster<SourceEvent> mSourceEventBroadcaster = new Broadcaster<>();

    public Tuner(String name, TunerController tunerController)
    {
        mName = name;
        mTunerController = tunerController;

        //Rebroadcast frequency and sample rate change events as tuner events
        mTunerController.addListener(new ISourceEventProcessor()
        {
            @Override
            public void process(SourceEvent event)
            {
                switch(event.getEvent())
                {
                    case NOTIFICATION_FREQUENCY_CHANGE:
                        broadcast(new TunerEvent(Tuner.this,
                            source.tuner.TunerEvent.Event.FREQUENCY));
                        break;
                    case NOTIFICATION_SAMPLE_RATE_CHANGE:
                        broadcast(new TunerEvent(Tuner.this,
                            source.tuner.TunerEvent.Event.SAMPLE_RATE));
                        break;
                    default:
                        break;
                }
            }
        });
    }

    public TunerController getTunerController()
    {
        return mTunerController;
    }

    public String toString()
    {
        return mName;
    }

    public void dispose()
    {
        mSampleListeners.clear();
    }

    public void setName(String name)
    {
        mName = name;
    }

    /**
     * Unique identifier for this tuner, used to lookup a tuner configuration
     * from the settings manager.
     *
     * @return - unique identifier like a serial number, or a usb bus location
     * or ip address and port.  Return some form of unique identification that
     * allows this tuner to be identified from among several of the same types
     * of tuners.
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
     * Returns a tuner frequency channel source, tuned to the correct frequency
     *
     * Note: ensure the concrete implementation registers the returned
     * tuner channel source as a listener on the tuner, to receive frequency
     * change events.
     *
     * @param channel - desired frequency and bandwidth
     * @return - source for 48k sample rate
     */
    public abstract TunerChannelSource getChannel(TunerChannel channel)
        throws RejectedExecutionException, SourceException;

    /**
     * Releases the tuned channel resources
     *
     * Note: ensure the concrete implementation unregisters the tuner channel
     * source from the tuner as a frequency change listener
     *
     * @param source - previously obtained tuner channel
     */
    public abstract void releaseChannel(TunerChannelSource source);

    /**
     * Registers the listener to receive complex float sample arrays
     */
    public void addListener(Listener<ComplexBuffer> listener)
    {
        mSampleListeners.add(listener);
    }

    /**
     * Removes the registered listener
     */
    public void removeListener(Listener<ComplexBuffer> listener)
    {
        mSampleListeners.remove(listener);
    }

    /**
     * Broadcasts the samples to all registered listeners
     */
    public void broadcast(ComplexBuffer sampleBuffer)
    {
        for(Listener<ComplexBuffer> listener : mSampleListeners)
        {
            listener.receive(sampleBuffer);
        }
    }

    /**
     * Registers the listener
     */
    public void addTunerChangeListener(Listener<TunerEvent> listener)
    {
        mTunerChangeListeners.add(listener);
    }

    /**
     * Removes the registered listener
     */
    public void removeTunerChangeListener(Listener<TunerEvent> listener)
    {
        mTunerChangeListeners.remove(listener);
    }

    /**
     * Broadcasts the tuner change event
     */
    public void broadcast(TunerEvent tunerEvent)
    {
        for(Listener<TunerEvent> listener : mTunerChangeListeners)
        {
            listener.receive(tunerEvent);
        }

        //Re-broadcast frequency and sample rate change events as source events
        switch(tunerEvent.getEvent())
        {
            case FREQUENCY:
                mSourceEventBroadcaster.broadcast(SourceEvent.frequencyChange(getTunerController().getFrequency()));
                break;
            case SAMPLE_RATE:
                mSourceEventBroadcaster.broadcast(SourceEvent.sampleRateChange(getTunerController().getSampleRate()));
                break;
        }
    }

    /**
     * Adds the listener to receive source events
     */
    public void addSourceEventListener(Listener<SourceEvent> listener)
    {
        mSourceEventBroadcaster.addListener(listener);
    }

    /**
     * Removes the listener from receiving source events
     */
    public void removeSourceEventListener(Listener<SourceEvent> listener)
    {
        mSourceEventBroadcaster.removeListener(listener);
    }
}
