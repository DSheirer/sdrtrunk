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
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25ExplicitChannel;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Update detailing other users/channels that are active on the network.
 */
public class LCGroupVoiceChannelUpdateExplicit extends LinkControlWord implements IFrequencyBandReceiver
{
    public static final int[] SERVICE_OPTIONS = {16, 17, 18, 19, 20, 21, 22, 23};
    public static final int[] GROUP_ADDRESS = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    public static final int[] DOWNLINK_FREQUENCY_BAND = {40, 41, 42, 43};
    public static final int[] DOWNLINK_CHANNEL = {44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    public static final int[] UPLINK_FREQUENCY_BAND = {56, 57, 58, 59};
    public static final int[] UPLINK_CHANNEL = {60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};

    private VoiceServiceOptions mVoiceServiceOptions;
    private IChannelDescriptor mChannel;
    private List<IChannelDescriptor> mChannels;
    private Identifier mTalkgroup;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     */
    public LCGroupVoiceChannelUpdateExplicit(BinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" TALKGROUP:").append(getGroupAddress());
        sb.append(" ").append(getChannel());
        sb.append(" ").append(getVoiceServiceOptions());

        return sb.toString();
    }

    /**
     * Service options for this channel
     */
    public VoiceServiceOptions getVoiceServiceOptions()
    {
        if(mVoiceServiceOptions == null)
        {
            mVoiceServiceOptions = new VoiceServiceOptions(getMessage().getInt(SERVICE_OPTIONS));
        }

        return mVoiceServiceOptions;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getGroupAddress());
        }

        return mIdentifiers;
    }

    public IChannelDescriptor getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25ExplicitChannel.create(getMessage().getInt(DOWNLINK_FREQUENCY_BAND),
                    getMessage().getInt(DOWNLINK_CHANNEL), getMessage().getInt(UPLINK_FREQUENCY_BAND),
                    getMessage().getInt(UPLINK_CHANNEL));
        }

        return mChannel;
    }

    public Identifier getGroupAddress()
    {
        if(mTalkgroup == null)
        {
            mTalkgroup = APCO25ToTalkgroup.createGroup(getMessage().getInt(GROUP_ADDRESS));
        }

        return mTalkgroup;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        List<IChannelDescriptor> channels = new ArrayList<>();
        channels.add(getChannel());
        return channels;
    }
}
