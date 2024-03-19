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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.standard;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25ExplicitChannel;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.VoiceLinkControlMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Group voice channel update explicit
 */
public class LCGroupVoiceChannelUpdateExplicit extends VoiceLinkControlMessage implements IFrequencyBandReceiver
{
    private static final IntField GROUP_ADDRESS = IntField.length16(OCTET_3_BIT_24);
    private static final IntField DOWNLINK_FREQUENCY_BAND = IntField.length4(OCTET_5_BIT_40);
    private static final IntField DOWNLINK_CHANNEL = IntField.length12(OCTET_5_BIT_40 + 4);
    private static final IntField UPLINK_FREQUENCY_BAND = IntField.length4(OCTET_7_BIT_56);
    private static final IntField UPLINK_CHANNEL = IntField.length12(OCTET_7_BIT_56 + 4);
    private APCO25Channel mChannel;
    private Identifier mTalkgroup;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     */
    public LCGroupVoiceChannelUpdateExplicit(CorrectedBinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" TALKGROUP:").append(getGroupAddress());
        sb.append(" ").append(getChannel());
        sb.append(" ").append(getServiceOptions());

        return sb.toString();
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

    public APCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25ExplicitChannel.create(getInt(DOWNLINK_FREQUENCY_BAND), getInt(DOWNLINK_CHANNEL),
                    getInt(UPLINK_FREQUENCY_BAND), getInt(UPLINK_CHANNEL));
        }

        return mChannel;
    }

    public Identifier getGroupAddress()
    {
        if(mTalkgroup == null)
        {
            mTalkgroup = APCO25Talkgroup.create(getInt(GROUP_ADDRESS));
        }

        return mTalkgroup;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        return Collections.singletonList(getChannel());
    }
}
