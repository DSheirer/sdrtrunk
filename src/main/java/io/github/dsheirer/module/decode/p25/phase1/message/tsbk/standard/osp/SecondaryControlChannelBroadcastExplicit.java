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

package io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Rfss;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Site;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25ExplicitChannel;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.OSPMessage;
import io.github.dsheirer.module.decode.p25.reference.SystemServiceClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Secondary control channel broadcast - explicit
 */
public class SecondaryControlChannelBroadcastExplicit extends OSPMessage implements IFrequencyBandReceiver
{
    private static final int[] RFSS = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] SITE = {24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] TRANSMIT_FREQUENCY_BAND = {32, 33, 34, 35};
    private static final int[] TRANSMIT_CHANNEL_NUMBER = {36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] RESERVED = {48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] RECEIVE_FREQUENCY_BAND = {56, 57, 58, 59};
    private static final int[] RECEIVE_CHANNEL_NUMBER = {60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] SYSTEM_SERVICE_CLASS = {72, 73, 74, 75, 76, 77, 78, 79};

    private Identifier mRfss;
    private Identifier mSite;
    private IChannelDescriptor mChannel;
    private SystemServiceClass mSystemServiceClass;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public SecondaryControlChannelBroadcastExplicit(P25P1DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" RFSS:").append(getRfss());
        sb.append(" SITE:").append(getSite());
        sb.append(" CHAN:").append(getChannel());
        sb.append(" SERVICE OPTIONS:").append(getSystemServiceClass());
        return sb.toString();
    }

    public Identifier getRfss()
    {
        if(mRfss == null)
        {
            mRfss = APCO25Rfss.create(getMessage().getInt(RFSS));
        }

        return mRfss;
    }

    public Identifier getSite()
    {
        if(mSite == null)
        {
            mSite = APCO25Site.create(getMessage().getInt(SITE));
        }

        return mSite;
    }

    public IChannelDescriptor getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25ExplicitChannel.create(getMessage().getInt(TRANSMIT_FREQUENCY_BAND),
                getMessage().getInt(TRANSMIT_CHANNEL_NUMBER), getMessage().getInt(RECEIVE_FREQUENCY_BAND),
                getMessage().getInt(RECEIVE_CHANNEL_NUMBER));
        }

        return mChannel;
    }

    public SystemServiceClass getSystemServiceClass()
    {
        if(mSystemServiceClass == null)
        {
            mSystemServiceClass = new SystemServiceClass(getMessage().getInt(SYSTEM_SERVICE_CLASS));
        }

        return mSystemServiceClass;
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
        channels.add(getChannel());
        return channels;
    }
}
