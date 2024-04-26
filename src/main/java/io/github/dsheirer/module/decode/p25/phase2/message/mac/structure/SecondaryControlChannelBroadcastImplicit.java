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
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.reference.SystemServiceClass;
import java.util.ArrayList;
import java.util.List;

/**
 * Secondary control channel broadcast implicit
 */
public class SecondaryControlChannelBroadcastImplicit extends SecondaryControlChannelBroadcast implements IFrequencyBandReceiver
{
    private static final IntField FREQUENCY_BAND_1 = IntField.length4(OCTET_4_BIT_24);
    private static final IntField CHANNEL_NUMBER_1 = IntField.length12(OCTET_4_BIT_24 + 4);
    private static final IntField SYSTEM_SERVICE_CLASS_1 = IntField.length8(OCTET_6_BIT_40);
    private static final IntField FREQUENCY_BAND_2 = IntField.length4(OCTET_7_BIT_48);
    private static final IntField CHANNEL_NUMBER_2 = IntField.length12(OCTET_7_BIT_48 + 4);
    private static final IntField SYSTEM_SERVICE_CLASS_2 = IntField.length8(OCTET_9_BIT_64);

    private IChannelDescriptor mChannel1;
    private IChannelDescriptor mChannel2;
    private SystemServiceClass mSystemServiceClass1;
    private SystemServiceClass mSystemServiceClass2;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public SecondaryControlChannelBroadcastImplicit(CorrectedBinaryMessage message, int offset)
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
        sb.append(" RFSS:").append(getRfss());
        sb.append(" SITE:").append(getSite());
        sb.append(" CHAN 1:").append(getChannel1());
        sb.append(" SERVICE OPTIONS:").append(getSystemServiceClass1());
        if(hasChannelB())
        {
            sb.append(" CHAN 2:").append(getChannel2());
            sb.append(" SERVICE OPTIONS:").append(getSystemServiceClass2());
        }
        return sb.toString();
    }

    public IChannelDescriptor getChannel1()
    {
        if(mChannel1 == null)
        {
            mChannel1 = APCO25Channel.create(getInt(FREQUENCY_BAND_1), getInt(CHANNEL_NUMBER_1));
        }

        return mChannel1;
    }

    public SystemServiceClass getSystemServiceClass1()
    {
        if(mSystemServiceClass1 == null)
        {
            mSystemServiceClass1 = new SystemServiceClass(getInt(SYSTEM_SERVICE_CLASS_1));
        }

        return mSystemServiceClass1;
    }

    private boolean hasChannelB()
    {
        return getInt(CHANNEL_NUMBER_1) != getInt(CHANNEL_NUMBER_2) && getInt(SYSTEM_SERVICE_CLASS_2) != 0;
    }

    public IChannelDescriptor getChannel2()
    {
        if(hasChannelB() && mChannel2 == null)
        {
            mChannel2 = APCO25Channel.create(getInt(FREQUENCY_BAND_2), getInt(CHANNEL_NUMBER_2));
        }

        return mChannel2;
    }

    public SystemServiceClass getSystemServiceClass2()
    {
        if(mSystemServiceClass2 == null)
        {
            mSystemServiceClass2 = new SystemServiceClass(getInt(SYSTEM_SERVICE_CLASS_2));
        }

        return mSystemServiceClass2;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getSite());
            mIdentifiers.add(getRfss());
        }

        return mIdentifiers;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        List<IChannelDescriptor> channels = new ArrayList<>();
        channels.add(getChannel1());

        if(hasChannelB())
        {
            channels.add(getChannel2());
        }
        return channels;
    }
}
