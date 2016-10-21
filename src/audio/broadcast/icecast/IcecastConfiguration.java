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

public class IcecastConfiguration extends BroadcastConfiguration
{
    private String mUserName;
    private String mMountPoint;
    private String mStreamName;
    private String mDescription;
    private String mGenre;
    private boolean mPublic;
    private int mChannels;
    private int mBitRate;
    private String mURL;

    public IcecastConfiguration(BroadcastFormat format)
    {
        super(format);
    }

    @Override
    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.ICECAST;
    }

    /**
     * Base64 encoded version of the username and password with prepended 'Basic ' tag.
     */
    public String getAuthorization()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getUserName()).append(":").append(getPassword());

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


    public String getDescription()
    {
        return mDescription;
    }

    public void setDescription(String description)
    {
        mDescription = description;
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
     * Public visibility of the broadcast
     */
    public boolean isPublic()
    {
        return mPublic;
    }

    /**
     * Sets public visibility of the broadcast
     * @param isPublic indicates if the broadcast should be visible to the public
     */
    public void setPublic(boolean isPublic)
    {
        mPublic = isPublic;
    }

    /**
     * Number of audio channels in the broadcast
     */
    public int getChannels()
    {
        return mChannels;
    }

    /**
     * Sets the number of audio channels in the broadcast
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
     * URL associated with the broadcast where users can find additional details.
     */
    public String getURL()
    {
        return mURL;
    }

    /**
     * URL associated with the broadcast where users can find additional details.
     * @param url
     */
    public void setURL(String url)
    {
        mURL = url;
    }
}
