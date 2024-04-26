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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.standard;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.APCO25System;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Wacn;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25ExplicitChannel;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Network status broadcast explicit
 */
public class LCNetworkStatusBroadcastExplicit extends LinkControlWord implements IFrequencyBandReceiver
{
    private static final IntField WACN = IntField.length20(OCTET_1_BIT_8);
    private static final IntField SYSTEM = IntField.length12(OCTET_3_BIT_24 + 4);
    private static final IntField DOWNLINK_FREQUENCY_BAND = IntField.length4(OCTET_5_BIT_40);
    private static final IntField DOWNLINK_CHANNEL_NUMBER = IntField.length12(OCTET_5_BIT_40 + 4);
    private static final IntField UPLINK_FREQUENCY_BAND = IntField.length4(OCTET_7_BIT_56);
    private static final IntField UPLINK_CHANNEL_NUMBER = IntField.length12(OCTET_7_BIT_56 + 4);
    private List<Identifier> mIdentifiers;
    private Identifier mWACN;
    private Identifier mSystem;
    private IChannelDescriptor mChannel;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCNetworkStatusBroadcastExplicit(CorrectedBinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" WACN:").append(getWACN());
        sb.append(" SYSTEM:").append(getSystem());
        sb.append(" CHAN:").append(getChannel());
        return sb.toString();
    }

    public Identifier getWACN()
    {
        if(mWACN == null)
        {
            mWACN = APCO25Wacn.create(getInt(WACN));
        }

        return mWACN;
    }

    public Identifier getSystem()
    {
        if(mSystem == null)
        {
            mSystem = APCO25System.create(getInt(SYSTEM));
        }

        return mSystem;
    }

    public IChannelDescriptor getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25ExplicitChannel.create(getInt(DOWNLINK_FREQUENCY_BAND), getInt(DOWNLINK_CHANNEL_NUMBER),
                    getInt(UPLINK_FREQUENCY_BAND), getInt(UPLINK_CHANNEL_NUMBER));
        }

        return mChannel;
    }

    /**
     * List of identifiers contained in this message
     */
    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getWACN());
            mIdentifiers.add(getSystem());
        }

        return mIdentifiers;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        return Collections.singletonList(getChannel());
    }
}