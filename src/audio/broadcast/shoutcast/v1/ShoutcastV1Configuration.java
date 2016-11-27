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
package audio.broadcast.shoutcast.v1;

import audio.broadcast.BroadcastConfiguration;
import audio.broadcast.BroadcastFormat;
import audio.broadcast.BroadcastServerType;
import audio.broadcast.icecast.IcecastTCPConfiguration;

public class ShoutcastV1Configuration extends BroadcastConfiguration
{
    private String mStreamName;
    private String mGenre;
    private boolean mPublic;
    private int mChannels;
    private int mBitRate;
    private String mURL;

    public ShoutcastV1Configuration()
    {
        //No-arg JAXB constructor
    }

    public ShoutcastV1Configuration(BroadcastFormat format)
    {
        super(format);
    }

    @Override
    public BroadcastConfiguration copyOf()
    {
        ShoutcastV1Configuration copy = new ShoutcastV1Configuration(getBroadcastFormat());

        //Broadcast Configuration Parameters
        copy.setName(getName());
        copy.setHost(getHost());
        copy.setPort(getPort());
        copy.setPassword(getPassword());
        copy.setDelay(getDelay());
        copy.setEnabled(false);

        //Icecast Configuration Parameters
        copy.setStreamName(getStreamName());
        copy.setGenre(getGenre());
        copy.setPublic(isPublic());
        copy.setChannels(getChannels());
        copy.setBitRate(getBitRate());
        copy.setURL(getURL());

        return copy;
    }

    @Override
    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.SHOUTCAST_V1;
    }

    /**
     * Stream name
     */
    public String getStreamName()
    {
        return mStreamName;
    }

    /**
     * Sets the stream name
     * @param name
     */
    public void setStreamName(String name)
    {
        mStreamName = name;
    }

    /**
     * Stream genre
     */
    public String getGenre()
    {
        return mGenre;
    }

    /**
     * Stream genre
     * @param genre
     */
    public void setGenre(String genre)
    {
        mGenre = genre;
    }

    /**
     * Public visibility of the broadcastAudio
     */
    public boolean isPublic()
    {
        return mPublic;
    }

    /**
     * Sets public visibility of the broadcastAudio
     * @param isPublic indicates if the broadcastAudio should be visible to the public
     */
    public void setPublic(boolean isPublic)
    {
        mPublic = isPublic;
    }

    /**
     * Number of audio channels in the broadcastAudio
     */
    public int getChannels()
    {
        return mChannels;
    }

    /**
     * Sets the number of audio channels in the broadcastAudio
     */
    public void setChannels(int channels)
    {
        mChannels = channels;
    }

    /**
     * Bit rate in bits per second
     */
    public int getBitRate()
    {
        return mBitRate;
    }

    /**
     * Bit rate
     * @param bitRate in samples per second
     */
    public void setBitRate(int bitRate)
    {
        mBitRate = bitRate;
    }

    /**
     * URL associated with the broadcastAudio where users can find additional details.
     */
    public String getURL()
    {
        return mURL;
    }

    /**
     * URL associated with the broadcastAudio where users can find additional details.
     * @param url
     */
    public void setURL(String url)
    {
        mURL = url;
    }
}
