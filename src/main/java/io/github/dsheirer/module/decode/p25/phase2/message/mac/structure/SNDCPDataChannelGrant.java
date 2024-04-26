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
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.IP25ChannelGrantDetailProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SNDCP data channel grant
 */
public class SNDCPDataChannelGrant extends MacStructureDataService
        implements IFrequencyBandReceiver, IP25ChannelGrantDetailProvider
{
    private static final IntField TRANSMIT_FREQUENCY_BAND = IntField.length4(OCTET_3_BIT_16);
    private static final IntField TRANSMIT_CHANNEL_NUMBER = IntField.length12(OCTET_3_BIT_16 + 4);
    private static final IntField RECEIVE_FREQUENCY_BAND = IntField.length4(OCTET_5_BIT_32);
    private static final IntField RECEIVE_CHANNEL_NUMBER = IntField.length12(OCTET_5_BIT_32 + 4);
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_7_BIT_48);
    private List<Identifier> mIdentifiers;
    private APCO25Channel mChannel;
    private Identifier mTargetAddress;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public SNDCPDataChannelGrant(CorrectedBinaryMessage message, int offset)
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
        sb.append(" TO:").append(getTargetAddress());
        sb.append(" CHAN:").append(getChannel());
        sb.append(" ").append(getServiceOptions());
        return sb.toString();
    }

    /**
     * Channel
     */
    public APCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25ExplicitChannel.create(getInt(TRANSMIT_FREQUENCY_BAND), getInt(TRANSMIT_CHANNEL_NUMBER),
                getInt(RECEIVE_FREQUENCY_BAND), getInt(RECEIVE_CHANNEL_NUMBER));
        }

        return mChannel;
    }

    /**
     * To Talkgroup
     */
    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25RadioIdentifier.createTo(getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    /**
     * Implements the channel grant detail provider interface but always returns null.
     */
    public Identifier getSourceAddress()
    {
        return null;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
            mIdentifiers.add(getChannel());
        }

        return mIdentifiers;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        return Collections.singletonList(getChannel());
    }
}
