/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.source;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.source.mixer.MixerManager;
import io.github.dsheirer.source.recording.RecordingSourceManager;
import io.github.dsheirer.source.tuner.TunerManager;
import io.github.dsheirer.source.tuner.TunerModel;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import io.github.dsheirer.source.tuner.channel.MultiFrequencyTunerChannelSource;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;

public class SourceManager
{
    private RecordingSourceManager mRecordingSourceManager;
    private TunerManager mTunerManager;
    private TunerModel mTunerModel;

    public SourceManager(TunerModel tunerModel, SettingsManager settingsManager, UserPreferences userPreferences)
    {
        mTunerModel = tunerModel;
        mRecordingSourceManager = new RecordingSourceManager(settingsManager);
        mTunerManager = new TunerManager(tunerModel, userPreferences);

        //TODO: change mixer & recording managers to be models and hand them
        //in via the constructor.  Perform loading outside of this class.
    }

    /**
     * Prepare for shutdown and release all tuners
     */
    public void shutdown()
    {
        mTunerManager.releaseTuners();
        mTunerManager.dispose();
    }

    public RecordingSourceManager getRecordingSourceManager()
    {
        return mRecordingSourceManager;
    }

    public TunerManager getTunerManager()
    {
        return mTunerManager;
    }

    public TunerModel getTunerModel()
    {
        return mTunerModel;
    }

    public Source getSource(SourceConfiguration config, ChannelSpecification channelSpecification) throws SourceException
    {
        Source retVal = null;

        switch(config.getSourceType())
        {
            case MIXER:
                retVal = MixerManager.getSource(config);
                break;
            case TUNER:
                if(config instanceof SourceConfigTuner)
                {
                    SourceConfigTuner sourceConfigTuner = (SourceConfigTuner)config;
                    TunerChannel tunerChannel = sourceConfigTuner.getTunerChannel(channelSpecification.getBandwidth());
                    String preferredTuner = sourceConfigTuner.getPreferredTuner();
                    retVal = mTunerModel.getSource(tunerChannel, channelSpecification, preferredTuner);
                }
                break;
            case TUNER_MULTIPLE_FREQUENCIES:
                if(config instanceof SourceConfigTunerMultipleFrequency)
                {
                    SourceConfigTunerMultipleFrequency sourceConfigTuner = (SourceConfigTunerMultipleFrequency)config;
                    TunerChannel tunerChannel = sourceConfigTuner.getTunerChannel(channelSpecification.getBandwidth());
                    String preferredTuner = sourceConfigTuner.getPreferredTuner();

                    Source source = mTunerModel.getSource(tunerChannel, channelSpecification, preferredTuner);

                    if(source instanceof TunerChannelSource)
                    {
                        retVal = new MultiFrequencyTunerChannelSource(getTunerModel(), (TunerChannelSource)source,
                            sourceConfigTuner.getFrequencies(), channelSpecification, sourceConfigTuner.getPreferredTuner());
                    }
                }
                break;
            case RECORDING:
                retVal = mRecordingSourceManager.getSource(config, channelSpecification);
                break;
            case NONE:
            default:
                break;
        }

        return retVal;
    }
}
