/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
 * ****************************************************************************
 */
package io.github.dsheirer.preference.network;

/**
 * Plain POJO representing a single heartbeat monitor configuration entry.
 */
public class HeartbeatEntry
{
    private boolean mEnabled = true;
    private String mChannelName = "";
    private int mSystemId;
    private int mSiteId;
    private String mKumaUrl = "";
    private String mPushUrl2 = "";
    private int mIntervalSeconds = 30;

    /**
     * Default constructor.
     */
    public HeartbeatEntry()
    {
    }

    /**
     * Copy constructor.
     * @param other entry to copy
     */
    public HeartbeatEntry(HeartbeatEntry other)
    {
        mEnabled = other.mEnabled;
        mChannelName = other.mChannelName;
        mSystemId = other.mSystemId;
        mSiteId = other.mSiteId;
        mKumaUrl = other.mKumaUrl;
        mPushUrl2 = other.mPushUrl2;
        mIntervalSeconds = other.mIntervalSeconds;
    }

    public boolean isEnabled()
    {
        return mEnabled;
    }

    public void setEnabled(boolean enabled)
    {
        mEnabled = enabled;
    }

    public String getChannelName()
    {
        return mChannelName;
    }

    public void setChannelName(String channelName)
    {
        mChannelName = channelName != null ? channelName : "";
    }

    public int getSystemId()
    {
        return mSystemId;
    }

    public void setSystemId(int systemId)
    {
        mSystemId = systemId;
    }

    public int getSiteId()
    {
        return mSiteId;
    }

    public void setSiteId(int siteId)
    {
        mSiteId = siteId;
    }

    public String getKumaUrl()
    {
        return mKumaUrl;
    }

    public void setKumaUrl(String kumaUrl)
    {
        mKumaUrl = kumaUrl != null ? kumaUrl : "";
    }

    public String getPushUrl2()
    {
        return mPushUrl2;
    }

    public void setPushUrl2(String pushUrl2)
    {
        mPushUrl2 = pushUrl2 != null ? pushUrl2 : "";
    }

    public int getIntervalSeconds()
    {
        return mIntervalSeconds;
    }

    public void setIntervalSeconds(int intervalSeconds)
    {
        mIntervalSeconds = intervalSeconds;
    }
}
