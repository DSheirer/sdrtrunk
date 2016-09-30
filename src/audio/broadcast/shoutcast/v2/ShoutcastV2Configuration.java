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

public class ShoutcastV2Configuration extends BroadcastConfiguration
{
    private int mStreamID;
    private String mUserID;
    private int mBitRate;
    private String mStreamName;
    private String mStreamGenre;
    private String mURL;
    private boolean mPublic = true;


    public ShoutcastV2Configuration(BroadcastFormat format)
    {
        super(format);
    }

    @Override
    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.SHOUTCAST_V2;
    }

    /**
     * Stream ID
     */
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
    public String getStreamGenre()
    {
        return mStreamGenre;
    }

    /**
     * Sets the genre tag for this stream
     */
    public void setStreamGenre(String genre)
    {
        mStreamGenre = genre;
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
