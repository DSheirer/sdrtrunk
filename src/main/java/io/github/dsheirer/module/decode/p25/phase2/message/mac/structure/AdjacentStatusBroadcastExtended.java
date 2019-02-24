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
import io.github.dsheirer.module.decode.p25.identifier.APCO25Rfss;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Site;
import io.github.dsheirer.module.decode.p25.identifier.APCO25System;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25ExplicitChannel;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;
import io.github.dsheirer.module.decode.p25.reference.SystemServiceClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adjacent (neighbor site) status broadcast - abbreviated format
 */
public class AdjacentStatusBroadcastExtended extends MacStructure implements IFrequencyBandReceiver
{
    private static final int[] LRA = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int CONVENTIONAL_CHANNEL_FLAG = 16;
    private static final int SITE_FAILURE_FLAG = 17;
    private static final int VALID_INFORMATION_FLAG = 18;
    private static final int ACTIVE_NETWORK_CONNECTION_TO_RFSS_CONTROLLER_FLAG = 19;
    private static final int[] SYSTEM_ID = {20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};

    private static final int[] RFSS_ID = {32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] SITE_ID = {40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] TRANSMIT_FREQUENCY_BAND = {48, 49, 50, 51};
    private static final int[] TRANSMIT_CHANNEL_NUMBER = {52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] RECEIVE_FREQUENCY_BAND = {64, 65, 66, 67};
    private static final int[] RECEIVE_CHANNEL_NUMBER = {68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] SERVICE_CLASS = {80, 81, 82, 83, 84, 85, 86, 87};

    private List<Identifier> mIdentifiers;
    private Identifier mLRA;
    private List<String> mSiteFlags;
    private Identifier mSystem;
    private Identifier mRFSS;
    private Identifier mSite;
    private APCO25Channel mChannel;
    private SystemServiceClass mSystemServiceClass;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public AdjacentStatusBroadcastExtended(CorrectedBinaryMessage message, int offset)
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
        sb.append(" FLAGS:").append(getSiteFlags());
        sb.append(" SERVICES:").append(getSystemServiceClass().getServices());
        return sb.toString();
    }

    public Identifier getLRA()
    {
        if(mLRA == null)
        {
            mLRA = APCO25Lra.create(getMessage().getInt(LRA, getOffset()));
        }

        return mLRA;
    }

    public List<String> getSiteFlags()
    {
        if(mSiteFlags == null)
        {
            mSiteFlags = new ArrayList<>();

            if(isConventionalChannel())
            {
                mSiteFlags.add("CONVENTIONAL CHANNEL");
            }

            if(isFailedConditionSite())
            {
                mSiteFlags.add("FAILURE CONDITION");
            }

            if(isValidSiteInformation())
            {
                mSiteFlags.add("VALID INFORMATION");
            }

            if(isActiveNetworkConnectionToRfssControllerSite())
            {
                mSiteFlags.add("ACTIVE RFSS CONNECTION");
            }
        }

        return mSiteFlags;
    }

    /**
     * Indicates if the channel is a conventional repeater channel
     */
    public boolean isConventionalChannel()
    {
        return getMessage().get(CONVENTIONAL_CHANNEL_FLAG + getOffset());
    }

    /**
     * Indicates if the site is in a failure condition
     */
    public boolean isFailedConditionSite()
    {
        return getMessage().get(SITE_FAILURE_FLAG + getOffset());
    }

    /**
     * Indicates if the site informaiton is valid
     */
    public boolean isValidSiteInformation()
    {
        return getMessage().get(VALID_INFORMATION_FLAG + getOffset());
    }

    /**
     * Indicates if the site has an active network connection to the RFSS controller
     */
    public boolean isActiveNetworkConnectionToRfssControllerSite()
    {
        return getMessage().get(ACTIVE_NETWORK_CONNECTION_TO_RFSS_CONTROLLER_FLAG + getOffset());
    }

    public Identifier getRFSS()
    {
        if(mRFSS == null)
        {
            mRFSS = APCO25Rfss.create(getMessage().getInt(RFSS_ID, getOffset()));
        }

        return mRFSS;
    }

    public Identifier getSite()
    {
        if(mSite == null)
        {
            mSite = APCO25Site.create(getMessage().getInt(SITE_ID, getOffset()));
        }

        return mSite;
    }

    public Identifier getSystem()
    {
        if(mSystem == null)
        {
            mSystem = APCO25System.create(getMessage().getInt(SYSTEM_ID, getOffset()));
        }

        return mSystem;
    }

    public APCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25ExplicitChannel.create(getMessage().getInt(TRANSMIT_FREQUENCY_BAND, getOffset()),
                getMessage().getInt(TRANSMIT_CHANNEL_NUMBER, getOffset()),
                getMessage().getInt(RECEIVE_FREQUENCY_BAND, getOffset()),
                getMessage().getInt(RECEIVE_CHANNEL_NUMBER, getOffset()));
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
