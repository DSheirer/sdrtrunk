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
import io.github.dsheirer.module.decode.p25.IServiceOptionsProvider;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;
import java.util.ArrayList;
import java.util.List;

/**
 * Group voice channel grant update - Implicit
 */
public class GroupVoiceChannelGrantUpdateImplicit extends MacStructure implements IFrequencyBandReceiver, IServiceOptionsProvider
{
    private static final IntField FREQUENCY_BAND_1 = IntField.range(8, 11);
    private static final IntField CHANNEL_NUMBER_1 = IntField.range(12, 23);
    private static final IntField GROUP_ADDRESS_1 = IntField.range(24, 39);
    private static final IntField FREQUENCY_BAND_2 = IntField.range(40, 43);
    private static final IntField CHANNEL_NUMBER_2 = IntField.range(44, 55);
    private static final IntField GROUP_ADDRESS_2 = IntField.range(56, 71);

    private List<Identifier> mIdentifiers;
    private Identifier mGroupAddress1;
    private APCO25Channel mChannel1;
    private Identifier mGroupAddress2;
    private APCO25Channel mChannel2;

    //Empty, non-encrypted service options instance.
    private ServiceOptions mServiceOptions = new VoiceServiceOptions(0);

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public GroupVoiceChannelGrantUpdateImplicit(CorrectedBinaryMessage message, int offset)
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
        sb.append(" GROUP-1:").append(getGroupAddress1());
        sb.append(" CHAN-1:").append(getChannel1());

        if(hasGroup2())
        {
            sb.append(" GROUP-2:").append(getGroupAddress2());
            sb.append(" CHAN-2:").append(getChannel2());
        }
        return sb.toString();
    }

    @Override
    public ServiceOptions getServiceOptions()
    {
        return mServiceOptions;
    }

    /**
     * Indicates if this message contains talkgroup and channel information for a second talkgroup.
     * @return
     */
    public boolean hasGroup2()
    {
        int group2 = getInt(GROUP_ADDRESS_2);
        return group2 != 0 && group2 != getInt(GROUP_ADDRESS_1);
    }

    public APCO25Channel getChannel1()
    {
        if(mChannel1 == null)
        {
            mChannel1 = APCO25Channel.create(getInt(FREQUENCY_BAND_1), getInt(CHANNEL_NUMBER_1));
        }

        return mChannel1;
    }

    public APCO25Channel getChannel2()
    {
        if(mChannel2 == null)
        {
            mChannel2 = APCO25Channel.create(getInt(FREQUENCY_BAND_2), getInt(CHANNEL_NUMBER_2));
        }

        return mChannel2;
    }


    /**
     * Talkgroup channel A
     */
    public Identifier getGroupAddress1()
    {
        if(mGroupAddress1 == null)
        {
            mGroupAddress1 = APCO25Talkgroup.create(getInt(GROUP_ADDRESS_1));
        }

        return mGroupAddress1;
    }

    /**
     * Talkgroup channel B
     */
    public Identifier getGroupAddress2()
    {
        if(mGroupAddress2 == null)
        {
            mGroupAddress2 = APCO25Talkgroup.create(getInt(GROUP_ADDRESS_2));
        }

        return mGroupAddress2;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getChannel1());
            mIdentifiers.add(getGroupAddress1());
            if(hasGroup2())
            {
                mIdentifiers.add(getChannel2());
                mIdentifiers.add(getGroupAddress2());
            }
        }

        return mIdentifiers;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        List<IChannelDescriptor> channels = new ArrayList<>();
        channels.add(getChannel1());

        if(hasGroup2())
        {
            channels.add(getChannel2());
        }
        return channels;
    }
}
