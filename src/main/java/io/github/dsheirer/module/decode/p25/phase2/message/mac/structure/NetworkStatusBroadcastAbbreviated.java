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
import io.github.dsheirer.module.decode.p25.identifier.APCO25Lra;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Nac;
import io.github.dsheirer.module.decode.p25.identifier.APCO25System;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Wacn;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.P25Channel;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;
import io.github.dsheirer.module.decode.p25.reference.SystemServiceClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Network status broadcast - abbreviated format
 */
public class NetworkStatusBroadcastAbbreviated extends MacStructure implements IFrequencyBandReceiver
{
    private static final int[] LRA = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] WACN = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35};
    private static final int[] SYSTEM_ID = {36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] FREQUENCY_BAND = {48, 49, 50, 51};
    private static final int[] CHANNEL_NUMBER = {52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] SERVICE_CLASS = {64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] COLOR_CODE = {76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87};

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
    public NetworkStatusBroadcastAbbreviated(CorrectedBinaryMessage message, int offset)
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
        return new ScrambleParameters(getMessage().getInt(WACN,getOffset()), getMessage().getInt(SYSTEM_ID, getOffset()),
            getMessage().getInt(COLOR_CODE, getOffset()));
    }

    public Identifier getLRA()
    {
        if(mLRA == null)
        {
            mLRA = APCO25Lra.create(getMessage().getInt(LRA, getOffset()));
        }

        return mLRA;
    }

    public Identifier getWACN()
    {
        if(mWACN == null)
        {
            mWACN = APCO25Wacn.create(getMessage().getInt(WACN, getOffset()));
        }

        return mWACN;
    }

    public Identifier getSystem()
    {
        if(mSystem == null)
        {
            mSystem = APCO25System.create(getMessage().getInt(SYSTEM_ID, getOffset()));
        }

        return mSystem;
    }

    /**
     * This channel refers to the control channel for this site.
     *
     * Note: even though this is broadcast on a Phase II channel, the control channels are currently limited to
     * phase 1 and therefore, this should be a Phase 1 control channel.
     */
    public APCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            P25Channel channel = new P25Channel(getMessage().getInt(FREQUENCY_BAND, getOffset()),
                getMessage().getInt(CHANNEL_NUMBER, getOffset()));
            mChannel = new APCO25Channel(channel);
        }

        return mChannel;
    }

    public SystemServiceClass getSystemServiceClass()
    {
        if(mSystemServiceClass == null)
        {
            mSystemServiceClass = SystemServiceClass.create(getMessage().getInt(SERVICE_CLASS, getOffset()));
        }

        return mSystemServiceClass;
    }

    public Identifier getNAC()
    {
        if(mNAC == null)
        {
            mNAC = APCO25Nac.create(getMessage().getInt(COLOR_CODE, getOffset()));
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
