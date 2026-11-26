/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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
import io.github.dsheirer.channel.IBandChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Lra;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Rfss;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Site;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25ExplicitChannel;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adjacent site status broadcast (ie neighbor).
 */
public class LCAdjacentSiteStatusBroadcastExplicit extends LinkControlWord implements IFrequencyBandReceiver
{
    private static final IntField LRA = IntField.length8(OCTET_1_BIT_8);
    private static final IntField DOWNLINK_FREQUENCY_BAND = IntField.length4(OCTET_2_BIT_16);
    private static final IntField DOWNLINK_CHANNEL_NUMBER = IntField.length12(OCTET_2_BIT_16 + 4);
    private static final IntField RFSS = IntField.length8(OCTET_4_BIT_32);
    private static final IntField SITE = IntField.length8(OCTET_5_BIT_40);
    private static final IntField UPLINK_FREQUENCY_BAND = IntField.length4(OCTET_6_BIT_48);
    private static final IntField UPLINK_CHANNEL_NUMBER = IntField.length12(OCTET_6_BIT_48 + 4);
    //Octet 8 is the C, F, V and A flags and 4 reserved bits; this message has no service class (AABF-D 7.3.25)
    private static final int CONVENTIONAL_CHANNEL_FLAG = 64;
    private static final int SITE_FAILURE_FLAG = 65;
    private static final int VALID_INFORMATION_FLAG = 66;
    private static final int ACTIVE_NETWORK_CONNECTION_TO_RFSS_CONTROLLER_FLAG = 67;

    private List<Identifier> mIdentifiers;
    private Identifier mLRA;
    private Identifier mRFSS;
    private Identifier mSite;
    private IBandChannelDescriptor mChannel;
    private List<String> mSiteFlags;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCAdjacentSiteStatusBroadcastExplicit(CorrectedBinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" RFSS:").append(getRfss());
        sb.append(" SITE:").append(getSite());
        sb.append(" LRA:").append(getLocationRegistrationArea());
        sb.append(" CHAN:").append(getChannel());
        sb.append(" FLAGS ").append(getSiteFlags());
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

    public IBandChannelDescriptor getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25ExplicitChannel.create(getInt(DOWNLINK_FREQUENCY_BAND), getInt(DOWNLINK_CHANNEL_NUMBER),
                    getInt(UPLINK_FREQUENCY_BAND), getInt(UPLINK_CHANNEL_NUMBER));
        }

        return mChannel;
    }

    /**
     * Indicates if the channel is a conventional repeater channel
     */
    public boolean isConventionalChannel()
    {
        return getMessage().get(CONVENTIONAL_CHANNEL_FLAG);
    }

    /**
     * Indicates if the site is in a failure condition
     */
    public boolean isFailedConditionSite()
    {
        return getMessage().get(SITE_FAILURE_FLAG);
    }

    /**
     * Indicates if the site information is valid
     */
    public boolean isValidSiteInformation()
    {
        return getMessage().get(VALID_INFORMATION_FLAG);
    }

    /**
     * Indicates if the site has an active network connection to the RFSS controller
     */
    public boolean isActiveNetworkConnectionToRfssControllerSite()
    {
        return getMessage().get(ACTIVE_NETWORK_CONNECTION_TO_RFSS_CONTROLLER_FLAG);
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
     * List of identifiers contained in this message
     */
    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getLocationRegistrationArea());
            mIdentifiers.add(getRfss());
            mIdentifiers.add(getSite());
        }

        return mIdentifiers;
    }

    @Override
    public List<IBandChannelDescriptor> getChannels()
    {
        return Collections.singletonList(getChannel());
    }
}
