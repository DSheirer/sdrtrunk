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
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25ExplicitChannel;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;
import io.github.dsheirer.module.decode.p25.reference.SystemServiceClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Secondary control channel broadcast - explicit channel format
 */
public class SecondaryControlChannelBroadcastExplicit extends MacStructure implements IFrequencyBandReceiver
{
    private static final int[] RFSS = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] SITE = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] TRANSMIT_FREQUENCY_BAND = {24, 25, 26, 27};
    private static final int[] TRANSMIT_CHANNEL_NUMBER = {28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] RECEIVE_FREQUENCY_BAND = {40, 41, 42, 43};
    private static final int[] RECEIVE_CHANNEL_NUMBER = {44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] SYSTEM_SERVICE_CLASS = {56, 57, 58, 59, 60, 61, 62, 63};

    private Identifier mRfss;
    private Identifier mSite;
    private IChannelDescriptor mChannel;
    private SystemServiceClass mSystemServiceClass;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public SecondaryControlChannelBroadcastExplicit(CorrectedBinaryMessage message, int offset)
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
        sb.append(" CHAN A:").append(getChannel());
        sb.append(" SERVICE OPTIONS:").append(getSystemServiceClass());
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

    public IChannelDescriptor getChannel()
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
            mSystemServiceClass = new SystemServiceClass(getMessage().getInt(SYSTEM_SERVICE_CLASS, getOffset()));
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
