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
package io.github.dsheirer.source;

import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.source.mixer.MixerManager;
import io.github.dsheirer.source.recording.RecordingSourceManager;
import io.github.dsheirer.source.tuner.TunerManager;
import io.github.dsheirer.source.tuner.TunerModel;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;

public class SourceManager
{
    private MixerManager mMixerManager;
    private RecordingSourceManager mRecordingSourceManager;
    private TunerManager mTunerManager;
    private TunerModel mTunerModel;

    public SourceManager(TunerModel tunerModel, SettingsManager settingsManager)
    {
        mTunerModel = tunerModel;
        mMixerManager = new MixerManager();
        mRecordingSourceManager = new RecordingSourceManager(settingsManager);
        mTunerManager = new TunerManager(mMixerManager, tunerModel);

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

    public MixerManager getMixerManager()
    {
        return mMixerManager;
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
                retVal = mMixerManager.getSource(config);
                break;
            case TUNER:
                retVal = mTunerModel.getSource((SourceConfigTuner) config, channelSpecification);
                break;
            case RECORDING:
                retVal = mRecordingSourceManager.getSource(config, channelSpecification);
            case NONE:
            default:
                break;
        }

        return retVal;
    }
}
