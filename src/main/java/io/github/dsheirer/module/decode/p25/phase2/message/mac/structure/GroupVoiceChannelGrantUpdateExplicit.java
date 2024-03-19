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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25ExplicitChannel;
import io.github.dsheirer.module.decode.p25.identifier.channel.P25P2ExplicitChannel;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Group voice channel grant update - explicit channel format
 */
public class GroupVoiceChannelGrantUpdateExplicit extends MacStructureVoiceService implements IFrequencyBandReceiver
{
    private static final IntField TRANSMIT_FREQUENCY_BAND = IntField.range(16, 19);
    private static final IntField TRANSMIT_CHANNEL_NUMBER = IntField.range(20, 31);
    private static final IntField RECEIVE_FREQUENCY_BAND = IntField.range(32, 35);
    private static final IntField RECEIVE_CHANNEL_NUMBER = IntField.range(36, 47);
    private static final IntField GROUP_ADDRESS = IntField.length16(48);

    private List<Identifier> mIdentifiers;
    private Identifier mGroupAddress;
    private APCO25Channel mChannel;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public GroupVoiceChannelGrantUpdateExplicit(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getOpcode());
        sb.append(" GROUP:").append(getGroupAddress());
        sb.append(" CHAN:").append(getChannel());
        return sb.toString();
    }

    public APCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = new APCO25ExplicitChannel(new P25P2ExplicitChannel(getInt(TRANSMIT_FREQUENCY_BAND),
                getInt(TRANSMIT_CHANNEL_NUMBER), getInt(RECEIVE_FREQUENCY_BAND), getInt(RECEIVE_CHANNEL_NUMBER)));
        }

        return mChannel;
    }

    /**
     * Talkgroup channel A
     */
    public Identifier getGroupAddress()
    {
        if(mGroupAddress == null)
        {
            mGroupAddress = APCO25Talkgroup.create(getInt(GROUP_ADDRESS));
        }

        return mGroupAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getChannel());
            mIdentifiers.add(getGroupAddress());
        }

        return mIdentifiers;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        return Collections.singletonList(getChannel());
    }
}
