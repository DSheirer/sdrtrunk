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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Base64;

public abstract class IcecastConfiguration extends BroadcastConfiguration
{
    protected String mUserName = "source";
    private String mMountPoint = "/stream";
    private String mDescription;
    private String mGenre;
    private boolean mPublic;
    private int mBitRate = 16;
    private int mChannels = 1;
    private int mSampleRate = 8000;
    private String mURL;

    public IcecastConfiguration(BroadcastFormat format)
    {
        super(format);
    }

    public IcecastConfiguration()
    {
    }

    /**
     * Base64 encoded version of the username and password with prepended 'Basic ' tag.
     */
    @XmlTransient
    public String getBase64EncodedCredentials()
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

    @XmlAttribute( name="user_name" )
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
    @XmlAttribute( name="mount_point" )
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
        if(mountPoint != null && !mountPoint.isEmpty())
        {
            if(mountPoint.startsWith("/"))
            {
                mMountPoint = mountPoint;
            }
            else
            {
                mMountPoint = "/" + mountPoint;
            }
        }
    }

    public boolean hasMountPoint()
    {
        return mMountPoint != null;
    }

    @XmlAttribute( name="description" )
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
    @XmlAttribute( name="genre" )
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
    @XmlAttribute( name="public" )
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
    @XmlAttribute( name="channels" )
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
    @XmlAttribute( name="bitrate" )
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

    @XmlAttribute( name="sample_rate" )
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
    @XmlAttribute( name="url" )
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

    @Override
    public boolean isValid()
    {
        if(!super.isValid())
        {
            return false;
        }
        else if(getUserName() == null)
        {
            return false;
        }
        else if(getMountPoint() == null)
        {
            return false;
        }
        else if(getChannels() != 1)
        {
            return false;
        }
        else if(getSampleRate() <= 0)
        {
            return false;
        }
        else if(getBitRate() <= 0)
        {
            return false;
        }

        return true;
    }
}
