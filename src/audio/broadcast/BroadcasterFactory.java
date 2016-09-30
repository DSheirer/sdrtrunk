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

import audio.broadcast.shoutcast.v1.ShoutcastV1Configuration;
import audio.broadcast.shoutcast.v1.ShoutcastV1Handler;
import audio.broadcast.shoutcast.v2.ShoutcastV2Configuration;
import audio.broadcast.shoutcast.v2.ShoutcastV2Handler;
import audio.convert.IAudioConverter;
import audio.convert.MP3AudioConverter;
import controller.ThreadPoolManager;

public class BroadcasterFactory
{
    public static final int MP3_MONO_16_KHZ_BITRATE = 16;
    public static final boolean MP3_NO_VARIABLE_BITRATE = false;

    /**
     * Creates an audio streaming broadcaster for the configuration
     * @param threadPoolManager for creating an audio broadcasting connection thread
     * @param configuration describing the server and audio types
     * @return configured broadcaster or null
     */
    public static Broadcaster getBroadcaster(ThreadPoolManager threadPoolManager,
                                      BroadcastConfiguration configuration)
    {
        IAudioConverter converter = getAudioConverter(configuration.getBroadcastFormat());

        if(converter != null)
        {
            switch(configuration.getBroadcastServerType())
            {
                case BROADCASTIFY:
                    break;
                case ICECAST:
                    break;
                case SHOUTCAST_V1:
                    return new Broadcaster(threadPoolManager,
                            new ShoutcastV1Handler((ShoutcastV1Configuration)configuration, converter));
                case SHOUTCAST_V2:
                    return new Broadcaster(threadPoolManager,
                            new ShoutcastV2Handler((ShoutcastV2Configuration)configuration, converter));
                case UNKNOWN:
                    break;
            }
        }

        return null;
    }

    /**
     * Creates an audio convert to convert from 8 kHz PCM audio to the specified format
     * @param format to convert to
     * @return audio convert or null
     */
    public static IAudioConverter getAudioConverter(BroadcastFormat format)
    {
        switch(format)
        {
            case MP3:
                return new MP3AudioConverter(MP3_MONO_16_KHZ_BITRATE, MP3_NO_VARIABLE_BITRATE);
        }

        return null;
    }
}
