/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25.message.tsbk.osp.control;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.channel.APCO25Channel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.identifier.integer.node.APCO25Lra;
import io.github.dsheirer.identifier.integer.node.APCO25Rfss;
import io.github.dsheirer.identifier.integer.node.APCO25Site;
import io.github.dsheirer.identifier.integer.node.APCO25System;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.IAdjacentSite;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

import java.util.ArrayList;
import java.util.List;

public class AdjacentStatusBroadcast extends TSBKMessage implements FrequencyBandReceiver, IAdjacentSite
{
    public static final int[] LOCATION_REGISTRATION_AREA = {80, 81, 82, 83, 84, 85, 86, 87};
    public static final int CONVENTIONAL_CHANNEL_FLAG = 88;
    public static final int SITE_FAILURE_CONDITION_FLAG = 89;
    public static final int VALID_FLAG = 90;
    public static final int ACTIVE_NETWORK_CONNECTION_FLAG = 91;
    public static final int[] SYSTEM_ID = {92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103};
    public static final int[] RFSS_ID = {104, 105, 106, 107, 108, 109, 110, 111};
    public static final int[] SITE_ID = {112, 113, 114, 115, 116, 117, 118, 119};
    public static final int[] FREQUENCY_BAND = {120, 121, 122, 123};
    public static final int[] CHANNEL_NUMBER = {124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135};
    public static final int[] SYSTEM_SERVICE_CLASS = {136, 137, 138, 139, 140, 141, 142, 143};

    private IIdentifier mLRA;
    private IIdentifier mSystem;
    private IIdentifier mRFSS;
    private IIdentifier mSite;
    private IAPCO25Channel mChannel;

    public AdjacentStatusBroadcast(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" LRA:" + getLRAId());

        sb.append(" SYS:" + getSystemID());

        sb.append(" SITE:" + getRFSSId() + "-" + getSiteID());

        sb.append(" CHAN:" + getChannel());

        if(isConventionalChannel())
        {
            sb.append(" CONVENTIONAL");
        }

        if(isSiteFailureCondition())
        {
            sb.append(" FAILURE-CONDITION");
        }

        if(isValidSiteInformation())
        {
            sb.append(" VALID-INFO");
        }

        if(hasActiveNetworkConnection())
        {
            sb.append(" ACTIVE-NETWORK-CONN");
        }

        sb.append(getSystemServiceClass());

        return sb.toString();
    }

    public IIdentifier getLRAId()
    {
        if(mLRA == null)
        {
            mLRA = APCO25Lra.create(mMessage.getInt(LOCATION_REGISTRATION_AREA));
        }

        return mLRA;
    }

    public boolean isConventionalChannel()
    {
        return mMessage.get(CONVENTIONAL_CHANNEL_FLAG);
    }

    public boolean isSiteFailureCondition()
    {
        return mMessage.get(SITE_FAILURE_CONDITION_FLAG);
    }

    public boolean isValidSiteInformation()
    {
        return mMessage.get(VALID_FLAG);
    }

    public boolean hasActiveNetworkConnection()
    {
        return mMessage.get(ACTIVE_NETWORK_CONNECTION_FLAG);
    }

    public String getUniqueID()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getSystemID());
        sb.append(":");
        sb.append(getRFSSId());
        sb.append(":");
        sb.append(getSiteID());

        return sb.toString();
    }

    public IIdentifier getSystemID()
    {
        if(mSystem == null)
        {
            mSystem = APCO25System.create(mMessage.getInt(SYSTEM_ID));
        }

        return mSystem;
    }

    public IIdentifier getRFSSId()
    {
        if(mRFSS == null)
        {
            mRFSS = APCO25Rfss.create(mMessage.getInt(RFSS_ID));
        }

        return mRFSS;
    }

    public IIdentifier getSiteID()
    {
        if(mSite == null)
        {
            mSite = APCO25Site.create(mMessage.getInt(SITE_ID));
        }

        return mSite;
    }

    public IAPCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25Channel.create(mMessage.getInt(FREQUENCY_BAND), mMessage.getInt(CHANNEL_NUMBER));
        }

        return mChannel;
    }

    public String getSystemServiceClass()
    {
        return SystemService.toString(mMessage.getInt(SYSTEM_SERVICE_CLASS));
    }

    @Override
    public List<IAPCO25Channel> getChannels()
    {
        List<IAPCO25Channel> channels = new ArrayList<>();
        channels.add(getChannel());
        return channels;
    }
}
