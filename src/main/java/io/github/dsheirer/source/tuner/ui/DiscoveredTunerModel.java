/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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
package io.github.dsheirer.source.tuner.ui;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerEvent;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationManager;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.DiscoveredUSBTuner;
import io.github.dsheirer.source.tuner.manager.IDiscoveredTunerStatusListener;
import io.github.dsheirer.source.tuner.manager.TunerStatus;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.DiscoveredRspDuoTuner1;
import io.github.dsheirer.source.tuner.sdrplay.rspDuo.DiscoveredRspDuoTuner2;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model for discovered tuners
 */
public class DiscoveredTunerModel implements Listener<TunerEvent>,
        IDiscoveredTunerStatusListener
{
    private static final Logger mLog = LoggerFactory.getLogger(DiscoveredTunerModel.class);


    private List<DiscoveredTuner> mDiscoveredTuners = new CopyOnWriteArrayList<>();
    private List<Listener<TunerEvent>> mTunerEventListeners = new ArrayList<>();
    private List<ModelListener> mModelListeners = new CopyOnWriteArrayList<>();
    private Lock mLock = new ReentrantLock();
    private TunerConfigurationManager mTunerConfigurationManager;

    /**
     * Constructs an instance
     * @param tunerConfigurationManager to update tuner configurations from tuners.
     */
    public DiscoveredTunerModel(TunerConfigurationManager tunerConfigurationManager)
    {
        mTunerConfigurationManager = tunerConfigurationManager;
    }

    /**
     * List of currently enabled and available tuners
     */
    public List<DiscoveredTuner> getAvailableTuners()
    {
        return mDiscoveredTuners.stream().filter(discoveredTuner -> discoveredTuner.hasTuner()).toList();
    }

    /**
     * Find the discovered tuner that matches the instantiated tuner
     * @param tuner to match
     * @return matching discovered tuner
     */
    private DiscoveredTuner getDiscoveredTuner(Tuner tuner)
    {
        DiscoveredTuner match = null;

        mLock.lock();

        try
        {
            for(DiscoveredTuner discoveredTuner: mDiscoveredTuners)
            {
                if(discoveredTuner.hasTuner() && discoveredTuner.getTuner().equals(tuner))
                {
                    match = discoveredTuner;
                    break;
                }
            }
        }
        finally
        {
            mLock.unlock();
        }

        return match;
    }

    /**
     * Access the discovered tuner at the specified row index
     * @param index to lookup
     * @return discovered tuner or null
     */
    public DiscoveredTuner getDiscoveredTuner(int index)
    {
        mLock.lock();

        try
        {
            if(index < mDiscoveredTuners.size())
            {
                return mDiscoveredTuners.get(index);
            }
        }
        finally
        {
            mLock.unlock();
        }

        return null;
    }

    /**
     * Find a discovered tuner by ID
     * @param id of the tuner to search for
     * @return discovered tuner with matching ID or null.
     */
    public DiscoveredTuner getDiscoveredTuner(String id)
    {
        DiscoveredTuner discoveredTuner = null;

        mLock.lock();

        try
        {
            Optional<DiscoveredTuner> result = mDiscoveredTuners.stream().filter(tuner -> tuner.getId().equals(id)).findFirst();

            if(result.isPresent())
            {
                discoveredTuner = result.get();
            }
        }
        finally
        {
            mLock.unlock();
        }

        return discoveredTuner;
    }


    /**
     * Adds the Tuner to this model
     */
    public void addDiscoveredTuner(DiscoveredTuner discoveredTuner)
    {
        mLock.lock();

        try
        {
            if(!mDiscoveredTuners.contains(discoveredTuner))
            {
                mDiscoveredTuners.add(discoveredTuner);
                notifyAdded(discoveredTuner);
                if(discoveredTuner.hasTuner())
                {
                    discoveredTuner.getTuner().addTunerEventListener(this);
                }
                discoveredTuner.addTunerStatusListener(this);
            }
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Stops and removes all discovered tuners, in preparation for shutdown.
     */
    public void releaseDiscoveredTuners()
    {
        mLock.lock();

        List<DiscoveredTuner> discoveredTuners = new ArrayList<>(mDiscoveredTuners);

        try
        {
            mDiscoveredTuners.clear();

            for(DiscoveredTuner discoveredTuner: discoveredTuners)
            {
                notifyRemoved(discoveredTuner);
            }
        }
        finally
        {
            mLock.unlock();
        }

        for(DiscoveredTuner discoveredTuner: discoveredTuners)
        {
            discoveredTuner.stop();
            discoveredTuner.removeTunerStatusListener(this);
        }
    }

    /**
     * Removes the Tuner from this model
     */
    public void removeDiscoveredTuner(DiscoveredTuner discoveredTuner)
    {
        mLog.info("Removing discovered tuner: " + discoveredTuner.getId());
        mLock.lock();

        try
        {
            if(mDiscoveredTuners.contains(discoveredTuner))
            {
                mDiscoveredTuners.remove(discoveredTuner);
                notifyRemoved(discoveredTuner);
                discoveredTuner.stop();
            }
        }
        catch(Exception e)
        {
            mLog.error("Unexpected error while shutting down discovered tuner", e);
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Indicates if the discovered tuner is still being managed by this tuner model and hasn't been removed.
     */
    public boolean hasTuner(DiscoveredTuner discoveredTuner)
    {
        return mDiscoveredTuners.contains(discoveredTuner);
    }

    /**
     * Indicates if this model currently has the discovered tuner that is plugged into the specified bus and port.
     * @param bus number
     * @param portAddress number
     * @return true if it already has the discovered device
     */
    public boolean hasUsbTuner(int bus, String portAddress)
    {
        boolean hasDevice;
        mLock.lock();

        try
        {
            hasDevice = mDiscoveredTuners.stream()
                    .filter(tuner -> tuner instanceof DiscoveredUSBTuner usbTuner && usbTuner.isAt(bus, portAddress))
                    .findFirst()
                    .isPresent();
        }
        finally
        {
            mLock.unlock();
        }

        return hasDevice;
    }

    /**
     * Removes the USB tuner at bus and port number
     * @param bus usb
     * @param portAddress usb
     */
    public DiscoveredTuner removeUsbTuner(int bus, String portAddress)
    {
        DiscoveredTuner removed = null;

        mLock.lock();

        try
        {
            DiscoveredTuner discoveredTuner = mDiscoveredTuners.stream()
                    .filter(tuner -> tuner instanceof DiscoveredUSBTuner usbTuner &&
                            usbTuner.isAt(bus, portAddress)).findFirst().orElse(null);

            if(discoveredTuner != null)
            {
                removeDiscoveredTuner(discoveredTuner);
                removed = discoveredTuner;
            }
        }
        finally
        {
            mLock.unlock();
        }

        return removed;
    }

    /**
     * Adds a tuner event listener
     */
    public void addListener(Listener<TunerEvent> listener)
    {
        mTunerEventListeners.add(listener);
    }

    /**
     * Removes a tuner event listener
     */
    public void removeListener(Listener<TunerEvent> listener)
    {
        mTunerEventListeners.remove(listener);
    }

    public void broadcast(TunerEvent event)
    {
        for(Listener<TunerEvent> listener : mTunerEventListeners)
        {
            listener.receive(event);
        }
    }

    @Override
    public void tunerStatusUpdated(DiscoveredTuner discoveredTuner, TunerStatus previous, TunerStatus current)
    {
        if(current == TunerStatus.ENABLED && discoveredTuner.hasTuner())
        {
            discoveredTuner.getTuner().addTunerEventListener(this);
            notifyUpdated(discoveredTuner);
            return;
        }
        else if(current == TunerStatus.DISABLED || current == TunerStatus.ERROR)
        {
            notifyUpdated(discoveredTuner);
            return;
        }

        if(current == TunerStatus.REMOVED)
        {
            mLog.info("Tuner removal detected - stopping and removing: " + discoveredTuner);

            //Note: RSPduo only gets device removal indication if the device is streaming.  There may be situation where
            //master only is streaming, or slave only is streaming.  Ensure we remove both devices when detected.

            //Special handling for RSPduo Tuner 1 configured as master - remove the slave tuner also
            if(discoveredTuner instanceof DiscoveredRspDuoTuner1 master1 &&
               master1.getDeviceSelectionMode().isMasterMode())
            {
                DiscoveredTuner slave2 = getDiscoveredTuner(master1.getSlaveId());

                if(slave2 != null)
                {
                    removeDiscoveredTuner(slave2);
                }

                removeDiscoveredTuner(discoveredTuner);
            }
            //Special handling for RSPduo Tuner 2 configured as slave - remove the master tuner also
            else if(discoveredTuner instanceof DiscoveredRspDuoTuner2 slave2 &&
                    slave2.getDeviceSelectionMode().isSlaveMode())
            {
                String masterId = slave2.getMasterId();

                removeDiscoveredTuner(slave2);

                DiscoveredTuner master1 = getDiscoveredTuner(masterId);

                if(master1 != null)
                {
                    removeDiscoveredTuner(master1);
                }
            }
            else
            {
                removeDiscoveredTuner(discoveredTuner);
            }
        }
    }

    @Override
    public void receive(TunerEvent event)
    {
        if(event.hasTuner())
        {
            mLock.lock();

            try
            {
                DiscoveredTuner matchingTuner = getDiscoveredTuner(event.getTuner());

                if(matchingTuner != null)
                {
                    switch(event.getEvent())
                    {
                        case UPDATE_CHANNEL_COUNT:
                        case UPDATE_FREQUENCY:
                        case UPDATE_LOCK_STATE:
                        case UPDATE_SAMPLE_RATE:
                        case NOTIFICATION_ERROR_STATE:
                            notifyUpdated(matchingTuner);
                            break;
                        case UPDATE_FREQUENCY_ERROR:
                            if(mTunerConfigurationManager != null)
                            {
                                mTunerConfigurationManager.updateTunerPPM(matchingTuner);
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
            finally
            {
                mLock.unlock();
            }
        }
        else
        {
            mLog.error("Got a tuner event without a tuner - " + event);
        }

        broadcast(event);
    }


    /**
     * Snapshot of the discovered tuners.
     */
    public List<DiscoveredTuner> getDiscoveredTuners()
    {
        return new ArrayList<>(mDiscoveredTuners);
    }

    /**
     * Adds a listener for tuners added to, removed from or updated in the model.  Listeners are invoked on the
     * calling thread, which can be any thread.
     */
    public void addModelListener(ModelListener listener)
    {
        mModelListeners.add(listener);
    }

    /**
     * Removes the model listener
     */
    public void removeModelListener(ModelListener listener)
    {
        mModelListeners.remove(listener);
    }

    private void notifyAdded(DiscoveredTuner discoveredTuner)
    {
        for(ModelListener listener: mModelListeners)
        {
            listener.tunerAdded(discoveredTuner);
        }
    }

    private void notifyRemoved(DiscoveredTuner discoveredTuner)
    {
        for(ModelListener listener: mModelListeners)
        {
            listener.tunerRemoved(discoveredTuner);
        }
    }

    private void notifyUpdated(DiscoveredTuner discoveredTuner)
    {
        for(ModelListener listener: mModelListeners)
        {
            listener.tunerUpdated(discoveredTuner);
        }
    }

    /**
     * Listener for changes to the discovered tuners in the model.
     */
    public interface ModelListener
    {
        /**
         * A tuner was added to the model
         */
        void tunerAdded(DiscoveredTuner discoveredTuner);

        /**
         * A tuner was removed from the model
         */
        void tunerRemoved(DiscoveredTuner discoveredTuner);

        /**
         * A tuner's status, frequency, sample rate, channel count or lock state changed
         */
        void tunerUpdated(DiscoveredTuner discoveredTuner);
    }

    public String getDiagnosticReport()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Discovered Tuner Model Diagnostic Report\n");

        List<DiscoveredTuner> tunersCopy = new ArrayList<>(mDiscoveredTuners);

        for(DiscoveredTuner tuner: tunersCopy)
        {
            sb.append("\n\n--------------- DISCOVERED TUNER --------------------\n\n");
            sb.append(tuner.getDiagnosticReport()).append("\n");
        }

        return sb.toString();
    }
}