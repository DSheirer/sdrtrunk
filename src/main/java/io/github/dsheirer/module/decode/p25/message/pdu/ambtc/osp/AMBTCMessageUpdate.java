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

package io.github.dsheirer.module.decode.p25.message.pdu.ambtc.osp;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.APCO25System;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Wacn;
import io.github.dsheirer.module.decode.p25.identifier.message.APCO25ShortDataMessage;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.AMBTCMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Message update
 */
public class AMBTCMessageUpdate extends AMBTCMessage
{
    private static final int[] HEADER_WACN = {64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] BLOCK_0_WACN = {0, 1, 2, 3};
    private static final int[] BLOCK_0_SYSTEM = {4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] BLOCK_0_SOURCE_ID = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33,
        34, 35, 36, 37, 38, 39};
    private static final int[] BLOCK_0_SDM = {40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};

    private Identifier mWacn;
    private Identifier mSystem;
    private Identifier mSourceId;
    private Identifier mTargetAddress;
    private Identifier mShortDataMessage;
    private List<Identifier> mIdentifiers;

    public AMBTCMessageUpdate(PDUSequence PDUSequence, int nac, long timestamp)
    {
        super(PDUSequence, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        if(getSourceId() != null)
        {
            sb.append(" FM:").append(getSourceId());
        }
        if(getTargetAddress() != null)
        {
            sb.append(" TO:").append(getTargetAddress());
        }
        if(getWacn() != null)
        {
            sb.append(" WACN:").append(getWacn());
        }
        if(getSystem() != null)
        {
            sb.append(" SYSTEM:").append(getSystem());
        }
        if(getShortDataMessage() != null)
        {
            sb.append(" SHORT DATA MESSAGE:").append(getShortDataMessage());
        }

        return sb.toString();
    }

    public Identifier getWacn()
    {
        if(mWacn == null && hasDataBlock(0))
        {
            int value = getHeader().getMessage().getInt(HEADER_WACN);
            value <<= 4;
            value += getDataBlock(0).getMessage().getInt(BLOCK_0_WACN);
            mWacn = APCO25Wacn.create(value);
        }

        return mWacn;
    }

    public Identifier getSystem()
    {
        if(mSystem == null && hasDataBlock(0))
        {
            mSystem = APCO25System.create(getDataBlock(0).getMessage().getInt(BLOCK_0_SYSTEM));
        }

        return mSystem;
    }

    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null && hasDataBlock(0))
        {
            mTargetAddress = APCO25ToTalkgroup.createIndividual(getDataBlock(0).getMessage().getInt(HEADER_ADDRESS));
        }

        return mTargetAddress;
    }

    public Identifier getSourceId()
    {
        if(mSourceId == null && hasDataBlock(0))
        {
            mSourceId = APCO25FromTalkgroup.createIndividual(getDataBlock(0).getMessage().getInt(BLOCK_0_SOURCE_ID));
        }

        return mSourceId;
    }

    public Identifier getShortDataMessage()
    {
        if(mShortDataMessage == null && hasDataBlock(0))
        {
            mShortDataMessage = APCO25ShortDataMessage.create(getDataBlock(0).getMessage().getInt(BLOCK_0_SDM));
        }

        return mShortDataMessage;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            if(getWacn() != null)
            {
                mIdentifiers.add(getWacn());
            }
            if(getSystem() != null)
            {
                mIdentifiers.add(getSystem());
            }
            if(getSourceId() != null)
            {
                mIdentifiers.add(getSourceId());
            }
            if(getTargetAddress() != null)
            {
                mIdentifiers.add(getTargetAddress());
            }
            if(getShortDataMessage() != null)
            {
                mIdentifiers.add(getShortDataMessage());
            }
        }

        return mIdentifiers;
    }
}
