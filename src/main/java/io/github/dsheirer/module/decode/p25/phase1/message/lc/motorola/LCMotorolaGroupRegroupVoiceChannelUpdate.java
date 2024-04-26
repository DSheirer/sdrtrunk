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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.patch.APCO25PatchGroup;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.VoiceLinkControlMessage;
import java.util.Collections;
import java.util.List;

/**
 * Motorola Group Regroup Voice Channel Update
 */
public class LCMotorolaGroupRegroupVoiceChannelUpdate extends VoiceLinkControlMessage implements IFrequencyBandReceiver
{
    private static final IntField SUPER_GROUP = IntField.length16(OCTET_3_BIT_24);
    private static final IntField FREQUENCY_BAND = IntField.length4(OCTET_7_BIT_56);
    private static final IntField CHANNEL_NUMBER = IntField.length12(OCTET_7_BIT_56 + 4);
    private APCO25PatchGroup mSupergroupAddress;
    private APCO25Channel mChannel;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCMotorolaGroupRegroupVoiceChannelUpdate(CorrectedBinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MOTOROLA GROUP REGROUP VOICE CHANNEL UPDATE");
        sb.append(" SUPERGROUP:").append(getSupergroupAddress());
        sb.append(" CHANNEL:").append(getChannel());
        return sb.toString();
    }

    public APCO25PatchGroup getSupergroupAddress()
    {
        if(mSupergroupAddress == null)
        {
            PatchGroup patchGroup = new PatchGroup(APCO25Talkgroup.create(getInt(SUPER_GROUP)));
            mSupergroupAddress = APCO25PatchGroup.create(patchGroup);
        }

        return mSupergroupAddress;
    }

    public APCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25Channel.create(getInt(FREQUENCY_BAND), getInt(CHANNEL_NUMBER));
        }

        return mChannel;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = Collections.singletonList(getSupergroupAddress());
        }

        return mIdentifiers;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        return Collections.singletonList(getChannel());
    }
}
