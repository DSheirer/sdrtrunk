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
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Lra;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Rfss;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Site;
import io.github.dsheirer.module.decode.p25.identifier.APCO25System;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Wacn;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25ExplicitChannel;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.reference.SiteFlags;
import io.github.dsheirer.module.decode.p25.reference.SystemServiceClass;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adjacent (neighbor site) status broadcast extended explicit
 */
public class AdjacentStatusBroadcastExtendedExplicit extends MacStructure implements IFrequencyBandReceiver
{
    private static final IntField LRA = IntField.length8(OCTET_3_BIT_16);
    private static final IntField SITE_FLAGS = IntField.length4(OCTET_4_BIT_24);
    private static final IntField SYSTEM_ID = IntField.length12(OCTET_4_BIT_24 + 4);
    private static final IntField RFSS_ID = IntField.length8(OCTET_6_BIT_40);
    private static final IntField SITE_ID = IntField.length8(OCTET_7_BIT_48);
    private static final IntField TRANSMIT_FREQUENCY_BAND = IntField.length4(OCTET_8_BIT_56);
    private static final IntField TRANSMIT_CHANNEL_NUMBER = IntField.length12(OCTET_8_BIT_56 + 4);
    private static final IntField RECEIVE_FREQUENCY_BAND = IntField.length4(OCTET_10_BIT_72);
    private static final IntField RECEIVE_CHANNEL_NUMBER = IntField.length12(OCTET_10_BIT_72 + 4);
    private static final IntField SERVICE_CLASS = IntField.length8(OCTET_12_BIT_88);
    private static final IntField WACN = IntField.length20(OCTET_13_BIT_96);

    private List<Identifier> mIdentifiers;
    private Identifier mLRA;
    private Identifier mSystem;
    private Identifier mRFSS;
    private IntegerIdentifier mSite;
    private Identifier mWACN;
    private APCO25Channel mChannel;
    private SiteFlags mSiteFlags;
    private SystemServiceClass mSystemServiceClass;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public AdjacentStatusBroadcastExtendedExplicit(CorrectedBinaryMessage message, int offset)
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
        sb.append(" RFSS:").append(getRFSS());
        sb.append(" SITE:").append(getSite());
        sb.append(" LRA:").append(getLRA());
        sb.append(" CHANNEL:").append(getChannel());
        sb.append(" FLAGS:").append(getSiteFlags());
        sb.append(" SERVICES:").append(getSystemServiceClass().getServices());
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

    public Identifier getLRA()
    {
        if(mLRA == null)
        {
            mLRA = APCO25Lra.create(getInt(LRA));
        }

        return mLRA;
    }

    public SiteFlags getSiteFlags()
    {
        if(mSiteFlags == null)
        {
            mSiteFlags = SiteFlags.create(getInt(SITE_FLAGS));
        }

        return mSiteFlags;
    }

    public Identifier getRFSS()
    {
        if(mRFSS == null)
        {
            mRFSS = APCO25Rfss.create(getInt(RFSS_ID));
        }

        return mRFSS;
    }

    public IntegerIdentifier getSite()
    {
        if(mSite == null)
        {
            mSite = APCO25Site.create(getInt(SITE_ID));
        }

        return mSite;
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
            mIdentifiers.add(getWACN());
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
