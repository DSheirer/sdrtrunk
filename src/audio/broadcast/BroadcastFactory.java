/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
package audio.broadcast;

import audio.broadcast.broadcastify.BroadcastifyConfiguration;
import audio.broadcast.broadcastify.BroadcastifyConfigurationEditor;
import audio.broadcast.icecast.IcecastHTTPConfiguration;
import audio.broadcast.icecast.IcecastHTTPBroadcaster;
import audio.broadcast.icecast.IcecastTCPBroadcaster;
import audio.broadcast.icecast.IcecastTCPConfiguration;
import audio.broadcast.shoutcast.v1.ShoutcastV1Configuration;
import audio.broadcast.shoutcast.v1.ShoutcastV1Broadcaster;
import audio.broadcast.shoutcast.v2.ShoutcastV2Configuration;
import audio.broadcast.shoutcast.v2.ShoutcastV2Broadcaster;
import audio.convert.IAudioConverter;
import audio.convert.MP3AudioConverter;
import controller.ThreadPoolManager;
import gui.editor.Editor;
import gui.editor.EmptyEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BroadcastFactory
{
    private final static Logger mLog = LoggerFactory.getLogger( BroadcastFactory.class );

    public static final int MP3_MONO_16_KHZ_BITRATE = 16;
    public static final boolean MP3_CONSTANT_BITRATE = false;
    public static final boolean MP3_VARIABLE_BITRATE = true;

    /**
     * Creates an audio streaming broadcaster for the configuration
     * @param threadPoolManager for creating an audio broadcasting connection thread
     * @param configuration describing the server and audio types
     * @return configured broadcaster or null
     */
    public static Broadcaster getBroadcaster(ThreadPoolManager threadPoolManager,
                                             BroadcastConfiguration configuration)
    {
        IAudioConverter converter = getAudioConverter(configuration);

        if(converter != null)
        {
            switch(configuration.getBroadcastServerType())
            {
                case BROADCASTIFY:
                    return new IcecastTCPBroadcaster(threadPoolManager,
                            (BroadcastifyConfiguration)configuration, converter);
                case ICECAST_HTTP:
                    return new IcecastHTTPBroadcaster(threadPoolManager,
                            (IcecastHTTPConfiguration)configuration, converter);
                case ICECAST_TCP:
                    return new IcecastTCPBroadcaster(threadPoolManager,
                            (IcecastTCPConfiguration)configuration, converter);
                case SHOUTCAST_V1:
                    return new ShoutcastV1Broadcaster(threadPoolManager,
                            (ShoutcastV1Configuration)configuration, converter);
                case SHOUTCAST_V2:
                    return new ShoutcastV2Broadcaster(threadPoolManager,
                            (ShoutcastV2Configuration)configuration, converter);
                case UNKNOWN:
                default:
                    mLog.info("Unrecognized broadcast configuration: " + configuration.getBroadcastFormat().name());
                    break;
            }
        }

        return null;
    }

    /**
     * Creates an audio convert to convert from 8 kHz PCM audio to the specified format
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
                mLog.info("Unrecognized broadcast format: " + configuration.getBroadcastFormat().name());
        }

        return null;
    }

    /**
     * Creates a broadcast configuration for the specified server type and format
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
                mLog.info("Unrecognized broadcast server type: " + serverType.name());
                break;
        }

        return null;
    }

    /**
     * Constructs an editor for the specified broadcast configuration
     * @param configuration to modify or view
     * @param broadcastModel model for broadcast configurations
     * @return an editor for the specified broadcast configuration
     */
    public static Editor<BroadcastConfiguration> getEditor(BroadcastConfiguration configuration,
                                                           BroadcastModel broadcastModel)
    {
        Editor<BroadcastConfiguration> editor;

        switch (configuration.getBroadcastServerType())
        {
            case BROADCASTIFY:
                 editor = new BroadcastifyConfigurationEditor(broadcastModel);
                break;
            default:
                editor = new EmptyEditor<BroadcastConfiguration>();
                break;
        }

        editor.setItem(configuration);

        return editor;
    }
}
