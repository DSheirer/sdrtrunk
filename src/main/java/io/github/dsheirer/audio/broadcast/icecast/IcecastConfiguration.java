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
package io.github.dsheirer.audio.broadcast.icecast;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastFormat;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Base64;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = IcecastHTTPConfiguration.class, name="icecastHTTPConfiguration"),
    @JsonSubTypes.Type(value = IcecastTCPConfiguration.class, name="icecastTCPConfiguration"),
})
public abstract class IcecastConfiguration extends BroadcastConfiguration
{
    protected String mUserName = "source";
    protected StringProperty mMountPoint = new SimpleStringProperty("/stream");
    private String mDescription;
    private String mGenre;
    private boolean mPublic;
    private int mBitRate = 16;
    private int mChannels = 1;
    private int mSampleRate = 8000;
    private String mURL;
    private boolean mInline = true;

    public IcecastConfiguration(BroadcastFormat format)
    {
        super(format);
    }

    public IcecastConfiguration()
    {
        this(BroadcastFormat.MP3);
    }

    /**
     * Base64 encoded version of the username and password with prepended 'Basic ' tag.
     */
    @JsonIgnore
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

    @JacksonXmlProperty(isAttribute = true, localName = "user_name")
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
     *
     * @return mount point
     */
    @JacksonXmlProperty(isAttribute = true, localName = "mount_point")
    public String getMountPoint()
    {
        return mMountPoint.get();
    }

    /**
     * Sets the mount point (path) for the stream
     *
     * @param mountPoint
     */
    public void setMountPoint(String mountPoint)
    {
        if(mountPoint != null && !mountPoint.isEmpty())
        {
            if(mountPoint.startsWith("/"))
            {
                mMountPoint.setValue(mountPoint);
            }
            else
            {
                mMountPoint.setValue("/" + mountPoint);
            }
        }
    }

    public boolean hasMountPoint()
    {
        return mMountPoint.get() != null;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "description")
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
    @JacksonXmlProperty(isAttribute = true, localName = "genre")
    public String getGenre()
    {
        return mGenre;
    }

    /**
     * Stream genre
     *
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
    @JacksonXmlProperty(isAttribute = true, localName = "public")
    public boolean isPublic()
    {
        return mPublic;
    }

    /**
     * Sets public visibility of the broadcastAudio
     *
     * @param isPublic indicates if the broadcastAudio should be visible to the public
     */
    public void setPublic(boolean isPublic)
    {
        mPublic = isPublic;
    }

    /**
     * Number of audio channels in the broadcastAudio
     */
    @JacksonXmlProperty(isAttribute = true, localName = "channels")
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
    @JacksonXmlProperty(isAttribute = true, localName = "bitrate")
    public int getBitRate()
    {
        return mBitRate;
    }

    /**
     * Bit rate
     *
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

    @JacksonXmlProperty(isAttribute = true, localName = "sample_rate")
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
    @JacksonXmlProperty(isAttribute = true, localName = "url")
    public String getURL()
    {
        return mURL;
    }

    /**
     * URL associated with the broadcastAudio where users can find additional details.
     *
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

    /**
     * Control whether metadata is sent inline or out-of-band.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "inline")
    public boolean getInline()
    {
        return mInline;
    }

    @JsonIgnore
    public int getInlineInterval()
    {
        if (hasBitRate())
        {
            // Interval = BitRate * 1000 / 8 = 1 second
            return getBitRate() * 125;
        }
        return -1;
    }

    public void setInline(boolean inline)
    {
        mInline = inline;
    }

    public boolean hasInline()
    {
        // Bitrate must be known to calculate metadata interval
        return mInline != false && hasBitRate();
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
