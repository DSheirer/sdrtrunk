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
package io.github.dsheirer.audio.broadcast;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.broadcastify.BroadcastifyCallBroadcaster;
import io.github.dsheirer.audio.broadcast.broadcastify.BroadcastifyCallConfiguration;
import io.github.dsheirer.audio.broadcast.broadcastify.BroadcastifyFeedConfiguration;
import io.github.dsheirer.audio.broadcast.rdioscanner.RdioScannerBroadcaster;
import io.github.dsheirer.audio.broadcast.rdioscanner.RdioScannerConfiguration;
import io.github.dsheirer.audio.broadcast.rdioscanner.RdioScannerFeedConfiguration;
import io.github.dsheirer.audio.broadcast.openmhz.OpenMHzBroadcaster;
import io.github.dsheirer.audio.broadcast.openmhz.OpenMHzConfiguration;
import io.github.dsheirer.audio.broadcast.openmhz.OpenMHzFeedConfiguration;
import io.github.dsheirer.audio.broadcast.icecast.IcecastHTTPAudioBroadcaster;
import io.github.dsheirer.audio.broadcast.icecast.IcecastHTTPConfiguration;
import io.github.dsheirer.audio.broadcast.icecast.IcecastTCPAudioBroadcaster;
import io.github.dsheirer.audio.broadcast.icecast.IcecastTCPConfiguration;
import io.github.dsheirer.audio.broadcast.shoutcast.v1.ShoutcastV1AudioBroadcaster;
import io.github.dsheirer.audio.broadcast.shoutcast.v1.ShoutcastV1Configuration;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ShoutcastV2AudioStreamingBroadcaster;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ShoutcastV2Configuration;
import io.github.dsheirer.audio.convert.ISilenceGenerator;
import io.github.dsheirer.audio.convert.InputAudioFormat;
import io.github.dsheirer.audio.convert.MP3Setting;
import io.github.dsheirer.audio.convert.MP3SilenceGenerator;
import io.github.dsheirer.preference.UserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BroadcastFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(BroadcastFactory.class);

    /**
     * Creates an audio streaming broadcaster for the configuration
     *
     * @param configuration describing the server and audio types
     * @return configured broadcaster or null
     */
    public static AbstractAudioBroadcaster getBroadcaster(BroadcastConfiguration configuration, AliasModel aliasModel,
                                                          UserPreferences userPreferences)
    {
        if(configuration != null)
        {
            InputAudioFormat inputAudioFormat = userPreferences.getMP3Preference().getAudioSampleRate();
            MP3Setting mp3Setting = userPreferences.getMP3Preference().getMP3Setting();

            switch(configuration.getBroadcastServerType())
            {
                case BROADCASTIFY_CALL:
                    return new BroadcastifyCallBroadcaster((BroadcastifyCallConfiguration)configuration,
                            inputAudioFormat, mp3Setting, aliasModel);
                case RDIOSCANNER_CALL:
                    return new RdioScannerBroadcaster((RdioScannerConfiguration)configuration,
                            inputAudioFormat, mp3Setting, aliasModel);
                case OPENMHZ:
                    return new OpenMHzBroadcaster((OpenMHzConfiguration)configuration,
                        inputAudioFormat, mp3Setting, aliasModel);
                case BROADCASTIFY:
                    return new IcecastTCPAudioBroadcaster((BroadcastifyFeedConfiguration) configuration,
                            inputAudioFormat, mp3Setting, aliasModel);
                case ICECAST_TCP:
                    return new IcecastTCPAudioBroadcaster((IcecastTCPConfiguration) configuration, inputAudioFormat,
                            mp3Setting, aliasModel);
                case ICECAST_HTTP:
                    return new IcecastHTTPAudioBroadcaster((IcecastHTTPConfiguration) configuration, inputAudioFormat,
                            mp3Setting, aliasModel);
                case SHOUTCAST_V1:
                    return new ShoutcastV1AudioBroadcaster((ShoutcastV1Configuration) configuration, inputAudioFormat,
                            mp3Setting, aliasModel);
                case SHOUTCAST_V2:
                    return new ShoutcastV2AudioStreamingBroadcaster((ShoutcastV2Configuration) configuration,
                            inputAudioFormat, mp3Setting, aliasModel);
                case UNKNOWN:
                default:
                    mLog.info("Unrecognized broadcastAudio configuration: " + configuration.getBroadcastFormat().name());
                    break;
            }
        }

        return null;
    }

    /**
     * Creates a broadcastAudio configuration for the specified server type and format
     *
     * @param serverType for the configuration
     * @param format for the output audio format
     * @return constructed (empty) configuration
     */
    public static BroadcastConfiguration getConfiguration(BroadcastServerType serverType, BroadcastFormat format)
    {
        switch(serverType)
        {
            case BROADCASTIFY_CALL:
                return new BroadcastifyCallConfiguration(format);
            case RDIOSCANNER_CALL:
                return new RdioScannerConfiguration(format);
            case BROADCASTIFY:
                return new BroadcastifyFeedConfiguration(format);
            case OPENMHZ:
                return new OpenMHzConfiguration(format);
            case ICECAST_HTTP:
                return new IcecastHTTPConfiguration(format);
            case ICECAST_TCP:
                return new IcecastTCPConfiguration(format);
            case SHOUTCAST_V1:
                return new ShoutcastV1Configuration(format);
            case SHOUTCAST_V2:
                return new ShoutcastV2Configuration(format);
            case UNKNOWN:
            default:
                mLog.info("Unrecognized broadcastAudio server type: " + serverType.name());
                break;
        }

        return null;
    }

    public static ISilenceGenerator getSilenceGenerator(BroadcastFormat format, InputAudioFormat inputAudioFormat, MP3Setting mp3Setting)
    {
        switch(format)
        {
            case MP3:
                return new MP3SilenceGenerator(inputAudioFormat, mp3Setting);
            default:
                throw new IllegalArgumentException("Unrecognized broadcast format [" + format +
                    "] can't create silence generator");
        }
    }
}
