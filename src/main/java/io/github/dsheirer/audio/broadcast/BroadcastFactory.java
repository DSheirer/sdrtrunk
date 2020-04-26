/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.audio.broadcast;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.broadcastify.BroadcastifyConfiguration;
import io.github.dsheirer.audio.broadcast.icecast.IcecastHTTPAudioBroadcaster;
import io.github.dsheirer.audio.broadcast.icecast.IcecastHTTPConfiguration;
import io.github.dsheirer.audio.broadcast.icecast.IcecastTCPAudioBroadcaster;
import io.github.dsheirer.audio.broadcast.icecast.IcecastTCPConfiguration;
import io.github.dsheirer.audio.broadcast.shoutcast.v1.ShoutcastV1AudioBroadcaster;
import io.github.dsheirer.audio.broadcast.shoutcast.v1.ShoutcastV1Configuration;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ShoutcastV2AudioBroadcaster;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ShoutcastV2Configuration;
import io.github.dsheirer.audio.convert.IAudioConverter;
import io.github.dsheirer.audio.convert.ISilenceGenerator;
import io.github.dsheirer.audio.convert.MP3AudioConverter;
import io.github.dsheirer.audio.convert.MP3SilenceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BroadcastFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(BroadcastFactory.class);

    public static final int MP3_MONO_16_KHZ_BITRATE = 16;
    public static final boolean MP3_CONSTANT_BITRATE = false;

    /**
     * Creates an audio streaming broadcaster for the configuration
     *
     * @param configuration describing the server and audio types
     * @return configured broadcaster or null
     */
    public static AudioBroadcaster getBroadcaster(BroadcastConfiguration configuration, AliasModel aliasModel)
    {
        if(configuration != null)
        {
            IAudioConverter converter = getAudioConverter(configuration);

            if(converter != null)
            {
                switch(configuration.getBroadcastServerType())
                {
                    case BROADCASTIFY:
                        return new IcecastTCPAudioBroadcaster((BroadcastifyConfiguration) configuration, aliasModel);
                    case ICECAST_TCP:
                        return new IcecastTCPAudioBroadcaster((IcecastTCPConfiguration) configuration, aliasModel);
                    case ICECAST_HTTP:
                        return new IcecastHTTPAudioBroadcaster((IcecastHTTPConfiguration) configuration, aliasModel);
                    case SHOUTCAST_V1:
                        return new ShoutcastV1AudioBroadcaster((ShoutcastV1Configuration) configuration, aliasModel);
                    case SHOUTCAST_V2:
                        return new ShoutcastV2AudioBroadcaster((ShoutcastV2Configuration) configuration, aliasModel);
                    case UNKNOWN:
                    default:
                        mLog.info("Unrecognized broadcastAudio configuration: " + configuration.getBroadcastFormat().name());
                        break;
                }
            }
        }

        return null;
    }

    /**
     * Creates an audio convert to convert from 8 kHz PCM audio to the specified format
     *
     * @param configuration containing the requested output audio format
     * @return audio convert or null
     */
    public static IAudioConverter getAudioConverter(BroadcastConfiguration configuration)
    {
        switch(configuration.getBroadcastFormat())
        {
            case MP3:
                return new MP3AudioConverter(MP3_MONO_16_KHZ_BITRATE, MP3_CONSTANT_BITRATE);
            default:
                mLog.info("Unrecognized broadcastAudio format: " + configuration.getBroadcastFormat().name());
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
            case BROADCASTIFY:
                return new BroadcastifyConfiguration(format);
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

    public static ISilenceGenerator getSilenceGenerator(BroadcastFormat format)
    {
        switch(format)
        {
            case MP3:
                return new MP3SilenceGenerator();
            default:
                throw new IllegalArgumentException("Unrecognized broadcast format [" + format +
                    "] can't create silence generator");
        }
    }
}
