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
import io.github.dsheirer.module.decode.p25.identifier.APCO25Rfss;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Site;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;
import io.github.dsheirer.module.decode.p25.reference.SystemServiceClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Secondary control channel broadcast - abbreviated format
 */
public class SecondaryControlChannelBroadcastAbbreviated extends MacStructure implements IFrequencyBandReceiver
{
    private static final int[] RFSS = {8,9,10,11,12,13,14,15};
    private static final int[] SITE = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] FREQUENCY_BAND_A = {24, 25, 26, 27};
    private static final int[] CHANNEL_NUMBER_A = {28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] SYSTEM_SERVICE_CLASS_A = {40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] FREQUENCY_BAND_B = {48, 49, 50, 51};
    private static final int[] CHANNEL_NUMBER_B = {52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] SYSTEM_SERVICE_CLASS_B = {64, 65, 66, 67, 68, 69, 70, 71};

    private Identifier mRfss;
    private Identifier mSite;
    private IChannelDescriptor mChannelA;
    private IChannelDescriptor mChannelB;
    private SystemServiceClass mSystemServiceClassA;
    private SystemServiceClass mSystemServiceClassB;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public SecondaryControlChannelBroadcastAbbreviated(CorrectedBinaryMessage message, int offset)
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
            mRfss = APCO25Rfss.create(getMessage().getInt(RFSS, getOffset()));
        }

        return mRfss;
    }

    public Identifier getSite()
    {
        if(mSite == null)
        {
            mSite = APCO25Site.create(getMessage().getInt(SITE, getOffset()));
        }

        return mSite;
    }

    public IChannelDescriptor getChannelA()
    {
        if(mChannelA == null)
        {
            mChannelA = APCO25Channel.create(getMessage().getInt(FREQUENCY_BAND_A, getOffset()),
                getMessage().getInt(CHANNEL_NUMBER_A, getOffset()));
        }

        return mChannelA;
    }

    public SystemServiceClass getSystemServiceClassA()
    {
        if(mSystemServiceClassA == null)
        {
            mSystemServiceClassA = new SystemServiceClass(getMessage().getInt(SYSTEM_SERVICE_CLASS_A, getOffset()));
        }

        return mSystemServiceClassA;
    }

    private boolean hasChannelB()
    {
        return getMessage().getInt(CHANNEL_NUMBER_A, getOffset()) != getMessage().getInt(CHANNEL_NUMBER_B, getOffset()) &&
            getMessage().getInt(SYSTEM_SERVICE_CLASS_B, getOffset()) != 0;
    }

    public IChannelDescriptor getChannelB()
    {
        if(hasChannelB() && mChannelB == null)
        {
            mChannelB = APCO25Channel.create(getMessage().getInt(FREQUENCY_BAND_B, getOffset()),
                getMessage().getInt(CHANNEL_NUMBER_B, getOffset()));
        }

        return mChannelB;
    }

    public SystemServiceClass getSystemServiceClassB()
    {
        if(mSystemServiceClassB == null)
        {
            mSystemServiceClassB = new SystemServiceClass(getMessage().getInt(SYSTEM_SERVICE_CLASS_B, getOffset()));
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
