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
import io.github.dsheirer.module.decode.p25.identifier.APCO25Lra;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Rfss;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Site;
import io.github.dsheirer.module.decode.p25.identifier.APCO25System;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25ExplicitChannel;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.block.UnconfirmedDataBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * RFSS Status Broadcast
 *
 * TODO: this parser class is incomplete at the moment ...
 */
public class AMBTCRFSSStatusBroadcast extends AMBTCMessage implements IFrequencyBandReceiver
{

    private static final int[] HEADER_LRA = {24, 25, 26, 27, 28, 29, 30, 31};
    private static final int HEADER_ACTIVE_NETWORK_CONNECTION_TO_RFSS_CONTROLLER_FLAG = 35;
    private static final int[] HEADER_SYSTEM = {36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] BLOCK_0_RFSS = {0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] BLOCK_0_SITE = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] BLOCK_0_DOWNLINK_FREQUENCY_BAND = {16, 17, 18, 19};
    private static final int[] BLOCK_0_DOWNLINK_CHANNEL_NUMBER = {20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] BLOCK_0_UPLINK_FREQUENCY_BAND = {32, 33, 34, 35};
    private static final int[] BLOCK_0_UPLINK_CHANNEL_NUMBER = {36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};

    private Identifier mLra;
    private Identifier mSystem;
    private Identifier mRfss;
    private Identifier mSite;
    private IChannelDescriptor mChannel;
    private List<Identifier> mIdentifiers;
    private List<IChannelDescriptor> mChannels;

    public AMBTCRFSSStatusBroadcast(PDUSequence PDUSequence, int nac, long timestamp)
    {
        super(PDUSequence, nac, timestamp);

        setValid(hasDataBlock(0));
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" SYSTEM:").append(getSystem());
        sb.append(" SITE:").append(getSite());
        if(isActiveNetworkConnectionToRfssControllerSite())
        {
            sb.append(" ACTIVE NETWORK CONNECTION");
        }
        sb.append(" RFSS:").append(getRFSS());
        sb.append(" LRA:").append(getLRA());
        sb.append(" CHAN:").append(getChannel());
        return sb.toString();
    }

    public Identifier getLRA()
    {
        if(mLra == null)
        {
            mLra = APCO25Lra.create(getHeader().getMessage().getInt(HEADER_LRA));
        }

        return mLra;
    }

    public Identifier getSystem()
    {
        if(mSystem == null)
        {
            mSystem = APCO25System.create(getHeader().getMessage().getInt(HEADER_SYSTEM));
        }

        return mSystem;
    }

    public Identifier getRFSS()
    {
        if(mRfss == null && hasDataBlock(0))
        {
            mRfss = APCO25Rfss.create(getDataBlock(0).getMessage().getInt(BLOCK_0_RFSS));
        }

        return mRfss;
    }

    /**
     * Indicates if the site has an active network connection to the RFSS controller
     */
    public boolean isActiveNetworkConnectionToRfssControllerSite()
    {
        return getHeader().getMessage().get(HEADER_ACTIVE_NETWORK_CONNECTION_TO_RFSS_CONTROLLER_FLAG);
    }

    public Identifier getSite()
    {
        if(mSite == null && hasDataBlock(0))
        {
            mSite = APCO25Site.create(getDataBlock(0).getMessage().getInt(BLOCK_0_SITE));
        }

        return mSite;
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

                mChannel = APCO25ExplicitChannel.create(block.getMessage().getInt(BLOCK_0_DOWNLINK_FREQUENCY_BAND),
                    block.getMessage().getInt(BLOCK_0_DOWNLINK_CHANNEL_NUMBER),
                    block.getMessage().getInt(BLOCK_0_UPLINK_FREQUENCY_BAND),
                    block.getMessage().getInt(BLOCK_0_UPLINK_CHANNEL_NUMBER));
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
            if(getLRA() != null)
            {
                mIdentifiers.add(getLRA());
            }
            if(getSystem() != null)
            {
                mIdentifiers.add(getSystem());
            }
            if(getRFSS() != null)
            {
                mIdentifiers.add(getRFSS());
            }
            if(getSite() != null)
            {
                mIdentifiers.add(getSite());
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
