/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.APCO25System;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Wacn;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25ExplicitChannel;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.block.UnconfirmedDataBlock;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import io.github.dsheirer.module.decode.p25.reference.SystemServiceClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Network Status Broadcast
 *
 * TODO: this parser class is incomplete at the moment ...
 */
public class AMBTCNetworkStatusBroadcast extends AMBTCMessage implements IFrequencyBandReceiver
{

    //    private static final int[] HEADER_LRA = {24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] HEADER_SYSTEM = {36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    //    private static final int[] HEADER_RFSS = {64, 65, 66, 67, 68, 69, 70, 71};
    //    private static final int[] HEADER_SITE = {72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] BLOCK_0_WACN = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};
    private static final int[] BLOCK_0_DOWNLINK_FREQUENCY_BAND = {24, 25, 26, 27};
    private static final int[] BLOCK_0_DOWNLINK_CHANNEL_NUMBER = {28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] BLOCK_0_UPLINK_FREQUENCY_BAND = {40, 41, 42, 43};
    private static final int[] BLOCK_0_UPLINK_CHANNEL_NUMBER = {44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] BLOCK_0_SYSTEM_SERVICE_CLASS = {56, 57, 58, 59, 60, 61, 62, 63};

    private ScrambleParameters mScrambleParameters;
    private Identifier mWacn;
    private Identifier mSystem;
    private IChannelDescriptor mChannel;
    private List<Identifier> mIdentifiers;
    private List<IChannelDescriptor> mChannels;
    private SystemServiceClass mSystemServiceClass;

    public AMBTCNetworkStatusBroadcast(PDUSequence PDUSequence, int nac, long timestamp)
    {
        super(PDUSequence, nac, timestamp);

        setValid(hasDataBlock(0));
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" WACN:").append(getWacn());
        sb.append(" SYSTEM:").append(getSystem());
        sb.append(" CHAN:").append(getChannel());
        sb.append(" SERVICES:").append(getSystemServiceClass());
        return sb.toString();
    }

    public SystemServiceClass getSystemServiceClass()
    {
        if(mSystemServiceClass == null && hasDataBlock(0))
        {
            if(hasDataBlock(0))
            {
                mSystemServiceClass = new SystemServiceClass(getDataBlock(0).getMessage().getInt(BLOCK_0_SYSTEM_SERVICE_CLASS));
            }
            else
            {
                mSystemServiceClass = new SystemServiceClass(0);
            }
        }

        return mSystemServiceClass;
    }

    public ScrambleParameters getScrambleParameters()
    {
        if(mScrambleParameters == null)
        {
            int wacn = (int)getWacn().getValue();
            int system = (int)getSystem().getValue();
            int nac = (int)getNAC().getValue();

            mScrambleParameters = new ScrambleParameters(wacn, system, nac);
        }

        return mScrambleParameters;
    }

    public Identifier getSystem()
    {
        if(mSystem == null)
        {
            mSystem = APCO25System.create(getHeader().getMessage().getInt(HEADER_SYSTEM));
        }

        return mSystem;
    }

    public Identifier getWacn()
    {
        if(mWacn == null && hasDataBlock(0))
        {
            mWacn = APCO25Wacn.create(getDataBlock(0).getMessage().getInt(BLOCK_0_WACN));
        }

        return mWacn;
    }

    public boolean isExtendedChannel()
    {
        return hasDataBlock(0) &&
            (getDataBlock(0).getMessage().getInt(BLOCK_0_DOWNLINK_CHANNEL_NUMBER) !=
                getDataBlock(0).getMessage().getInt(BLOCK_0_UPLINK_CHANNEL_NUMBER));
    }

    /**
     * Control channel.
     */
    public IChannelDescriptor getChannel()
    {
        if(mChannel == null && hasDataBlock(0))
        {
            if(hasDataBlock(0))
            {
                UnconfirmedDataBlock block = getDataBlock(0);

                if(isExtendedChannel())
                {
                    mChannel = APCO25ExplicitChannel.create(block.getMessage().getInt(BLOCK_0_DOWNLINK_FREQUENCY_BAND),
                        block.getMessage().getInt(BLOCK_0_DOWNLINK_CHANNEL_NUMBER),
                        block.getMessage().getInt(BLOCK_0_UPLINK_FREQUENCY_BAND),
                        block.getMessage().getInt(BLOCK_0_UPLINK_CHANNEL_NUMBER));
                }
                else
                {
                    mChannel = APCO25Channel.create(block.getMessage().getInt(BLOCK_0_DOWNLINK_FREQUENCY_BAND),
                        block.getMessage().getInt(BLOCK_0_DOWNLINK_CHANNEL_NUMBER));
                }
            }
            else
            {
                mChannel = APCO25Channel.create(-1, 0);
            }
        }

        return mChannel;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            if(getWacn() != null)
            {
                mIdentifiers.add(getWacn());
            }
            if(getSystem() != null)
            {
                mIdentifiers.add(getSystem());
            }
        }

        return mIdentifiers;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        if(mChannels == null)
        {
            mChannels = new ArrayList<>();

            if(getChannel() != null)
            {
                mChannels.add(getChannel());
            }
        }

        return mChannels;
    }
}
