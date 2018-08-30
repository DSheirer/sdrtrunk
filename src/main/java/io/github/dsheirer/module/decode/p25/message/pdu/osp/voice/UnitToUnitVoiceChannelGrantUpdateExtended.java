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
package io.github.dsheirer.module.decode.p25.message.pdu.osp.voice;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.edac.CRCP25;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.channel.APCO25ExplicitChannel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.identifier.integer.node.APCO25System;
import io.github.dsheirer.identifier.integer.node.APCO25Wacn;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.pdu.PDUMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;

import java.util.ArrayList;
import java.util.List;

public class UnitToUnitVoiceChannelGrantUpdateExtended extends PDUMessage implements FrequencyBandReceiver
{
    public static final int[] SOURCE_ADDRESS = {88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104,
        105, 106, 107, 108, 109, 110, 111};
    public static final int[] SERVICE_OPTIONS = {128, 129, 130, 131, 132, 133, 134, 135};
    public static final int[] SOURCE_WACN = {160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174,
        175, 176, 177, 178, 179};
    public static final int[] SOURCE_SYSTEM_ID = {180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191};
    public static final int[] SOURCE_ID = {192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206,
        207, 208, 209, 210, 211, 212, 213, 214, 215};
    public static final int[] TARGET_ADDRESS = {216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229,
        230, 231, 232, 233, 234, 235, 236, 237, 238, 239};
    public static final int[] DOWNLINK_FREQUENCY_BAND = {240, 241, 242, 243};
    public static final int[] DOWNLINK_CHANNEL_NUMBER = {244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255};
    public static final int[] UPLINK_FREQUENCY_BAND = {256, 257, 258, 259};
    public static final int[] UPLINK_CHANNEL_NUMBER = {260, 261, 262, 263, 264, 265, 266, 267, 268, 269, 270, 271};
    public static final int[] MULTIPLE_BLOCK_CRC = {320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331, 332,
        333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350, 351};

    private ServiceOptions mServiceOptions;
    private IIdentifier mSourceAddress;
    private IIdentifier mSourceWACN;
    private IIdentifier mSourceSystem;
    private IIdentifier mSourceId;
    private IIdentifier mTargetAddress;
    private IAPCO25Channel mChannel;

    public UnitToUnitVoiceChannelGrantUpdateExtended(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);

        /* Header block is already error detected/corrected - perform error
         * detection correction on the intermediate and final data blocks */
        mMessage = CRCP25.correctPDU2(mMessage);
        mCRC[1] = mMessage.getCRC();
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());
        sb.append(" ").append(getServiceOptions());
        sb.append(" FROM ADDR:");
        sb.append(getSourceAddress());
        sb.append(" ID:");
        sb.append(getSourceID());
        sb.append(" TO:");
        sb.append(getTargetAddress());
        sb.append(" WACN:");
        sb.append(getSourceWACN());
        sb.append(" SYS:");
        sb.append(getSourceSystemID());
        sb.append(" CHAN:" + getChannel());

        return sb.toString();
    }

    public ServiceOptions getServiceOptions()
    {
        if(mServiceOptions == null)
        {
            mServiceOptions = new ServiceOptions(mMessage.getInt(SERVICE_OPTIONS));
        }

        return mServiceOptions;
    }

    public IIdentifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25FromTalkgroup.createIndividual(mMessage.getInt(SOURCE_ADDRESS));
        }

        return mSourceAddress;
    }

    public IIdentifier getSourceID()
    {
        if(mSourceId == null)
        {
            mSourceId = APCO25FromTalkgroup.createIndividual(mMessage.getInt(SOURCE_ID));
        }

        return mSourceId;
    }

    public IIdentifier getSourceWACN()
    {
        if(mSourceWACN == null)
        {
            mSourceWACN = APCO25Wacn.create(mMessage.getInt(SOURCE_WACN));
        }

        return mSourceWACN;
    }

    public IIdentifier getSourceSystemID()
    {
        if(mSourceSystem == null)
        {
            mSourceSystem = APCO25System.create(mMessage.getInt(SOURCE_SYSTEM_ID));
        }

        return mSourceSystem;
    }

    public IIdentifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25ToTalkgroup.createIndividual(mMessage.getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
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
    public List<IAPCO25Channel> getChannels()
    {
        List<IAPCO25Channel> channels = new ArrayList<>();
        channels.add(getChannel());
        return channels;
    }
}
