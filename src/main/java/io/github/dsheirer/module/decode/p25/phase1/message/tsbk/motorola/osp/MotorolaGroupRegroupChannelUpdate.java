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
package io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.patch.APCO25PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.OSPMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola group regroup channel update.
 */
public class MotorolaGroupRegroupChannelUpdate extends OSPMessage implements IFrequencyBandReceiver
{
    public static final int[] FREQUENCY_BAND_1 = {16, 17, 18, 19};
    public static final int[] CHANNEL_NUMBER_1 = {20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    public static final int[] PATCH_GROUP_1 = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    public static final int[] FREQUENCY_BAND_2 = {48, 49, 50, 51};
    public static final int[] CHANNEL_NUMBER_2 = {52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63};
    public static final int[] PATCH_GROUP_2 = {64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};

    private PatchGroupIdentifier mPatchGroup1;
    private PatchGroupIdentifier mPatchGroup2;
    private APCO25Channel mChannel1;
    private APCO25Channel mChannel2;
    private List<Identifier> mIdentifiers;

    public MotorolaGroupRegroupChannelUpdate(P25P1DataUnitID dataUnitID, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitID, message, nac, timestamp);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" PATCH GRP1:");
        sb.append(getPatchGroup1());
        sb.append(" CHAN 1:");
        sb.append(getChannel1());

        if(hasPatchGroup2())
        {
            sb.append(" GRP2:");
            sb.append(getPatchGroup2());
            sb.append(" CHAN 2:");
            sb.append(getChannel2());
        }

        return sb.toString();
    }

    public boolean hasPatchGroup2()
    {
        return (getMessage().getInt(PATCH_GROUP_1) != getMessage().getInt(PATCH_GROUP_2)) &&
            getMessage().getInt(PATCH_GROUP_2) != 0;
    }

    public PatchGroupIdentifier getPatchGroup1()
    {
        if(mPatchGroup1 == null)
        {
            mPatchGroup1 = APCO25PatchGroup.create(new PatchGroup(APCO25Talkgroup.create(getMessage().getInt(PATCH_GROUP_1))));
        }

        return mPatchGroup1;
    }

    public PatchGroupIdentifier getPatchGroup2()
    {
        if(mPatchGroup2 == null)
        {
            mPatchGroup2 = APCO25PatchGroup.create(new PatchGroup(APCO25Talkgroup.create(getMessage().getInt(PATCH_GROUP_2))));
        }

        return mPatchGroup2;
    }

    public APCO25Channel getChannel1()
    {
        if(mChannel1 == null)
        {
            mChannel1 = APCO25Channel.create(getMessage().getInt(FREQUENCY_BAND_1), getMessage().getInt(CHANNEL_NUMBER_1));
        }

        return mChannel1;
    }

    public APCO25Channel getChannel2()
    {
        if(hasPatchGroup2() && mChannel2 == null)
        {
            mChannel2 = APCO25Channel.create(getMessage().getInt(FREQUENCY_BAND_2), getMessage().getInt(CHANNEL_NUMBER_2));
        }

        return mChannel2;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        List<IChannelDescriptor> channels = new ArrayList<>();
        channels.add(getChannel1());
        if(hasPatchGroup2())
        {
            channels.add(getChannel2());
        }
        return channels;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getPatchGroup1());
            if(hasPatchGroup2())
            {
                mIdentifiers.add(getPatchGroup2());
            }
        }

        return mIdentifiers;
    }
}
