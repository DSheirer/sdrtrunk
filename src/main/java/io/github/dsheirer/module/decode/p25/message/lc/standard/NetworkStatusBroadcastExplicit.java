/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.p25.message.lc.standard;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.channel.APCO25ExplicitChannel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.identifier.integer.node.APCO25System;
import io.github.dsheirer.identifier.integer.node.APCO25Wacn;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.lc.LinkControlWord;

import java.util.ArrayList;
import java.util.List;

/**
 * Secondary control channel broadcast information.
 */
public class NetworkStatusBroadcastExplicit extends LinkControlWord implements FrequencyBandReceiver
{
    private static final int[] WACN = {8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};
    private static final int[] SYSTEM = {28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] DOWNLINK_FREQUENCY_BAND = {40, 41, 42, 43};
    private static final int[] DOWNLINK_CHANNEL_NUMBER = {44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] UPLINK_FREQUENCY_BAND = {56, 57, 58, 59};
    private static final int[] UPLINK_CHANNEL_NUMBER = {60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};

    private List<IIdentifier> mIdentifiers;
    private IIdentifier mWACN;
    private IIdentifier mSystem;
    private IAPCO25Channel mChannel;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public NetworkStatusBroadcastExplicit(BinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" WACN:").append(getWACN());
        sb.append(" SYSTEM:").append(getSystem());
        sb.append(" CHAN:" + getChannel());
        return sb.toString();
    }

    public IIdentifier getWACN()
    {
        if(mWACN == null)
        {
            mWACN = APCO25Wacn.create(getMessage().getInt(WACN));
        }

        return mWACN;
    }

    public IIdentifier getSystem()
    {
        if(mSystem == null)
        {
            mSystem = APCO25System.create(getMessage().getInt(SYSTEM));
        }

        return mSystem;
    }

    public IAPCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25ExplicitChannel.create(getMessage().getInt(DOWNLINK_FREQUENCY_BAND),
                    getMessage().getInt(DOWNLINK_CHANNEL_NUMBER), getMessage().getInt(UPLINK_FREQUENCY_BAND),
                    getMessage().getInt(UPLINK_CHANNEL_NUMBER));
        }

        return mChannel;
    }

    /**
     * List of identifiers contained in this message
     */
    @Override
    public List<IIdentifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getWACN());
            mIdentifiers.add(getSystem());
        }

        return mIdentifiers;
    }

    @Override
    public List<IAPCO25Channel> getChannels()
    {
        List<IAPCO25Channel> channels = new ArrayList<>();
        channels.add(getChannel());
        return channels;
    }
}