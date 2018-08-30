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
package io.github.dsheirer.module.decode.p25.message.tdu.lc;

import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.channel.APCO25Channel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.identifier.integer.node.APCO25System;
import io.github.dsheirer.identifier.integer.node.APCO25Wacn;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.reference.Service;

import java.util.ArrayList;
import java.util.List;

public class NetworkStatusBroadcast extends TDULinkControlMessage implements FrequencyBandReceiver
{
    public static final int[] WACN = {72, 73, 74, 75, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 112, 113, 114,
        115, 116, 117, 118, 119, 120, 121, 122, 123};
    public static final int[] SYSTEM = {136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147};
    public static final int[] FREQUENCY_BAND = {160, 161, 162, 163};
    public static final int[] CHANNEL_NUMBER = {164, 165, 166, 167, 168, 169, 170, 171, 184, 185, 186, 187};
    public static final int[] SYSTEM_SERVICE_CLASS = {188, 189, 190, 191, 192, 193, 194, 195};

    private IIdentifier mWACN;
    private IIdentifier mSystem;
    private IAPCO25Channel mChannel;

    public NetworkStatusBroadcast(TDULinkControlMessage source)
    {
        super(source);
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" NETWORK:" + getWACN());
        sb.append(" SYS:" + getSystem());
        sb.append(" CHAN:" + getChannel());
        sb.append(" " + Service.getServices(getSystemServiceClass()).toString());

        return sb.toString();
    }

    public IIdentifier getWACN()
    {
        if(mWACN == null)
        {
            mWACN = APCO25Wacn.create(mMessage.getInt(WACN));
        }

        return mWACN;
    }

    public IIdentifier getSystem()
    {
        if(mSystem == null)
        {
            mSystem = APCO25System.create(mMessage.getInt(SYSTEM));
        }

        return mSystem;
    }

    public IAPCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25Channel.create(mMessage.getInt(FREQUENCY_BAND), mMessage.getInt(CHANNEL_NUMBER));
        }

        return mChannel;
    }

    public int getSystemServiceClass()
    {
        return mMessage.getInt(SYSTEM_SERVICE_CLASS);
    }

    @Override
    public List<IAPCO25Channel> getChannels()
    {
        List<IAPCO25Channel> channels = new ArrayList<>();
        channels.add(getChannel());
        return channels;
    }
}
