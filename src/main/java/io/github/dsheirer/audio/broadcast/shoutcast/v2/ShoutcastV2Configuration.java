/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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
package io.github.dsheirer.audio.broadcast.shoutcast.v2;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastFormat;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class ShoutcastV2Configuration extends BroadcastConfiguration
{
    private IntegerProperty mStreamID = new SimpleIntegerProperty();
    private String mUserID;
    private int mBitRate = 16;
    private String mGenre;
    private String mURL;
    private boolean mPublic = true;

    public ShoutcastV2Configuration()
    {
        //No-arg JAXB constructor
        this(BroadcastFormat.MP3);
    }

    public ShoutcastV2Configuration(BroadcastFormat format)
    {
        super(format);
        mValid.bind(Bindings.and(Bindings.and(Bindings.isNotNull(mHost), Bindings.greaterThan(mPort, 0)),
            Bindings.greaterThan(mStreamID, -1)));
    }

    @Override
    public BroadcastConfiguration copyOf()
    {
        ShoutcastV2Configuration copy = new ShoutcastV2Configuration(getBroadcastFormat());

        //Broadcast Configuration Parameters
        copy.setName(getName());
        copy.setHost(getHost());
        copy.setPort(getPort());
        copy.setPassword(getPassword());
        copy.setDelay(getDelay());
        copy.setEnabled(false);

        //Shoutcast V2 Configuration Parameters
        copy.setStreamID(getStreamID());
        copy.setUserID(getUserID());
        copy.setBitRate(getBitRate());
        copy.setGenre(getGenre());
        copy.setURL(getURL());
        copy.setPublic(isPublic());

        return copy;
    }


    @Override
    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.SHOUTCAST_V2;
    }

    /**
     * Stream ID
     */
    @JacksonXmlProperty(isAttribute = true, localName = "stream_id")
    public int getStreamID()
    {
        return mStreamID.get();
    }

    /**
     * Sets the stream ID
     *
     * @param streamID numeric stream identifier between 1 - 2147483647
     */
    public void setStreamID(int streamID)
    {
        mStreamID.set(streamID);
    }

    /**
     * User ID
     */
    @JacksonXmlProperty(isAttribute = true, localName = "user_id")
    public String getUserID()
    {
        return mUserID;
    }

    /**
     * Sets the user ID
     *
     * @param userID
     */
    public void setUserID(String userID)
    {
        mUserID = userID;
    }

    /**
     * Bit rate in kilobits per second
     */
    @JacksonXmlProperty(isAttribute = true, localName = "bitrate")
    public int getBitRate()
    {
        return mBitRate;
    }

    /**
     * Bit rate
     *
     * @param bitRate in kilobits per second
     */
    public void setBitRate(int bitRate)
    {
        mBitRate = bitRate;
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
     * Sets the genre tag for this stream
     */
    public void setGenre(String genre)
    {
        mGenre = genre;
    }


    public boolean hasGenre()
    {
        return mGenre != null && !mGenre.isEmpty();
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
        return mURL != null && !mURL.isEmpty();
    }
}
