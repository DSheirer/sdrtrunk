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

package io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Lra;
import io.github.dsheirer.module.decode.p25.identifier.APCO25System;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Wacn;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.OSPMessage;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import io.github.dsheirer.module.decode.p25.reference.SystemServiceClass;
import java.util.ArrayList;
import java.util.List;

/**
 * Network status broadcast
 */
public class NetworkStatusBroadcast extends OSPMessage implements IFrequencyBandReceiver
{
    private static final int[] LOCATION_REGISTRATION_AREA = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] WACN = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43};
    private static final int[] SYSTEM = {44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] FREQUENCY_BAND = {56, 57, 58, 59};
    private static final int[] CHANNEL_NUMBER = {60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] SYSTEM_SERVICE_CLASS = {72, 73, 74, 75, 76, 77, 78, 79};

    private Identifier mLocationRegistrationArea;
    private Identifier mWacn;
    private Identifier mSystem;
    private IChannelDescriptor mChannel;
    private SystemServiceClass mSystemServiceClass;
    private List<Identifier> mIdentifiers;
    private ScrambleParameters mScrambleParameters;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public NetworkStatusBroadcast(P25P1DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" WACN:").append(getWacn());
        sb.append(" SYSTEM:").append(getSystem());
        sb.append(" LRA:").append(getLocationRegistrationArea());
        sb.append(" CHAN:").append(getChannel());
        sb.append(" SERVICES:").append(getSystemServiceClass().getServices());
        return sb.toString();
    }

    /**
     * P25 Phase II scramble (randomizer) parameters.
     */
    public ScrambleParameters getScrambleParameters()
    {
        if(mScrambleParameters == null)
        {
            mScrambleParameters = new ScrambleParameters((int)getWacn().getValue(), (int)getSystem().getValue(),
                (int)getNAC().getValue());
        }

        return mScrambleParameters;
    }

    public Identifier getLocationRegistrationArea()
    {
        if(mLocationRegistrationArea == null)
        {
            mLocationRegistrationArea = APCO25Lra.create(getMessage().getInt(LOCATION_REGISTRATION_AREA));
        }

        return mLocationRegistrationArea;
    }

    public Identifier getWacn()
    {
        if(mWacn == null)
        {
            mWacn = APCO25Wacn.create(getMessage().getInt(WACN));
        }

        return mWacn;
    }

    public Identifier getSystem()
    {
        if(mSystem == null)
        {
            mSystem = APCO25System.create(getMessage().getInt(SYSTEM));
        }

        return mSystem;
    }

    public IChannelDescriptor getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25Channel.create(getMessage().getInt(FREQUENCY_BAND), getMessage().getInt(CHANNEL_NUMBER));
        }

        return mChannel;
    }

    public SystemServiceClass getSystemServiceClass()
    {
        if(mSystemServiceClass == null)
        {
            mSystemServiceClass = SystemServiceClass.create(getMessage().getInt(SYSTEM_SERVICE_CLASS));
        }

        return mSystemServiceClass;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getLocationRegistrationArea());
            mIdentifiers.add(getWacn());
            mIdentifiers.add(getSystem());
        }

        return mIdentifiers;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        List<IChannelDescriptor> channels = new ArrayList<>();
        channels.add(getChannel());
        return channels;
    }
}
