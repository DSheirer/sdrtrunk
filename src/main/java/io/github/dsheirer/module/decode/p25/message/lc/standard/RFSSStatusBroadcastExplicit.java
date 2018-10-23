/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.p25.message.lc.standard;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.channel.APCO25ExplicitChannel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.identifier.integer.node.APCO25Lra;
import io.github.dsheirer.identifier.integer.node.APCO25Rfss;
import io.github.dsheirer.identifier.integer.node.APCO25Site;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Secondary control channel broadcast information.
 */
public class RFSSStatusBroadcastExplicit extends LinkControlWord implements FrequencyBandReceiver
{
    private static final int[] LRA = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] DOWNLINK_FREQUENCY_BAND = {16, 17, 18, 19};
    private static final int[] DOWNLINK_CHANNEL_NUMBER = {20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] RFSS = {32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] SITE = {40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] UPLINK_FREQUENCY_BAND = {48, 49, 50, 51};
    private static final int[] UPLINK_CHANNEL_NUMBER = {52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] SERVICE_CLASS = {64, 65, 66, 67, 68, 69, 70, 71};

    private List<IIdentifier> mIdentifiers;
    private IIdentifier mLRA;
    private IIdentifier mRFSS;
    private IIdentifier mSite;
    private IAPCO25Channel mChannel;
    private ServiceOptions mServiceOptions;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public RFSSStatusBroadcastExplicit(BinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" LRA:").append(getLRA());
        sb.append(" SITE:" + getRFSS() + "-" + getSite());
        sb.append(" CHAN:" + getChannel());
        sb.append(" SERVICE OPTIONS:" + getServiceOptions());
        return sb.toString();
    }

    public IIdentifier getLRA()
    {
        if(mLRA == null)
        {
            mLRA = APCO25Lra.create(getMessage().getInt(LRA));
        }

        return mRFSS;
    }

    public IIdentifier getRFSS()
    {
        if(mRFSS == null)
        {
            mRFSS = APCO25Rfss.create(getMessage().getInt(RFSS));
        }

        return mRFSS;
    }

    public IIdentifier getSite()
    {
        if(mSite == null)
        {
            mSite = APCO25Site.create(getMessage().getInt(SITE));
        }

        return mSite;
    }

    public IAPCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25ExplicitChannel.create(getMessage().getInt(DOWNLINK_FREQUENCY_BAND),
                getMessage().getInt(DOWNLINK_CHANNEL_NUMBER), getMessage().getInt(UPLINK_FREQUENCY_BAND),
                getMessage().getInt(UPLINK_CHANNEL_NUMBER));
        }

        return mChannel;
    }

    public ServiceOptions getServiceOptions()
    {
        if(mServiceOptions == null)
        {
            mServiceOptions = new ServiceOptions(getMessage().getInt(SERVICE_CLASS));
        }

        return mServiceOptions;
    }

    /**
     * List of identifiers contained in this message
     */
    @Override
    public List<IIdentifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getLRA());
            mIdentifiers.add(getRFSS());
            mIdentifiers.add(getSite());
        }

        return mIdentifiers;
    }

    @Override
    public List<IAPCO25Channel> getChannels()
    {
        List<IAPCO25Channel> channels = new ArrayList<>();
        channels.add(getChannel());
        return channels;
    }
}
