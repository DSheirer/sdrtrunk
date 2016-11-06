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
package audio.broadcast.broadcastify;

import audio.broadcast.BroadcastFormat;
import audio.broadcast.BroadcastServerType;
import audio.broadcast.icecast.IcecastTCPConfiguration;

import javax.xml.bind.annotation.XmlAttribute;

public class BroadcastifyConfiguration extends IcecastTCPConfiguration
{
    public int mFeedID;

    public BroadcastifyConfiguration()
    {
        //No-arg constructor for JAXB
    }

    /**
     * Broadcastify configuration for Icecast 2.3.2 compatible servers
     *
     * @param format of output audio (MP3)
     */
    public BroadcastifyConfiguration(BroadcastFormat format)
    {
        super(format);

        setBitRate(16);
        setChannels(1);
        setSampleRate(8000);
    }

    @Override
    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.BROADCASTIFY;
    }

    @XmlAttribute(name="feed_id")
    public int getFeedID()
    {
        return mFeedID;
    }

    public void setFeedID(int feedID)
    {
        mFeedID = feedID;
    }
}
