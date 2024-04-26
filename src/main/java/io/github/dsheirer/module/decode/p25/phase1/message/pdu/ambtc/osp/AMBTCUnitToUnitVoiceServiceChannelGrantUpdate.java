/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.IServiceOptionsProvider;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25ExplicitChannel;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.block.UnconfirmedDataBlock;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;
import java.util.ArrayList;
import java.util.List;

public class AMBTCUnitToUnitVoiceServiceChannelGrantUpdate extends AMBTCMessage implements IFrequencyBandReceiver, IServiceOptionsProvider
{
    private static final int[] HEADER_SERVICE_OPTIONS = {64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] HEADER_TARGET_WACN = {72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] BLOCK_0_SOURCE_WACN = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};
    private static final int[] BLOCK_0_SOURCE_SYSTEM = {20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] BLOCK_0_SOURCE_ID = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48,
        49, 50, 51, 52, 53, 54, 55};
    private static final int[] BLOCK_0_TARGET_ADDRESS = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70,
        71, 72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] BLOCK_0_DOWNLINK_FREQUENCY_BAND = {80, 81, 82, 83};
    private static final int[] BLOCK_0_DOWNLINK_CHANNEL_NUMBER = {84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95};
    private static final int[] BLOCK_1_UPLINK_FREQUENCY_BAND = {0, 1, 2, 3};
    private static final int[] BLOCK_1_UPLINK_CHANNEL_NUMBER = {4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] BLOCK_1_TARGET_WACN = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};
    private static final int[] BLOCK_1_TARGET_SYSTEM = {28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] BLOCK_1_TARGET_ID = {40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56,
            57, 58, 59, 60, 61, 62, 63};

    private VoiceServiceOptions mVoiceServiceOptions;
    private APCO25FullyQualifiedRadioIdentifier mSourceAddress;
    private APCO25FullyQualifiedRadioIdentifier mTargetAddress;
    private List<Identifier> mIdentifiers;
    private APCO25Channel mChannel;
    private List<IChannelDescriptor> mChannels;

    public AMBTCUnitToUnitVoiceServiceChannelGrantUpdate(PDUSequence PDUSequence, int nac, long timestamp)
    {
        super(PDUSequence, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        if(getSourceAddress() != null)
        {
            sb.append(" FM:").append(getSourceAddress());
        }
        if(getTargetAddress() != null)
        {
            sb.append(" TO:").append(getTargetAddress());
        }
        if(getChannel() != null)
        {
            sb.append(" CHAN:").append(getChannel());
        }
        sb.append(" SERVICE OPTIONS:").append(getServiceOptions());
        return sb.toString();
    }

    public VoiceServiceOptions getServiceOptions()
    {
        if(mVoiceServiceOptions == null)
        {
            mVoiceServiceOptions = new VoiceServiceOptions(getHeader().getMessage().getInt(HEADER_SERVICE_OPTIONS));
        }

        return mVoiceServiceOptions;
    }

    public APCO25FullyQualifiedRadioIdentifier getSourceAddress()
    {
        if(mSourceAddress == null && hasDataBlock(0))
        {
            int localAddress = getHeader().getMessage().getInt(HEADER_ADDRESS);
            int wacn = getDataBlock(0).getMessage().getInt(BLOCK_0_SOURCE_WACN);
            int system = getDataBlock(0).getMessage().getInt(BLOCK_0_SOURCE_SYSTEM);
            int id = getDataBlock(0).getMessage().getInt(BLOCK_0_SOURCE_ID);
            mSourceAddress = APCO25FullyQualifiedRadioIdentifier.createFrom(localAddress, wacn, system, id);
        }

        return mSourceAddress;
    }

    public APCO25FullyQualifiedRadioIdentifier getTargetAddress()
    {
        if(mTargetAddress == null && hasDataBlock(0) && hasDataBlock(1))
        {
            int localAddress = getDataBlock(0).getMessage().getInt(BLOCK_0_TARGET_ADDRESS);
            int wacn = getHeader().getMessage().getInt(HEADER_TARGET_WACN) << 12;
            wacn += getDataBlock(1).getMessage().getInt(BLOCK_1_TARGET_WACN);
            int system = getDataBlock(1).getMessage().getInt(BLOCK_1_TARGET_SYSTEM);
            int id = getDataBlock(1).getMessage().getInt(BLOCK_1_TARGET_ID);
            mTargetAddress = APCO25FullyQualifiedRadioIdentifier.createFrom(localAddress, wacn, system, id);
        }

        return mTargetAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            if(getSourceAddress() != null)
            {
                mIdentifiers.add(getSourceAddress());
            }
            if(getTargetAddress() != null)
            {
                mIdentifiers.add(getTargetAddress());
            }
            if(getChannel() != null)
            {
                mIdentifiers.add(getChannel());
            }
        }

        return mIdentifiers;
    }

    public boolean isExtendedChannel()
    {
        return hasDataBlock(0) &&
            (getDataBlock(0).getMessage().getInt(BLOCK_0_DOWNLINK_CHANNEL_NUMBER) !=
                getDataBlock(0).getMessage().getInt(BLOCK_1_UPLINK_CHANNEL_NUMBER));
    }

    public APCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            if(hasDataBlock(0))
            {
                UnconfirmedDataBlock block0 = getDataBlock(0);

                if(isExtendedChannel() && hasDataBlock(1))
                {
                    UnconfirmedDataBlock block1 = getDataBlock(1);

                    mChannel = APCO25ExplicitChannel.create(block0.getMessage().getInt(BLOCK_0_DOWNLINK_FREQUENCY_BAND),
                        block0.getMessage().getInt(BLOCK_0_DOWNLINK_CHANNEL_NUMBER),
                        block1.getMessage().getInt(BLOCK_1_UPLINK_FREQUENCY_BAND),
                        block1.getMessage().getInt(BLOCK_1_UPLINK_CHANNEL_NUMBER));
                }
                else
                {
                    mChannel = APCO25Channel.create(block0.getMessage().getInt(BLOCK_0_DOWNLINK_FREQUENCY_BAND),
                        block0.getMessage().getInt(BLOCK_0_DOWNLINK_CHANNEL_NUMBER));
                }
            }

            if(mChannel == null)
            {
                mChannel = APCO25Channel.create(-1, 0);
            }
        }

        return mChannel;
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
