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

package io.github.dsheirer.module.decode.p25.message.lc.motorola;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.channel.APCO25Channel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25PatchGroup;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;

import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Patch Group Voice Channel Update
 */
public class LCMotorolaPatchGroupVoiceChannelUpdate extends MotorolaLinkControlWord implements FrequencyBandReceiver
{
    private static final int[] UNKNOWN_1 = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] PATCH_GROUP = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] UNKNOWN_2 = {40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] UNKNOWN_3 = {48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] FREQUENCY_BAND = {56, 57, 58, 59};
    private static final int[] CHANNEL_NUMBER = {60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};

    private APCO25PatchGroup mPatchGroup;
    private IAPCO25Channel mChannel;
    private List<IIdentifier> mIdentifiers;
    private List<IAPCO25Channel> mChannels;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCMotorolaPatchGroupVoiceChannelUpdate(BinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MOTOROLA PATCH VOICE CHANNEL UPDATE");
        sb.append(" PATCH GROUP:").append(getPatchGroup());
        sb.append(" CHANNEL:").append(getChannel());
        sb.append(" UNK1:").append(getUnknownField1());
        sb.append(" UNK2:").append(getUnknownField2());
        sb.append(" UNK3:").append(getUnknownField3());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    public APCO25PatchGroup getPatchGroup()
    {
        if(mPatchGroup == null)
        {
            mPatchGroup = APCO25PatchGroup.create(getMessage().getInt(PATCH_GROUP));
        }

        return mPatchGroup;
    }

    public IAPCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25Channel.create(getMessage().getInt(FREQUENCY_BAND), getMessage().getInt(CHANNEL_NUMBER));
        }

        return mChannel;
    }

    public String getUnknownField1()
    {
        return getMessage().getHex(UNKNOWN_1, 2);
    }
    public String getUnknownField2()
    {
        return getMessage().getHex(UNKNOWN_2, 2);
    }
    public String getUnknownField3()
    {
        return getMessage().getHex(UNKNOWN_3, 2);
    }

    @Override
    public List<IIdentifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getPatchGroup());
        }

        return mIdentifiers;
    }

    @Override
    public List<IAPCO25Channel> getChannels()
    {
        if(mChannels == null)
        {
            mChannels = new ArrayList<>();
            mChannels.add(getChannel());
        }

        return mChannels;
    }
}
