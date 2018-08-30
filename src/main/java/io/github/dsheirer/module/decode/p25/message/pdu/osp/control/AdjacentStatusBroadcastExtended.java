/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25.message.pdu.osp.control;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.edac.CRCP25;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.channel.APCO25ExplicitChannel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.identifier.integer.node.APCO25Lra;
import io.github.dsheirer.identifier.integer.node.APCO25Rfss;
import io.github.dsheirer.identifier.integer.node.APCO25Site;
import io.github.dsheirer.identifier.integer.node.APCO25System;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.IAdjacentSite;
import io.github.dsheirer.module.decode.p25.message.pdu.PDUMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.SystemService;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

import java.util.ArrayList;
import java.util.List;

public class AdjacentStatusBroadcastExtended extends PDUMessage implements FrequencyBandReceiver, IAdjacentSite
{
    public static final int[] LRA = {88, 89, 90, 91, 92, 93, 94, 95};
    public static final int[] SYSTEM_ID = {100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111};
    public static final int[] RF_SUBSYSTEM_ID = {128, 129, 130, 131, 132, 133, 134, 135};
    public static final int[] SITE_ID = {136, 137, 138, 139, 140, 141, 142, 143};
    public static final int[] DOWNLINK_FREQUENCY_BAND = {160, 161, 162, 163};
    public static final int[] DOWNLINK_CHANNEL_NUMBER = {164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175};
    public static final int[] UPLINK_FREQUENCY_BAND = {176, 177, 178, 179};
    public static final int[] UPLINK_CHANNEL_NUMBER = {180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191};
    public static final int[] SYSTEM_SERVICE_CLASS = {192, 193, 194, 195, 196, 197, 198, 199};
    public static final int[] MULTIPLE_BLOCK_CRC = {224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236,
        237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255};

    private IIdentifier mLRA;
    private IIdentifier mSystem;
    private IIdentifier mRFSS;
    private IIdentifier mSite;
    private IAPCO25Channel mChannel;

    public AdjacentStatusBroadcastExtended(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);

        /* Header block is already error detected/corrected - perform error
         * detection correction on the intermediate and final data blocks */
        mMessage = CRCP25.correctPDU1(mMessage);
        mCRC[1] = mMessage.getCRC();
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" LRA:" + getLRAId());
        sb.append(" SYS:" + getSystemID());
        sb.append(" RFSS:" + getRFSSId());
        sb.append(" SITE:" + getSiteID());
        sb.append(" CHAN:" + getChannel());
        sb.append(" SYS SVC CLASS:" + getSystemServiceClass());

        return sb.toString();
    }

    public String getUniqueID()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getSystemID());
        sb.append(":");
        sb.append(getRFSSId());
        sb.append(":");
        sb.append(getSiteID());

        return sb.toString();
    }

    public IIdentifier getSystemID()
    {
        if(mSystem == null)
        {
            mSystem = APCO25System.create(mMessage.getInt(SYSTEM_ID));
        }

        return mSystem;
    }

    public IIdentifier getSiteID()
    {
        if(mSite == null)
        {
            mSite = APCO25Site.create(mMessage.getInt(SITE_ID));
        }

        return mSite;
    }

    public IAPCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25ExplicitChannel.create(mMessage.getInt(DOWNLINK_FREQUENCY_BAND),
                mMessage.getInt(DOWNLINK_CHANNEL_NUMBER), mMessage.getInt(UPLINK_FREQUENCY_BAND),
                mMessage.getInt(UPLINK_CHANNEL_NUMBER));
        }

        return mChannel;
    }

    @Override
    public IIdentifier getRFSSId()
    {
        if(mRFSS == null)
        {
            mRFSS = APCO25Rfss.create(mMessage.getInt(RF_SUBSYSTEM_ID));
        }

        return mRFSS;
    }

    @Override
    public IIdentifier getLRAId()
    {
        if(mLRA == null)
        {
            mLRA = APCO25Lra.create(mMessage.getInt(LRA));
        }

        return mLRA;
    }

    @Override
    public String getSystemServiceClass()
    {
        return SystemService.toString(mMessage.getInt(SYSTEM_SERVICE_CLASS));
    }

    @Override
    public List<IAPCO25Channel> getChannels()
    {
        List<IAPCO25Channel> channels = new ArrayList<>();
        channels.add(getChannel());
        return channels;
    }
}
