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
package io.github.dsheirer.audio.broadcast.shoutcast.v1;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastFormat;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import javafx.beans.binding.Bindings;

public class ShoutcastV1Configuration extends BroadcastConfiguration
{
    private String mGenre;
    private String mDescription;
    private boolean mPublic;
    private int mChannels = 1;
    private int mBitRate = 16;

    //No-arg JAXB constructor
    public ShoutcastV1Configuration()
    {
        this(BroadcastFormat.MP3);
    }

    public ShoutcastV1Configuration(BroadcastFormat format)
    {
        super(format);
        mValid.bind(Bindings.and(Bindings.isNotNull(mHost), Bindings.greaterThan(mPort, 0)));
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
        copy.setGenre(getGenre());
        copy.setPublic(isPublic());
        copy.setChannels(getChannels());
        copy.setBitRate(getBitRate());

        return copy;
    }

    @Override
    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.SHOUTCAST_V1;
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

    /**
     * Stream description
     */
    @JacksonXmlProperty(isAttribute = true, localName = "description")
    public String getDescription()
    {
        return mDescription;
    }

    /**
     * Stream description
     *
     * @param description
     */
    public void setDescription(String description)
    {
        mDescription = description;
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
}
