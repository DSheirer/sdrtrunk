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
import io.github.dsheirer.module.decode.p25.identifier.APCO25Rfss;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Site;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.reference.SystemServiceClass;
import java.util.ArrayList;
import java.util.List;

/**
 * Secondary control channel broadcast.
 */
public class LCSecondaryControlChannelBroadcast extends LinkControlWord implements IFrequencyBandReceiver
{
    private static final IntField RFSS = IntField.length8(OCTET_1_BIT_8);
    private static final IntField SITE = IntField.length8(OCTET_2_BIT_16);
    private static final IntField FREQUENCY_BAND_A = IntField.length4(OCTET_3_BIT_24);
    private static final IntField CHANNEL_NUMBER_A = IntField.length12(OCTET_3_BIT_24 + 4);
    private static final IntField SERVICE_CLASS_A = IntField.length8(OCTET_5_BIT_40);
    private static final IntField FREQUENCY_BAND_B = IntField.length4(OCTET_6_BIT_48);
    private static final IntField CHANNEL_NUMBER_B = IntField.length12(OCTET_6_BIT_48 + 4);
    private static final IntField SERVICE_CLASS_B = IntField.length8(OCTET_8_BIT_64);

    private List<Identifier> mIdentifiers;
    private Identifier mRFSS;
    private Identifier mSite;
    private IChannelDescriptor mChannelA;
    private IChannelDescriptor mChannelB;
    private SystemServiceClass mSystemServiceClassA;
    private SystemServiceClass mSystemServiceClassB;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCSecondaryControlChannelBroadcast(CorrectedBinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" SITE:").append(getRFSS()).append("-").append(getSite());
        sb.append(" CHAN A:").append(getChannelA());
        sb.append(" SERVICE CLASS:").append(getSystemServiceClassA());

        if(hasChannelB())
        {
            sb.append(" CHAN B:").append(getChannelB());
            sb.append(" SERVICE CLASS:").append(getSystemServiceClassB());
        }
        return sb.toString();
    }

    public Identifier getRFSS()
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

    public IChannelDescriptor getChannelA()
    {
        if(mChannelA == null)
        {
            mChannelA = APCO25Channel.create(getInt(FREQUENCY_BAND_A), getInt(CHANNEL_NUMBER_A));
        }

        return mChannelA;
    }

    private boolean hasChannelB()
    {
        return getInt(CHANNEL_NUMBER_A) != getInt(CHANNEL_NUMBER_B) && getInt(SERVICE_CLASS_B) != 0;
    }

    public IChannelDescriptor getChannelB()
    {
        if(mChannelB == null)
        {
            mChannelB = APCO25Channel.create(getInt(FREQUENCY_BAND_B), getInt(CHANNEL_NUMBER_B));
        }

        return mChannelB;
    }

    public SystemServiceClass getSystemServiceClassA()
    {
        if(mSystemServiceClassA == null)
        {
            mSystemServiceClassA = new SystemServiceClass(getInt(SERVICE_CLASS_A));
        }

        return mSystemServiceClassA;
    }


    public SystemServiceClass getSystemServiceClassB()
    {
        if(mSystemServiceClassB == null)
        {
            mSystemServiceClassB = new SystemServiceClass(getInt(SERVICE_CLASS_B));
        }

        return mSystemServiceClassB;
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
            mIdentifiers.add(getRFSS());
            mIdentifiers.add(getSite());
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
