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
package audio.broadcast.icecast;

import audio.broadcast.BroadcastConfiguration;
import audio.broadcast.BroadcastFormat;
import audio.broadcast.BroadcastServerType;

import java.util.Base64;

public class IcecastHTTPConfiguration extends BroadcastConfiguration
{
    private String mUserName;
    private String mMountPoint;
    private String mDescription;
    private String mGenre;
    private boolean mPublic;
    private int mBitRate;
    private int mChannels = 1;
    private int mSampleRate = 8000;
    private String mURL;

    public IcecastHTTPConfiguration()
    {
        //no-arg JAXB constructor
    }

    /**
     * Icecast 2.3.x and 2.4.x compatible configuration
     * @param format
     */
    public IcecastHTTPConfiguration(BroadcastFormat format)
    {
        super(format);
    }

    @Override
    public BroadcastConfiguration copyOf()
    {
        IcecastHTTPConfiguration copy = new IcecastHTTPConfiguration(getBroadcastFormat());

        //Broadcast Configuration Parameters
        copy.setName(getName());
        copy.setHost(getHost());
        copy.setPort(getPort());
        copy.setPassword(getPassword());
        copy.setDelay(getDelay());
        copy.setEnabled(false);

        //Icecast Configuration Parameters
        copy.setUserName(getUserName());
        copy.setMountPoint(getMountPoint());
        copy.setDescription(getDescription());
        copy.setGenre(getGenre());
        copy.setPublic(isPublic());
        copy.setBitRate(getBitRate());
        copy.setChannels(getChannels());
        copy.setSampleRate(getSampleRate());
        copy.setURL(getURL());

        return copy;
    }

    @Override
    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.ICECAST_HTTP;
    }

    /**
     * Base64 encoded version of the username and password with prepended 'Basic ' tag.
     */
    public String getEncodedCredentials()
    {
        StringBuilder sb = new StringBuilder();

        if(hasUserName())
        {
            sb.append(getUserName()).append(":").append(getPassword());
        }
        else
        {
            sb.append(getPassword());
        }

        String base64 = Base64.getEncoder().encodeToString(sb.toString().getBytes());

        return "Basic " + base64;
    }

    public String getUserName()
    {
        return mUserName;
    }

    public void setUserName(String userName)
    {
        mUserName = userName;
    }

    public boolean hasUserName()
    {
        return mUserName != null;
    }

    /**
     * Mount point or path to the stream
     * @return mount point
     */
    public String getMountPoint()
    {
        return mMountPoint;
    }

    /**
     * Sets the mount point (path) for the stream
     * @param mountPoint
     */
    public void setMountPoint(String mountPoint)
    {
        mMountPoint = mountPoint;
    }

    public boolean hasMountPoint()
    {
        return mMountPoint != null;
    }


    public String getDescription()
    {
        return mDescription;
    }

    public void setDescription(String description)
    {
        mDescription = description;
    }

    public boolean hasDescription()
    {
        return mDescription != null;
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

    public boolean hasGenre()
    {
        return mGenre != null;
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

    public boolean hasChannels()
    {
        return mChannels > 0;
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

    public boolean hasBitRate()
    {
        return mBitRate > 0;
    }

    public int getSampleRate()
    {
        return mSampleRate;
    }

    public void setSampleRate(int sampleRate)
    {
        mSampleRate = sampleRate;
    }

    public boolean hasSampleRate()
    {
        return mSampleRate > 0;
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

    public boolean hasURL()
    {
        return mURL != null;
    }
}
