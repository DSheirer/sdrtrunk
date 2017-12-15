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
package ua.in.smartjava.audio.broadcast;

import ua.in.smartjava.alias.AliasModel;
import ua.in.smartjava.audio.broadcast.broadcastify.BroadcastifyConfiguration;
import ua.in.smartjava.audio.broadcast.broadcastify.BroadcastifyConfigurationEditor;
import ua.in.smartjava.audio.broadcast.icecast.IcecastHTTPAudioBroadcaster;
import ua.in.smartjava.audio.broadcast.icecast.IcecastHTTPConfiguration;
import ua.in.smartjava.audio.broadcast.icecast.IcecastHTTPConfigurationEditor;
import ua.in.smartjava.audio.broadcast.icecast.IcecastTCPAudioBroadcaster;
import ua.in.smartjava.audio.broadcast.icecast.IcecastTCPConfiguration;
import ua.in.smartjava.audio.broadcast.icecast.IcecastTCPConfigurationEditor;
import ua.in.smartjava.audio.broadcast.shoutcast.v1.ShoutcastV1AudioBroadcaster;
import ua.in.smartjava.audio.broadcast.shoutcast.v1.ShoutcastV1Configuration;
import ua.in.smartjava.audio.broadcast.shoutcast.v1.ShoutcastV1ConfigurationEditor;
import ua.in.smartjava.audio.broadcast.shoutcast.v2.ShoutcastV2AudioBroadcaster;
import ua.in.smartjava.audio.broadcast.shoutcast.v2.ShoutcastV2Configuration;
import ua.in.smartjava.audio.broadcast.shoutcast.v2.ShoutcastV2ConfigurationEditor;
import ua.in.smartjava.audio.convert.IAudioConverter;
import ua.in.smartjava.audio.convert.ISilenceGenerator;
import ua.in.smartjava.audio.convert.MP3AudioConverter;
import ua.in.smartjava.audio.convert.MP3SilenceGenerator;
import ua.in.smartjava.gui.editor.Editor;
import ua.in.smartjava.gui.editor.EmptyEditor;
import ua.in.smartjava.icon.IconManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.in.smartjava.record.AudioRecorder;
import ua.in.smartjava.record.mp3.MP3Recorder;

import java.nio.file.Path;

public class BroadcastFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(BroadcastFactory.class);

    public static final int MP3_MONO_16_KHZ_BITRATE = 16;
    public static final boolean MP3_CONSTANT_BITRATE = false;

    /**
     * Creates an ua.in.smartjava.audio streaming broadcaster for the configuration
     *
     * @param configuration describing the server and ua.in.smartjava.audio types
     * @return configured broadcaster or null
     */
    public static AudioBroadcaster getBroadcaster(BroadcastConfiguration configuration)
    {
        if(configuration != null)
        {
            IAudioConverter converter = getAudioConverter(configuration);

            if(converter != null)
            {
                switch(configuration.getBroadcastServerType())
                {
                    case BROADCASTIFY:
                        return new IcecastTCPAudioBroadcaster((BroadcastifyConfiguration) configuration);
                    case ICECAST_TCP:
                        return new IcecastTCPAudioBroadcaster((IcecastTCPConfiguration) configuration);
                    case ICECAST_HTTP:
                        return new IcecastHTTPAudioBroadcaster((IcecastHTTPConfiguration) configuration);
                    case SHOUTCAST_V1:
                        return new ShoutcastV1AudioBroadcaster((ShoutcastV1Configuration) configuration);
                    case SHOUTCAST_V2:
                        return new ShoutcastV2AudioBroadcaster((ShoutcastV2Configuration) configuration);
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
     * Creates an ua.in.smartjava.audio convert to convert from 8 kHz PCM ua.in.smartjava.audio to the specified format
     *
     * @param configuration containing the requested output ua.in.smartjava.audio format
     * @return ua.in.smartjava.audio convert or null
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
     * @param format for the output ua.in.smartjava.audio format
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

    /**
     * Constructs an editor for the specified broadcastAudio configuration
     *
     * @param configuration to modify or view
     * @param broadcastModel model for broadcastAudio configurations
     * @return an editor for the specified broadcastAudio configuration
     */
    public static Editor<BroadcastConfiguration> getEditor(BroadcastConfiguration configuration,
                                                           BroadcastModel broadcastModel,
                                                           AliasModel aliasModel,
                                                           IconManager iconManager)
    {
        Editor<BroadcastConfiguration> editor;

        switch(configuration.getBroadcastServerType())
        {
            case BROADCASTIFY:
                editor = new BroadcastifyConfigurationEditor(broadcastModel, aliasModel, iconManager);
                break;
            case ICECAST_TCP:
                editor = new IcecastTCPConfigurationEditor(broadcastModel, aliasModel, iconManager);
                break;
            case ICECAST_HTTP:
                editor = new IcecastHTTPConfigurationEditor(broadcastModel, aliasModel, iconManager);
                break;
            case SHOUTCAST_V1:
                editor = new ShoutcastV1ConfigurationEditor(broadcastModel, aliasModel, iconManager);
                break;
            case SHOUTCAST_V2:
                editor = new ShoutcastV2ConfigurationEditor(broadcastModel, aliasModel, iconManager);
                break;
            default:
                editor = new EmptyEditor<BroadcastConfiguration>();
                break;
        }

        editor.setItem(configuration);

        return editor;
    }

    /**
     * Creates an ua.in.smartjava.audio recorder for the specified broadcastAudio format using the specified path output file name
     */
    public static AudioRecorder getAudioRecorder(Path path, BroadcastFormat broadcastFormat)
    {
        switch(broadcastFormat)
        {
            case MP3:
                return new MP3Recorder(path);
            default:
                mLog.debug("Unrecognized broadcastAudio format [" + broadcastFormat + "] cannot create ua.in.smartjava.audio recorder");
                return null;
        }
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
