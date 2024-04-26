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
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;
import java.util.ArrayList;
import java.util.List;

/**
 * Group voice channel grant update - multiple users/channels - Implicit
 */
public class GroupVoiceChannelGrantUpdateMultipleImplicit extends MacStructureVoiceService implements IFrequencyBandReceiver
{
    private static final IntField FREQUENCY_BAND_1 = IntField.range(16, 19);
    private static final IntField CHANNEL_NUMBER_1 = IntField.range(20, 31);
    private static final IntField GROUP_ADDRESS_1 = IntField.range(32, 47);
    private static final IntField SERVICE_OPTIONS_2 = IntField.range(48, 55);
    private static final IntField FREQUENCY_BAND_2 = IntField.range(56, 59);
    private static final IntField CHANNEL_NUMBER_2 = IntField.range(60, 71);
    private static final IntField GROUP_ADDRESS_2 = IntField.range(72, 87);
    private static final IntField SERVICE_OPTIONS_3 = IntField.range(88, 95);
    private static final IntField FREQUENCY_BAND_3 = IntField.range(96, 99);
    private static final IntField CHANNEL_NUMBER_3 = IntField.range(100, 111);
    private static final IntField GROUP_ADDRESS_3 = IntField.range(112, 127);

    private List<Identifier> mIdentifiers;
    private Identifier mGroupAddress1;
    private APCO25Channel mChannel1;
    private VoiceServiceOptions mVoiceServiceOptions2;
    private Identifier mGroupAddress2;
    private APCO25Channel mChannel2;
    private VoiceServiceOptions mVoiceServiceOptions3;
    private Identifier mGroupAddress3;
    private APCO25Channel mChannel3;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public GroupVoiceChannelGrantUpdateMultipleImplicit(CorrectedBinaryMessage message, int offset)
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
        sb.append(" ").append(getServiceOptions1());

        if(hasGroup2())
        {
            sb.append(" GROUP-2:").append(getGroupAddress2());
            sb.append(" CHAN-2:").append(getChannel2());
            sb.append(" ").append(getServiceOptions2());
        }

        if(hasGroup3())
        {
            sb.append(" GROUP-3:").append(getGroupAddress3());
            sb.append(" CHAN-3:").append(getChannel3());
            sb.append(" ").append(getServiceOptions3());
        }

        return sb.toString();
    }

    /**
     * Indicates if this message contains a group address B and corresponding channel.
     */
    public boolean hasGroup2()
    {
        int group2 = getInt(GROUP_ADDRESS_2);
        return getInt(GROUP_ADDRESS_1) != group2 && group2 != 0;
    }

    /**
     * Indicates if this message contains a group address C and corresponding channel.
     */
    public boolean hasGroup3()
    {
        int group3 = getInt(GROUP_ADDRESS_3);
        return getInt(GROUP_ADDRESS_1) != group3 && getInt(GROUP_ADDRESS_2) != group3 && group3 != 0;
    }

    /**
     * Voice Service Options for call 1 (remapped from the getServiceOptions() from parent).
     */
    public VoiceServiceOptions getServiceOptions1()
    {
        return getServiceOptions();
    }

    public VoiceServiceOptions getServiceOptions2()
    {
        if(mVoiceServiceOptions2 == null)
        {
            mVoiceServiceOptions2 = new VoiceServiceOptions(getInt(SERVICE_OPTIONS_2));
        }

        return mVoiceServiceOptions2;
    }

    public VoiceServiceOptions getServiceOptions3()
    {
        if(mVoiceServiceOptions3 == null)
        {
            mVoiceServiceOptions3 = new VoiceServiceOptions(getInt(SERVICE_OPTIONS_3));
        }

        return mVoiceServiceOptions3;
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

    public APCO25Channel getChannel3()
    {
        if(mChannel3 == null)
        {
            mChannel3 = APCO25Channel.create(getInt(FREQUENCY_BAND_3), getInt(CHANNEL_NUMBER_3));
        }

        return mChannel3;
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

    /**
     * Talkgroup channel C
     */
    public Identifier getGroupAddress3()
    {
        if(mGroupAddress3 == null)
        {
            mGroupAddress3 = APCO25Talkgroup.create(getInt(GROUP_ADDRESS_3));
        }

        return mGroupAddress3;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getChannel1());
            mIdentifiers.add(getChannel2());
            mIdentifiers.add(getChannel3());
            mIdentifiers.add(getGroupAddress1());
            mIdentifiers.add(getGroupAddress2());
            mIdentifiers.add(getGroupAddress3());
        }

        return mIdentifiers;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        List<IChannelDescriptor> channels = new ArrayList<>();
        channels.add(getChannel1());
        channels.add(getChannel2());
        channels.add(getChannel3());
        return channels;
    }
}
