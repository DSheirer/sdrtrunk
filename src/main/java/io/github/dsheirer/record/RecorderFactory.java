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

package io.github.dsheirer.record;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.decode.dmr.audio.DMRCallSequenceRecorder;
import io.github.dsheirer.module.decode.p25.audio.P25P1CallSequenceRecorder;
import io.github.dsheirer.module.decode.p25.audio.P25P2CallSequenceRecorder;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.record.binary.BinaryRecorder;
import io.github.dsheirer.record.wave.ComplexSamplesWaveRecorder;
import io.github.dsheirer.record.wave.IRecordingStatusListener;
import io.github.dsheirer.record.wave.NativeBufferWaveRecorder;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.util.StringUtils;
import io.github.dsheirer.util.TimeStamp;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating recorder modules.
 */
public class RecorderFactory
{
    public static final float BASEBAND_SAMPLE_RATE = 25000.0f; //Default sample rate - source can override

    /**
     * Creates recorder modules based on the channel configuration details
     * @param userPreferences
     * @param channel
     * @return
     */
    public static List<Module> getRecorders(UserPreferences userPreferences, Channel channel)
    {
        List<Module> recorderModules = new ArrayList<>();

        for(RecorderType recorderType: channel.getRecordConfiguration().getRecorders())
        {
            long frequency = getFrequency(channel);

            switch(recorderType)
            {
                case BASEBAND:
                    if(channel.isStandardChannel())
                    {
                        recorderModules.add(getBasebandRecorder(channel.toString(), frequency, userPreferences));
                    }
                    break;
                case DEMODULATED_BIT_STREAM:
                    if(channel.isStandardChannel() && channel.getDecodeConfiguration().getDecoderType().providesBitstream())
                    {
                        recorderModules.add(new BinaryRecorder(getRecordingBasePath(userPreferences),
                                StringUtils.replaceIllegalCharacters(channel.toString()),
                                channel.getDecodeConfiguration().getDecoderType().getProtocol(),
                                frequency));
                    }
                    break;
                case TRAFFIC_BASEBAND:
                    if(channel.isTrafficChannel())
                    {
                        recorderModules.add(getBasebandRecorder(channel.toString(), frequency, userPreferences));
                    }
                    break;
                case TRAFFIC_DEMODULATED_BIT_STREAM:
                    if(channel.isTrafficChannel() && channel.getDecodeConfiguration().getDecoderType().providesBitstream())
                    {
                        recorderModules.add(new BinaryRecorder(getRecordingBasePath(userPreferences),
                                StringUtils.replaceIllegalCharacters(channel.toString()),
                                channel.getDecodeConfiguration().getDecoderType().getProtocol(),
                                frequency));
                    }
                    break;
                case MBE_CALL_SEQUENCE:
                    if(channel.isStandardChannel() && channel.getSourceConfiguration() instanceof SourceConfigTuner)
                    {

                        switch(channel.getDecodeConfiguration().getDecoderType())
                        {
                            case DMR:
                                recorderModules.add(new DMRCallSequenceRecorder(userPreferences, frequency,
                                        channel.getSystem(), channel.getSite()));
                                break;
                            case P25_PHASE1:
                                recorderModules.add(new P25P1CallSequenceRecorder(userPreferences, frequency,
                                    channel.getSystem(), channel.getSite()));
                                break;
                            case P25_PHASE2:
                                recorderModules.add(new P25P2CallSequenceRecorder(userPreferences, frequency,
                                    channel.getSystem(), channel.getSite()));
                                break;
                        }
                    }
                    break;
                case TRAFFIC_MBE_CALL_SEQUENCE:
                    if(channel.isTrafficChannel() && channel.getSourceConfiguration() instanceof SourceConfigTuner)
                    {
                        switch(channel.getDecodeConfiguration().getDecoderType())
                        {
                            case DMR:
                                recorderModules.add(new DMRCallSequenceRecorder(userPreferences, frequency,
                                        channel.getSystem(), channel.getSite()));
                                break;
                            case P25_PHASE1:
                                recorderModules.add(new P25P1CallSequenceRecorder(userPreferences, frequency,
                                    channel.getSystem(), channel.getSite()));
                                break;
                            case P25_PHASE2:
                                recorderModules.add(new P25P2CallSequenceRecorder(userPreferences, frequency,
                                    channel.getSystem(), channel.getSite()));
                                break;
                        }
                    }
                    break;
            }
        }

        return recorderModules;
    }

    /**
     * Extract the frequency from the channel configuration
     * @param channel with a source configuration
     * @return frequency
     */
    private static long getFrequency(Channel channel)
    {
        if(channel.getSourceConfiguration() instanceof SourceConfigTuner)
        {
            return ((SourceConfigTuner)channel.getSourceConfiguration()).getFrequency();
        }
        else if(channel.getSourceConfiguration() instanceof SourceConfigTunerMultipleFrequency)
        {
            return ((SourceConfigTunerMultipleFrequency)channel.getSourceConfiguration()).getFrequencies().get(0);
        }

        return 0;
    }

    /**
     * Base path to recordings folder
     */
    public static Path getRecordingBasePath(UserPreferences userPreferences)
    {
        return userPreferences.getDirectoryPreference().getDirectoryRecording();
    }

    /**
     * Constructs a baseband recorder for use in a processing chain.
     */
    public static ComplexSamplesWaveRecorder getBasebandRecorder(String channelName, long frequency, UserPreferences userPreferences)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getRecordingBasePath(userPreferences));
        sb.append(File.separator);
        sb.append(TimeStamp.getTimeStamp("_"));
        sb.append("_");
        sb.append(frequency);
        sb.append("_");
        sb.append(StringUtils.replaceIllegalCharacters(channelName)).append("_baseband");

        return new ComplexSamplesWaveRecorder(BASEBAND_SAMPLE_RATE, sb.toString());
    }

    /**
     * Constructs a baseband recorder for use in a processing chain.
     */
    public static NativeBufferWaveRecorder getTunerRecorder(String channelName, UserPreferences userPreferences,
                                                            IRecordingStatusListener statusListener)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getRecordingBasePath(userPreferences));
        sb.append(File.separator).append(StringUtils.replaceIllegalCharacters(channelName)).append("_baseband");
        return new NativeBufferWaveRecorder(BASEBAND_SAMPLE_RATE, sb.toString(), statusListener);
    }
}
