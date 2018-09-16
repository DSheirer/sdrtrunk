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

import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.Source;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.tuner.TunerEvent.Event;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationModel;
import io.github.dsheirer.spectrum.SpectralDisplayPanel;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TunerModel extends AbstractTableModel implements Listener<TunerEvent>
{
    private static final long serialVersionUID = 1L;

    private final static Logger mLog = LoggerFactory.getLogger(TunerModel.class);

    public static final int TUNER_TYPE = 0;
    public static final int TUNER_ID = 1;
    public static final int SAMPLE_RATE = 2;
    public static final int FREQUENCY = 3;
    public static final int CHANNEL_COUNT = 4;
    public static final int FREQUENCY_ERROR = 5;
    public static final int MEASURED_FREQUENCY_ERROR = 6;
    public static final int SPECTRAL_DISPLAY_NEW = 7;

    private static final String MHZ = " MHz";
    private static final String[] COLUMNS = {"Tuner", "ID", "Sample Rate", "Frequency", "Channels", "Current PPM",
        "Measured Error", "Display"};

    private List<Tuner> mTuners = new ArrayList<>();
    private List<Listener<TunerEvent>> mTunerEventListeners = new ArrayList<>();
    private DecimalFormat mFrequencyFormat = new DecimalFormat("0.00000");
    private DecimalFormat mSampleRateFormat = new DecimalFormat("0.000");
    private DecimalFormat mFrequencyErrorPPMFormat = new DecimalFormat("0.0");

    private TunerConfigurationModel mTunerConfigurationModel;

    public TunerModel(TunerConfigurationModel tunerConfigurationModel)
    {
        mTunerConfigurationModel = tunerConfigurationModel;
    }

    public TunerConfigurationModel getTunerConfigurationModel()
    {
        return mTunerConfigurationModel;
    }

    /**
     * List of Tuners currently in the model
     */
    public List<Tuner> getTuners()
    {
        return mTuners;
    }

    public Tuner getTuner(int index)
    {
        if(index < mTuners.size())
        {
            return mTuners.get(index);
        }

        return null;
    }

    /**
     * Find a tuner that matches the name argument
     *
     * @param name of the tuner
     * @return named tuner or null
     */
    public Tuner getTuner(String name)
    {
        if(name != null)
        {
            for(Tuner tuner : mTuners)
            {
                if(tuner.getName().equalsIgnoreCase(name))
                {
                    return tuner;
                }
            }
        }

        return null;
    }

    public void addTuners(List<Tuner> Tuners)
    {
        for(Tuner tuner : Tuners)
        {
            addTuner(tuner);
        }
    }

    /**
     * Adds the Tuner to this model
     */
    public void addTuner(Tuner tuner)
    {
        if(!mTuners.contains(tuner))
        {
            //Get the tuner configuration and apply it to the tuner - this
            //call should always produce a tuner configuration
            TunerConfiguration config = mTunerConfigurationModel
                .getTunerConfiguration(tuner.getTunerType(), tuner.getUniqueID());

            try
            {
                tuner.getTunerController().apply(config);

                mTuners.add(tuner);

                int index = mTuners.indexOf(tuner);

                fireTableRowsInserted(index, index);

                tuner.addTunerChangeListener(this);
            }
            catch(SourceException se)
            {
                mLog.error("Couldn't apply tuner configuration to tuner - ["
                    + tuner.getTunerType().name() + "] with id ["
                    + tuner.getUniqueID() + "] - tuner will not be included");
            }

        }
    }

    /**
     * Removes the Tuner from this model
     */
    public void removeTuner(Tuner tuner)
    {
        if(mTuners.contains(tuner))
        {
            tuner.removeTunerChangeListener(this);

            int index = mTuners.indexOf(tuner);

            mTuners.remove(tuner);

            fireTableRowsDeleted(index, index);
        }
    }

    public void addListener(Listener<TunerEvent> listener)
    {
        mTunerEventListeners.add(listener);
    }

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
        SystemProperties properties = SystemProperties.getInstance();
        boolean enabled = properties.get(SpectralDisplayPanel.SPECTRAL_DISPLAY_ENABLED, true);

        if(enabled && mTuners.size() > 0)
        {
            //Hack: the airspy tuner would lockup aperiodically and refuse to produce
            //transfer buffers ... delaying registering for buffers for 500 ms seems
            //to allow the airspy to stabilize before we start asking for samples.
            ThreadPool.SCHEDULED.schedule(new Runnable()
            {
                @Override
                public void run()
                {
                    broadcast(new TunerEvent(mTuners.get(0), Event.REQUEST_MAIN_SPECTRAL_DISPLAY));
                }
            }, 500, TimeUnit.MILLISECONDS);
        }
        else
        {
            broadcast(new TunerEvent(null, Event.CLEAR_MAIN_SPECTRAL_DISPLAY));
        }
    }

    @Override
    public void receive(TunerEvent event)
    {
        if(event.getTuner() != null)
        {
            int index = mTuners.indexOf(event.getTuner());

            if(index >= 0)
            {
                switch(event.getEvent())
                {
                    case CHANNEL_COUNT:
                        fireTableCellUpdated(index, CHANNEL_COUNT);
                        break;
                    case FREQUENCY_UPDATED:
                        fireTableCellUpdated(index, FREQUENCY);
                        mTunerConfigurationModel.tunerFrequencyChanged(event.getTuner());
                        break;
                    case FREQUENCY_ERROR_UPDATED:
                        fireTableCellUpdated(index, FREQUENCY_ERROR);
                        break;
                    case MEASURED_FREQUENCY_ERROR_UPDATED:
                        fireTableCellUpdated(index, MEASURED_FREQUENCY_ERROR);
                        break;
                    case SAMPLE_RATE_UPDATED:
                        fireTableCellUpdated(index, SAMPLE_RATE);
                        break;
                    case REQUEST_MAIN_SPECTRAL_DISPLAY:
                        fireTableCellUpdated(index, MEASURED_FREQUENCY_ERROR);
                    default:
                        break;
                }
            }
        }

        broadcast(event);
    }

    @Override
    public int getRowCount()
    {
        return mTuners.size();
    }

    @Override
    public int getColumnCount()
    {
        return COLUMNS.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        if(rowIndex < mTuners.size())
        {
            Tuner tuner = mTuners.get(rowIndex);

            switch(columnIndex)
            {
                case TUNER_TYPE:
                    return tuner.getTunerType().getLabel();
                case TUNER_ID:
                    return tuner.getUniqueID();
                case SAMPLE_RATE:
                    double sampleRate = tuner.getTunerController().getSampleRate();

                    return mSampleRateFormat.format(sampleRate / 1E6D) + MHZ;
                case FREQUENCY:
                    try
                    {
                        long frequency = tuner.getTunerController().getFrequency();

                        return mFrequencyFormat.format(frequency / 1E6D) + MHZ;
                    }
                    catch(Exception e)
                    {
                        return 0;
                    }
                case CHANNEL_COUNT:
                    int channelCount = tuner.getChannelSourceManager().getTunerChannelCount();
                    return channelCount + " (" + (tuner.getTunerController().isLocked() ? "LOCKED)" : "UNLOCKED)");
                case FREQUENCY_ERROR:
                    double ppm = tuner.getTunerController().getFrequencyCorrection();
                    return mFrequencyErrorPPMFormat.format(ppm);
                case MEASURED_FREQUENCY_ERROR:
                    if(tuner.getTunerController().hasMeasuredFrequencyError())
                    {
                        StringBuilder sb = new StringBuilder();
                        sb.append(tuner.getTunerController().getMeasuredFrequencyError());
                        sb.append("Hz (");
                        sb.append(mFrequencyErrorPPMFormat.format(tuner.getTunerController().getPPMFrequencyError()));
                        sb.append("ppm)");
                        return sb.toString();
                    }

                    return "";
                case SPECTRAL_DISPLAY_NEW:
                    return "New";
                default:
                    break;
            }
        }

        return null;
    }

    @Override
    public String getColumnName(int columnIndex)
    {
        return COLUMNS[columnIndex];
    }

    /**
     * Iterates current tuners to get a tuner channel source for the frequency
     * specified in the channel config's source config object.
     *
     * Returns null if no tuner can source the channel
     */
    public Source getSource(SourceConfigTuner config, ChannelSpecification channelSpecification)
    {
        TunerChannelSource retVal = null;

        TunerChannel tunerChannel = config.getTunerChannel();

        tunerChannel.setBandwidth(channelSpecification.getBandwidth());

        Iterator<Tuner> it = mTuners.iterator();

        Tuner tuner;

        if(config.hasPreferredTuner())
        {
            tuner = getTuner(config.getPreferredTuner());

            if(tuner != null)
            {
                try
                {
                    retVal = tuner.getChannelSourceManager().getSource(tunerChannel, channelSpecification);

                    if(retVal != null)
                    {
                        return retVal;
                    }
                }
                catch(Exception e)
                {
                    //Fall through to logger below
                }
            }

            mLog.info("Unable to source channel [" + config.getFrequency() + "] from preferred tuner [" +
                config.getPreferredTuner() + "] - searching for another tuner");
        }

        while(it.hasNext() && retVal == null)
        {
            tuner = it.next();

            try
            {
                retVal = tuner.getChannelSourceManager().getSource(tunerChannel, channelSpecification);
            }
            catch(Exception e)
            {
                mLog.error("error obtaining channel from tuner [" + tuner.getName() + "]", e);
            }
        }

        return retVal;
    }
}