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
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.OSPMessage;
import io.github.dsheirer.module.decode.p25.reference.SystemServiceClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Secondary control channel broadcast
 */
public class SecondaryControlChannelBroadcast extends OSPMessage implements IFrequencyBandReceiver
{
    private static final int[] RFSS = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] SITE = {24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] FREQUENCY_BAND_A = {32, 33, 34, 35};
    private static final int[] CHANNEL_NUMBER_A = {36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] SYSTEM_SERVICE_CLASS_A = {48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] FREQUENCY_BAND_B = {56, 57, 58, 59};
    private static final int[] CHANNEL_NUMBER_B = {60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] SYSTEM_SERVICE_CLASS_B = {72, 73, 74, 75, 76, 77, 78, 79};

    private Identifier mRfss;
    private Identifier mSite;
    private IChannelDescriptor mChannelA;
    private IChannelDescriptor mChannelB;
    private SystemServiceClass mSystemServiceClassA;
    private SystemServiceClass mSystemServiceClassB;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public SecondaryControlChannelBroadcast(P25P1DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" RFSS:").append(getRfss());
        sb.append(" SITE:").append(getSite());
        sb.append(" CHAN A:").append(getChannelA());
        sb.append(" SERVICE OPTIONS:").append(getSystemServiceClassA());
        if(hasChannelB())
        {
            sb.append(" CHAN B:").append(getChannelB());
            sb.append(" SERVICE OPTIONS:").append(getSystemServiceClassB());
        }
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

    public IChannelDescriptor getChannelA()
    {
        if(mChannelA == null)
        {
            mChannelA = APCO25Channel.create(getMessage().getInt(FREQUENCY_BAND_A), getMessage().getInt(CHANNEL_NUMBER_A));
        }

        return mChannelA;
    }

    public SystemServiceClass getSystemServiceClassA()
    {
        if(mSystemServiceClassA == null)
        {
            mSystemServiceClassA = new SystemServiceClass(getMessage().getInt(SYSTEM_SERVICE_CLASS_A));
        }

        return mSystemServiceClassA;
    }

    private boolean hasChannelB()
    {
        return getMessage().getInt(CHANNEL_NUMBER_A) != getMessage().getInt(CHANNEL_NUMBER_B) &&
            getMessage().getInt(SYSTEM_SERVICE_CLASS_B) != 0;
    }

    public IChannelDescriptor getChannelB()
    {
        if(hasChannelB() && mChannelB == null)
        {
            mChannelB = APCO25Channel.create(getMessage().getInt(FREQUENCY_BAND_B), getMessage().getInt(CHANNEL_NUMBER_B));
        }

        return mChannelB;
    }

    public SystemServiceClass getSystemServiceClassB()
    {
        if(mSystemServiceClassB == null)
        {
            mSystemServiceClassB = new SystemServiceClass(getMessage().getInt(SYSTEM_SERVICE_CLASS_B));
        }

        return mSystemServiceClassB;
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
        channels.add(getChannelA());

        if(hasChannelB())
        {
            channels.add(getChannelB());
        }
        return channels;
    }
}
