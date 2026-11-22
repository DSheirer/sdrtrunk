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

package io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp;

import io.github.dsheirer.channel.IBandChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Wacn;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25ExplicitChannel;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.block.UnconfirmedDataBlock;
import io.github.dsheirer.module.decode.p25.reference.SystemServiceClass;

/**
 * Adjacent Status Broadcast - Uncoordinated Band Plan (opcode 0x3E), extended/multi-block (AMBTC) format.
 *
 * Announces a neighbor site that may belong to a different WACN/system (and band plan) than the current site.
 * Opcode 0x3E was previously defined as Protection Parameter Broadcast; current TIA-102.AABC revisions reassign it.
 *
 * Header: LRA (24-31), flags (32-35), SYSTEM (36-47), RFSS (64-71), SITE (72-79)  -- same as opcode 0x3C
 * Block 0: TRANSMIT CHANNEL (0-15), RECEIVE CHANNEL (16-31), SYSTEM SERVICE CLASS (32-39), WACN (40-59)
 *
 * A receive channel of 0xFFFF indicates that no separate receive channel is specified.
 */
public class AMBTCAdjacentStatusBroadcastUncoordinated extends AMBTCAdjacentStatusBroadcast
{
    private static final int[] BLOCK_0_DOWNLINK_FREQUENCY_BAND = {0, 1, 2, 3};
    private static final int[] BLOCK_0_DOWNLINK_CHANNEL_NUMBER = {4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] BLOCK_0_UPLINK_CHANNEL = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] BLOCK_0_UPLINK_FREQUENCY_BAND = {16, 17, 18, 19};
    private static final int[] BLOCK_0_UPLINK_CHANNEL_NUMBER = {20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] BLOCK_0_SYSTEM_SERVICE_CLASS = {32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] BLOCK_0_WACN = {40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
        58, 59};
    private static final int UNSPECIFIED_CHANNEL = 0xFFFF;

    private IBandChannelDescriptor mChannel;
    private Identifier mWacn;
    private SystemServiceClass mSystemServiceClass;

    public AMBTCAdjacentStatusBroadcastUncoordinated(PDUSequence PDUSequence, int nac, long timestamp)
    {
        super(PDUSequence, nac, timestamp);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" WACN:").append(getWacn());
        sb.append(" SYSTEM:").append(getSystem());
        sb.append(" LRA:").append(getLocationRegistrationArea());
        sb.append(" RFSS:").append(getRfss());
        sb.append(" SITE:").append(getSite());
        sb.append(" CHANNEL:").append(getChannel());
        sb.append(" SERVICES:").append(getSystemServiceClass());
        return sb.toString();
    }

    /**
     * WACN of the neighbor site
     */
    public Identifier getWacn()
    {
        if(mWacn == null && hasDataBlock(0))
        {
            mWacn = APCO25Wacn.create(getDataBlock(0).getMessage().getInt(BLOCK_0_WACN));
        }

        return mWacn;
    }

    /**
     * Services offered by the neighbor site
     */
    public SystemServiceClass getSystemServiceClass()
    {
        if(mSystemServiceClass == null)
        {
            mSystemServiceClass = new SystemServiceClass(hasDataBlock(0) ?
                getDataBlock(0).getMessage().getInt(BLOCK_0_SYSTEM_SERVICE_CLASS) : 0);
        }

        return mSystemServiceClass;
    }

    private boolean hasUplinkChannel()
    {
        return hasDataBlock(0) && getDataBlock(0).getMessage().getInt(BLOCK_0_UPLINK_CHANNEL) != UNSPECIFIED_CHANNEL;
    }

    @Override
    public boolean isExtendedChannel()
    {
        return hasUplinkChannel() && super.isExtendedChannel();
    }

    @Override
    public IBandChannelDescriptor getChannel()
    {
        if(mChannel == null)
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
}
