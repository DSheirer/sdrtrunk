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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.P25P2Channel;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Group voice channel grant update - multiple users/channels
 */
public class GroupVoiceChannelGrantUpdateMultiple extends MacStructure implements IFrequencyBandReceiver
{
    private static final int[] SERVICE_OPTIONS_A = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] FREQUENCY_BAND_A = {16, 17, 18, 19};
    private static final int[] CHANNEL_NUMBER_A = {20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] GROUP_ADDRESS_A = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};

    private static final int[] SERVICE_OPTIONS_B = {48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] FREQUENCY_BAND_B = {56, 57, 58, 59};
    private static final int[] CHANNEL_NUMBER_B = {60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] GROUP_ADDRESS_B = {72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87};

    private static final int[] SERVICE_OPTIONS_C = {88, 89, 90, 91, 92, 93, 94, 95};
    private static final int[] FREQUENCY_BAND_C = {96, 97, 98, 99};
    private static final int[] CHANNEL_NUMBER_C = {100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111};
    private static final int[] GROUP_ADDRESS_C = {112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125,
        126, 127};

    private List<Identifier> mIdentifiers;
    private VoiceServiceOptions mVoiceServiceOptionsA;
    private TalkgroupIdentifier mGroupAddressA;
    private APCO25Channel mChannelA;
    private VoiceServiceOptions mVoiceServiceOptionsB;
    private TalkgroupIdentifier mGroupAddressB;
    private APCO25Channel mChannelB;
    private VoiceServiceOptions mVoiceServiceOptionsC;
    private TalkgroupIdentifier mGroupAddressC;
    private APCO25Channel mChannelC;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public GroupVoiceChannelGrantUpdateMultiple(CorrectedBinaryMessage message, int offset)
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
        sb.append(" GROUP-A:").append(getGroupAddressA());
        sb.append(" CHAN-A:").append(getChannelA());
        sb.append(" ").append(getVoiceServiceOptionsA());

        if(hasGroupB())
        {
            sb.append(" GROUP-B:").append(getGroupAddressB());
            sb.append(" CHAN-B:").append(getChannelB());
            sb.append(" ").append(getVoiceServiceOptionsB());
        }

        if(hasGroupC())
        {
            sb.append(" GROUP-C:").append(getGroupAddressC());
            sb.append(" CHAN-C:").append(getChannelC());
            sb.append(" ").append(getVoiceServiceOptionsC());
        }

        return sb.toString();
    }

    /**
     * Indicates if this message contains a group address B and corresponding channel.
     */
    public boolean hasGroupB()
    {
        int groupB = getMessage().getInt(GROUP_ADDRESS_B, getOffset());
        return getMessage().getInt(GROUP_ADDRESS_A, getOffset()) != groupB && groupB != 0;
    }

    /**
     * Indicates if this message contains a group address C and corresponding channel.
     */
    public boolean hasGroupC()
    {
        int groupC = getMessage().getInt(GROUP_ADDRESS_C, getOffset());
        return getMessage().getInt(GROUP_ADDRESS_A, getOffset()) != groupC &&
            getMessage().getInt(GROUP_ADDRESS_B, getOffset()) != groupC && groupC != 0;
    }

    public VoiceServiceOptions getVoiceServiceOptionsA()
    {
        if(mVoiceServiceOptionsA == null)
        {
            mVoiceServiceOptionsA = new VoiceServiceOptions(getMessage().getInt(SERVICE_OPTIONS_A, getOffset()));
        }

        return mVoiceServiceOptionsA;
    }

    public VoiceServiceOptions getVoiceServiceOptionsB()
    {
        if(mVoiceServiceOptionsB == null)
        {
            mVoiceServiceOptionsB = new VoiceServiceOptions(getMessage().getInt(SERVICE_OPTIONS_B, getOffset()));
        }

        return mVoiceServiceOptionsB;
    }

    public VoiceServiceOptions getVoiceServiceOptionsC()
    {
        if(mVoiceServiceOptionsC == null)
        {
            mVoiceServiceOptionsC = new VoiceServiceOptions(getMessage().getInt(SERVICE_OPTIONS_C, getOffset()));
        }

        return mVoiceServiceOptionsC;
    }

    public APCO25Channel getChannelA()
    {
        if(mChannelA == null)
        {
            mChannelA = new APCO25Channel(new P25P2Channel(getMessage().getInt(FREQUENCY_BAND_A, getOffset()),
                getMessage().getInt(CHANNEL_NUMBER_A, getOffset())));
        }

        return mChannelA;
    }

    public APCO25Channel getChannelB()
    {
        if(mChannelB == null)
        {
            mChannelB = new APCO25Channel(new P25P2Channel(getMessage().getInt(FREQUENCY_BAND_B, getOffset()),
                getMessage().getInt(CHANNEL_NUMBER_B, getOffset())));
        }

        return mChannelB;
    }

    public APCO25Channel getChannelC()
    {
        if(mChannelC == null)
        {
            mChannelC = new APCO25Channel(new P25P2Channel(getMessage().getInt(FREQUENCY_BAND_C, getOffset()),
                getMessage().getInt(CHANNEL_NUMBER_C, getOffset())));
        }

        return mChannelC;
    }

    /**
     * Talkgroup channel A
     */
    public TalkgroupIdentifier getGroupAddressA()
    {
        if(mGroupAddressA == null)
        {
            mGroupAddressA = APCO25ToTalkgroup.createGroup(getMessage().getInt(GROUP_ADDRESS_A, getOffset()));
        }

        return mGroupAddressA;
    }

    /**
     * Talkgroup channel B
     */
    public TalkgroupIdentifier getGroupAddressB()
    {
        if(mGroupAddressB == null)
        {
            mGroupAddressB = APCO25ToTalkgroup.createGroup(getMessage().getInt(GROUP_ADDRESS_B, getOffset()));
        }

        return mGroupAddressB;
    }

    /**
     * Talkgroup channel C
     */
    public TalkgroupIdentifier getGroupAddressC()
    {
        if(mGroupAddressC == null)
        {
            mGroupAddressC = APCO25ToTalkgroup.createGroup(getMessage().getInt(GROUP_ADDRESS_C, getOffset()));
        }

        return mGroupAddressC;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getChannelA());
            mIdentifiers.add(getChannelB());
            mIdentifiers.add(getChannelC());
            mIdentifiers.add(getGroupAddressA());
            mIdentifiers.add(getGroupAddressB());
            mIdentifiers.add(getGroupAddressC());
        }

        return mIdentifiers;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        List<IChannelDescriptor> channels = new ArrayList<>();
        channels.add(getChannelA());
        channels.add(getChannelB());
        channels.add(getChannelC());
        return channels;
    }
}
