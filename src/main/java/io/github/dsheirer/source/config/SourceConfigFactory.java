/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.source.config;

import io.github.dsheirer.source.SourceType;

public class SourceConfigFactory
{
    public static SourceConfiguration getDefaultSourceConfiguration()
    {
        return getSourceConfiguration(SourceType.TUNER);
    }

    public static SourceConfiguration getSourceConfiguration(SourceType source)
    {
        SourceConfiguration retVal;

        switch(source)
        {
            case TUNER:
                retVal = new SourceConfigTuner();
                break;
            case TUNER_MULTIPLE_FREQUENCIES:
                retVal = new SourceConfigTunerMultipleFrequency();
                break;
            case MIXER:
                retVal = new SourceConfigMixer();
                break;
            case NONE:
            default:
                retVal = new SourceConfigNone();
                break;
        }

        return retVal;
    }

    /**
     * Creates a copy of the configuration
     */
    public static SourceConfiguration copy(SourceConfiguration config)
    {
        if(config != null)
        {
            switch(config.getSourceType())
            {
                case MIXER:
                    SourceConfigMixer originalMixer = (SourceConfigMixer) config;
                    SourceConfigMixer copyMixer = new SourceConfigMixer();
                    copyMixer.setChannel(originalMixer.getChannel());
                    copyMixer.setMixer(originalMixer.getMixer());
                    return copyMixer;
                case RECORDING:
                    SourceConfigRecording originalRec = (SourceConfigRecording) config;
                    SourceConfigRecording copyRec = new SourceConfigRecording();
                    copyRec.setFrequency(originalRec.getFrequency());
                    copyRec.setRecordingAlias(originalRec.getRecordingAlias());
                    return copyRec;
                case TUNER:
                    SourceConfigTuner originalTuner = (SourceConfigTuner) config;
                    SourceConfigTuner copyTuner = new SourceConfigTuner();
                    copyTuner.setPreferredTuner(originalTuner.getPreferredTuner());
                    copyTuner.setFrequency(originalTuner.getFrequency());
                    return copyTuner;
                case TUNER_MULTIPLE_FREQUENCIES:
                    SourceConfigTunerMultipleFrequency originalMulti = (SourceConfigTunerMultipleFrequency) config;
                    SourceConfigTunerMultipleFrequency copyMulti = new SourceConfigTunerMultipleFrequency();
                    copyMulti.setPreferredTuner(originalMulti.getPreferredTuner());
                    copyMulti.setFrequencies(originalMulti.getFrequencies());
                    return copyMulti;
                case NONE:
                default:
                    return new SourceConfigNone();
            }
        }

        return null;
    }
}
