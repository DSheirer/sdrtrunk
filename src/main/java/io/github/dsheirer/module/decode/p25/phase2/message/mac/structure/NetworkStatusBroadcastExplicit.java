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
import io.github.dsheirer.module.decode.p25.identifier.APCO25Lra;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Nac;
import io.github.dsheirer.module.decode.p25.identifier.APCO25System;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Wacn;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25ExplicitChannel;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import io.github.dsheirer.module.decode.p25.reference.SystemServiceClass;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Network status broadcast explicit
 */
public class NetworkStatusBroadcastExplicit extends MacStructure implements IFrequencyBandReceiver
{
    private static final IntField LRA = IntField.length8(OCTET_2_BIT_8);
    private static final IntField WACN = IntField.length20(OCTET_3_BIT_16);
    private static final IntField SYSTEM_ID = IntField.length12(OCTET_5_BIT_32 + 4);
    private static final IntField TRANSMIT_FREQUENCY_BAND = IntField.length4(OCTET_7_BIT_48);
    private static final IntField TRANSMIT_CHANNEL_NUMBER = IntField.length12(OCTET_7_BIT_48 + 4);
    private static final IntField RECEIVE_FREQUENCY_BAND = IntField.length4(OCTET_9_BIT_64);
    private static final IntField RECEIVE_CHANNEL_NUMBER = IntField.length12(OCTET_9_BIT_64 + 4);
    private static final IntField SERVICE_CLASS = IntField.length8(OCTET_11_BIT_80);
    private static final IntField COLOR_CODE = IntField.length12(OCTET_12_BIT_88 + 4);

    private List<Identifier> mIdentifiers;
    private Identifier mLRA;
    private Identifier mWACN;
    private Identifier mSystem;
    private APCO25Channel mChannel;
    private SystemServiceClass mSystemServiceClass;
    private Identifier mNAC;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public NetworkStatusBroadcastExplicit(CorrectedBinaryMessage message, int offset)
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
        sb.append(" WACN:").append(getWACN());
        sb.append(" SYSTEM:").append(getSystem());
        sb.append(" NAC:").append(getNAC());
        sb.append(" LRA:").append(getLRA());
        sb.append(" CHANNEL:").append(getChannel());
        sb.append(" SERVICES:").append(getSystemServiceClass().getServices());
        return sb.toString();
    }

    /**
     * Scramble sequence parameters: WACN, SYSTEM and NAC
     */
    public ScrambleParameters getScrambleParameters()
    {
        return new ScrambleParameters(getInt(WACN), getInt(SYSTEM_ID), getInt(COLOR_CODE));
    }

    public Identifier getLRA()
    {
        if(mLRA == null)
        {
            mLRA = APCO25Lra.create(getInt(LRA));
        }

        return mLRA;
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
            mSystem = APCO25System.create(getInt(SYSTEM_ID));
        }

        return mSystem;
    }

    public APCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25ExplicitChannel.create(getInt(TRANSMIT_FREQUENCY_BAND),
                getInt(TRANSMIT_CHANNEL_NUMBER),
                getInt(RECEIVE_FREQUENCY_BAND),
                getInt(RECEIVE_CHANNEL_NUMBER));
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

    public Identifier getNAC()
    {
        if(mNAC == null)
        {
            mNAC = APCO25Nac.create(getInt(COLOR_CODE));
        }

        return mNAC;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getLRA());
            mIdentifiers.add(getWACN());
            mIdentifiers.add(getSystem());
            mIdentifiers.add(getNAC());
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
