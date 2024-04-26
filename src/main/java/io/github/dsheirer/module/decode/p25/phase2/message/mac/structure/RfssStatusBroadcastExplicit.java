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
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.reference.SystemServiceClass;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RFSS status broadcast explicit
 */
public class RfssStatusBroadcastExplicit extends RfssStatusBroadcast implements IFrequencyBandReceiver
{
    private static final IntField TRANSMIT_FREQUENCY_BAND = IntField.length4(OCTET_7_BIT_48);
    private static final IntField TRANSMIT_CHANNEL_NUMBER = IntField.length12(OCTET_7_BIT_48 + 4);
    private static final IntField RECEIVE_FREQUENCY_BAND = IntField.length4(OCTET_9_BIT_64);
    private static final IntField RECEIVE_CHANNEL_NUMBER = IntField.length12(OCTET_9_BIT_64 + 4);
    private static final IntField SERVICE_CLASS = IntField.length8(OCTET_11_BIT_80);

    private List<Identifier> mIdentifiers;
    private APCO25Channel mChannel;
    private SystemServiceClass mSystemServiceClass;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public RfssStatusBroadcastExplicit(CorrectedBinaryMessage message, int offset)
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
        sb.append(" SYSTEM:").append(getSystem());
        sb.append(" RFSS:").append(getRFSS());
        sb.append(" SITE:").append(getSite());
        sb.append(" LRA:").append(getLRA());
        sb.append(" CHANNEL:").append(getChannel());
        sb.append(" SERVICES:").append(getSystemServiceClass().getServices());
        return sb.toString();
    }

    /**
     * Control channel.  This will be a phase 1 channel, even though it's being broadcast on a Phase II channel.
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

    public SystemServiceClass getSystemServiceClass()
    {
        if(mSystemServiceClass == null)
        {
            mSystemServiceClass = SystemServiceClass.create(getInt(SERVICE_CLASS));
        }

        return mSystemServiceClass;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getLRA());
            mIdentifiers.add(getSystem());
            mIdentifiers.add(getRFSS());
            mIdentifiers.add(getSite());
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
