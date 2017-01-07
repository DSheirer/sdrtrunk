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
package audio.broadcast.shoutcast.v2;

import audio.broadcast.BroadcastConfiguration;
import audio.broadcast.BroadcastFormat;
import audio.broadcast.BroadcastServerType;

import javax.xml.bind.annotation.XmlAttribute;

public class ShoutcastV2Configuration extends BroadcastConfiguration
{
    private int mStreamID;
    private String mUserID;
    private int mBitRate = 16;
    private String mGenre;
    private String mURL;
    private boolean mPublic = true;

    public ShoutcastV2Configuration()
    {
        //No-arg JAXB constructor
    }

    public ShoutcastV2Configuration(BroadcastFormat format)
    {
        super(format);
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
    @XmlAttribute(name = "stream_id")
    public int getStreamID()
    {
        return mStreamID;
    }

    /**
     * Sets the stream ID
     *
     * @param streamID numeric stream identifier between 1 - 2147483647
     */
    public void setStreamID(int streamID)
    {
        assert(1 <= streamID && streamID <= Integer.MAX_VALUE);
        mStreamID = streamID;
    }

    /**
     * User ID
     */
    @XmlAttribute(name = "user_id")
    public String getUserID()
    {
        return mUserID;
    }

    /**
     * Sets the user ID
     * @param userID
     */
    public void setUserID(String userID)
    {
        mUserID = userID;
    }

    /**
     * Bit rate in kilobits per second
     */
    @XmlAttribute(name = "bitrate")
    public int getBitRate()
    {
        return mBitRate;
    }

    /**
     * Bit rate
     * @param bitRate in kilobits per second
     */
    public void setBitRate(int bitRate)
    {
        mBitRate = bitRate;
    }

    /**
     * Stream genre
     */
    @XmlAttribute(name = "genre")
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
    @XmlAttribute(name = "public")
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
     * URL associated with the broadcastAudio where users can find additional details.
     */
    @XmlAttribute(name = "url")
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
        return mURL != null && !mURL.isEmpty();
    }

    @Override
    public boolean isValid()
    {
        return super.isValid() && hasName() &&
            getUserID() != null && !getUserID().isEmpty() &&
            1 <= getStreamID() &&
            getPassword() != null && !getPassword().isEmpty();
    }
}
