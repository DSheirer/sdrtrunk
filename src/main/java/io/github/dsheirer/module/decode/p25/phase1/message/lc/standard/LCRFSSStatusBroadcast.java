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
import io.github.dsheirer.module.decode.p25.identifier.APCO25Lra;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Rfss;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Site;
import io.github.dsheirer.module.decode.p25.identifier.APCO25System;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.reference.SystemServiceClass;
import java.util.ArrayList;
import java.util.List;

/**
 * RF SubSystem broadcast information.
 */
public class LCRFSSStatusBroadcast extends LinkControlWord implements IFrequencyBandReceiver
{
    private static final IntField LRA = IntField.length8(OCTET_1_BIT_8);
    private static final IntField SYSTEM = IntField.length12(OCTET_2_BIT_16 + 4);
    private static final IntField RFSS = IntField.length8(OCTET_4_BIT_32);
    private static final IntField SITE = IntField.length8(OCTET_5_BIT_40);
    private static final IntField FREQUENCY_BAND = IntField.length4(OCTET_6_BIT_48);
    private static final IntField CHANNEL_NUMBER = IntField.length12(OCTET_6_BIT_48 + 4);
    private static final IntField SERVICE_CLASS = IntField.length8(OCTET_8_BIT_64);

    private List<Identifier> mIdentifiers;
    private Identifier mLRA;
    private Identifier mSystem;
    private Identifier mRFSS;
    private Identifier mSite;
    private IChannelDescriptor mChannel;
    private SystemServiceClass mSystemServiceClass;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCRFSSStatusBroadcast(CorrectedBinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" LRA:").append(getLocationRegistrationArea());
        sb.append(" SYSTEM:").append(getSystem());
        sb.append(" SITE:").append(getRfss()).append("-").append(getSite());
        sb.append(" CHAN:").append(getChannel());
        sb.append(" SERVICE OPTIONS:").append(getSystemServiceClass());
        return sb.toString();
    }

    public Identifier getLocationRegistrationArea()
    {
        if(mLRA == null)
        {
            mLRA = APCO25Lra.create(getInt(LRA));
        }

        return mLRA;
    }

    public Identifier getSystem()
    {
        if(mSystem == null)
        {
            mSystem = APCO25System.create(getInt(SYSTEM));
        }

        return mSystem;
    }

    public Identifier getRfss()
    {
        if(mRFSS == null)
        {
            mRFSS = APCO25Rfss.create(getInt(RFSS));
        }

        return mRFSS;
    }

    public Identifier getSite()
    {
        if(mSite == null)
        {
            mSite = APCO25Site.create(getInt(SITE));
        }

        return mSite;
    }

    public IChannelDescriptor getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25Channel.create(getInt(FREQUENCY_BAND), getInt(CHANNEL_NUMBER));
        }

        return mChannel;
    }

    public SystemServiceClass getSystemServiceClass()
    {
        if(mSystemServiceClass == null)
        {
            mSystemServiceClass = new SystemServiceClass(getInt(SERVICE_CLASS));
        }

        return mSystemServiceClass;
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
            mIdentifiers.add(getLocationRegistrationArea());
            mIdentifiers.add(getSystem());
            mIdentifiers.add(getRfss());
            mIdentifiers.add(getSite());
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
