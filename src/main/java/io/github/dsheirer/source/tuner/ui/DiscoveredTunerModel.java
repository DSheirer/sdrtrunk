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
package io.github.dsheirer.source.tuner.ui;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerEvent;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.DiscoveredUSBTuner;
import io.github.dsheirer.source.tuner.manager.IDiscoveredTunerStatusListener;
import io.github.dsheirer.source.tuner.manager.TunerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;
import java.awt.EventQueue;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Model for discovered tuners
 */
public class DiscoveredTunerModel extends AbstractTableModel implements Listener<TunerEvent>,
        IDiscoveredTunerStatusListener
{
    private static final long serialVersionUID = 1L;
    private static final Logger mLog = LoggerFactory.getLogger(DiscoveredTunerModel.class);

    //Model columns
    public static final int COLUMN_TUNER_STATUS = 0;
    public static final int COLUMN_TUNER_CLASS = 1;
    public static final int COLUMN_TUNER_TYPE = 2;
    public static final int COLUMN_FREQUENCY = 3;
    public static final int COLUMN_CHANNEL_COUNT = 4;
    private static final String MHZ = " MHz";
    private static final String[] COLUMN_HEADERS = {"Status","Class", "Type", "Frequency", "Channels"};

    private List<DiscoveredTuner> mDiscoveredTuners = new CopyOnWriteArrayList<>();
    private List<Listener<TunerEvent>> mTunerEventListeners = new ArrayList<>();
    private DecimalFormat mFrequencyFormat = new DecimalFormat("0.00000");
    private Lock mLock = new ReentrantLock();


    /**
     * Constructs an instance
     */
    public DiscoveredTunerModel()
    {
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
                int index = mDiscoveredTuners.indexOf(discoveredTuner);
                EventQueue.invokeLater(() -> fireTableRowsInserted(index, index));
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

        try
        {
            List<DiscoveredTuner> discoveredTuners = new ArrayList<>(mDiscoveredTuners);

            for(DiscoveredTuner discoveredTuner: discoveredTuners)
            {
                if(discoveredTuner.hasTuner())
                {
                    discoveredTuner.getTuner().removeTunerEventListener(this);
                }

                removeDiscoveredTuner(discoveredTuner);
            }
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Removes the Tuner from this model
     */
    public void removeDiscoveredTuner(DiscoveredTuner discoveredTuner)
    {
        mLock.lock();

        try
        {
            if(mDiscoveredTuners.contains(discoveredTuner))
            {
                int index = mDiscoveredTuners.indexOf(discoveredTuner);
                mDiscoveredTuners.remove(discoveredTuner);
                EventQueue.invokeLater(() -> fireTableRowsDeleted(index, index));
                discoveredTuner.stop();
            }
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
     * @param port number
     * @return true if it already has the discovered device
     */
    public boolean hasUsbTuner(int bus, int port)
    {
        boolean hasDevice;
        mLock.lock();

        try
        {
            hasDevice = mDiscoveredTuners.stream()
                    .filter(tuner -> tuner instanceof DiscoveredUSBTuner usbTuner && usbTuner.isAt(bus, port))
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
     * @param port usb
     */
    public void removeUsbTuner(int bus, int port)
    {
        mLock.lock();

        try
        {
            DiscoveredTuner discoveredTuner = mDiscoveredTuners.stream()
                    .filter(tuner -> tuner instanceof DiscoveredUSBTuner usbTuner &&
                            usbTuner.isAt(bus, port)).findFirst().get();

            if(discoveredTuner != null)
            {
                removeDiscoveredTuner(discoveredTuner);
            }
        }
        finally
        {
            mLock.unlock();
        }
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

    /**
     * Requests to display the first tuner in this model.  Invoke this method
     * after all listeners have registered and tuners have been added to this
     * model, in order to inform the primary display to use the first tuner.
     */
    public void requestFirstTunerDisplay()
    {
//TODO: move this out of the model ... is not part of the scope of this model
//        SystemProperties properties = SystemProperties.getInstance();
//        boolean enabled = properties.get(SpectralDisplayPanel.SPECTRAL_DISPLAY_ENABLED, true);
//
//        if(enabled && mDiscoveredTuners.size() > 0)
//        {
//            //Hack: the airspy tuner would lockup aperiodically and refuse to produce
//            //transfer buffers ... delaying registering for buffers for 500 ms seems
//            //to allow the airspy to stabilize before we start asking for samples.
//            ThreadPool.SCHEDULED.schedule(new Runnable()
//            {
//                @Override
//                public void run()
//                {
//                    broadcast(new TunerEvent(mDiscoveredTuners.get(0), Event.REQUEST_MAIN_SPECTRAL_DISPLAY));
//                }
//            }, 500, TimeUnit.MILLISECONDS);
//        }
//        else
//        {
//            broadcast(new TunerEvent(null, Event.CLEAR_MAIN_SPECTRAL_DISPLAY));
//        }
    }

    @Override
    public void tunerStatusUpdated(DiscoveredTuner discoveredTuner, TunerStatus previous, TunerStatus current)
    {
        if(current == TunerStatus.ENABLED && discoveredTuner.hasTuner())
        {
            discoveredTuner.getTuner().addTunerEventListener(this);
        }

        int row = mDiscoveredTuners.indexOf(discoveredTuner);
        EventQueue.invokeLater(() -> fireTableRowsUpdated(row, row));
    }

    @Override
    public void receive(TunerEvent event)
    {
        if(event.hasTuner())
        {
            mLock.lock();

            try
            {
                DiscoveredTuner matching = getDiscoveredTuner(event.getTuner());
                int index = mDiscoveredTuners.indexOf(matching);

                if(index >= 0)
                {
                    switch(event.getEvent())
                    {
                        case UPDATE_CHANNEL_COUNT:
                            EventQueue.invokeLater(() -> fireTableCellUpdated(index, COLUMN_CHANNEL_COUNT));
                            break;
                        case UPDATE_FREQUENCY:
                            EventQueue.invokeLater(() -> fireTableCellUpdated(index, COLUMN_FREQUENCY));
                            break;
                        case NOTIFICATION_ERROR_STATE:
                            EventQueue.invokeLater(() -> fireTableRowsUpdated(index, index));
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

    @Override
    public int getRowCount()
    {
        return mDiscoveredTuners.size();
    }

    @Override
    public int getColumnCount()
    {
        return COLUMN_HEADERS.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        if(rowIndex < mDiscoveredTuners.size())
        {
            DiscoveredTuner discoveredTuner = mDiscoveredTuners.get(rowIndex);

            switch(columnIndex)
            {
                case COLUMN_TUNER_STATUS:
                    return discoveredTuner.getTunerStatus();
                case COLUMN_TUNER_CLASS:
                    return discoveredTuner.getTunerClass().toString();
                case COLUMN_TUNER_TYPE:
                    if(discoveredTuner.hasTuner())
                    {
                        return discoveredTuner.getTuner().getTunerType().getLabel();
                    }
                    else
                    {
                        return "";
                    }
//                case COLUMN_TUNER_ID:
//                    if(discoveredTuner.hasTuner())
//                    {
//                        return discoveredTuner.getTuner().getUniqueID();
//                    }
//                    else
//                    {
//                        return discoveredTuner.getId();
//                    }
//                case COLUMN_SAMPLE_RATE:
//                    if(discoveredTuner.hasTuner())
//                    {
//                        double sampleRate = discoveredTuner.getTuner().getTunerController().getSampleRate();
//                        return mSampleRateFormat.format(sampleRate / 1E6D) + MHZ;
//                    }
//                    else
//                    {
//                        return "";
//                    }
                case COLUMN_FREQUENCY:
                    if(discoveredTuner.hasTuner())
                    {
                        long frequency = discoveredTuner.getTuner().getTunerController().getFrequency();
                        return mFrequencyFormat.format(frequency / 1E6D) + MHZ;
                    }
                    else
                    {
                        return "";
                    }
                case COLUMN_CHANNEL_COUNT:
                    if(discoveredTuner.hasTuner())
                    {
                        int channelCount = discoveredTuner.getTuner().getChannelSourceManager().getTunerChannelCount();
                        return channelCount + " (" + (discoveredTuner.getTuner().getTunerController().isLocked() ? "LOCKED)" : "UNLOCKED)");
                    }
                    else
                    {
                        return "";
                    }
//                case COLUMN_FREQUENCY_ERROR:
//                    if(discoveredTuner.hasTuner())
//                    {
//                        double ppm = discoveredTuner.getTuner().getTunerController().getFrequencyCorrection();
//                        return mFrequencyErrorPPMFormat.format(ppm);
//                    }
//                    else
//                    {
//                        return "";
//                    }
//                case COLUMN_MEASURED_FREQUENCY_ERROR:
//                    if(discoveredTuner.hasTuner())
//                    {
//                        if(discoveredTuner.getTuner().getTunerController().hasMeasuredFrequencyError())
//                        {
//                            StringBuilder sb = new StringBuilder();
//                            sb.append(discoveredTuner.getTuner().getTunerController().getMeasuredFrequencyError());
//                            sb.append("Hz (");
//                            sb.append(mFrequencyErrorPPMFormat.format(discoveredTuner.getTuner().getTunerController().getPPMFrequencyError()));
//                            sb.append("ppm)");
//                            return sb.toString();
//                        }
//                    }
//                    return "";
//                case COLUMN_ERROR_OR_SPECTRAL_DISPLAY_NEW:
//                    if(discoveredTuner.hasErrorMessage())
//                    {
//                        return discoveredTuner.getErrorMessage();
//                    }
//                    else if(discoveredTuner.hasTuner())
//                    {
//                        return "New";
//                    }
//                    return "";
                default:
                    break;
            }
        }

        return null;
    }

    @Override
    public String getColumnName(int columnIndex)
    {
        return COLUMN_HEADERS[columnIndex];
    }
}