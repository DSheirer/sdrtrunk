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

package io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Lra;
import io.github.dsheirer.module.decode.p25.identifier.APCO25System;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Wacn;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25IdentifierTalkgroup;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.AMBTCMessage;

import java.util.ArrayList;
import java.util.List;

public class AMBTCLocationRegistrationRequest extends AMBTCMessage
{
    private static final int[] HEADER_WACN = {64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] BLOCK_0_WACN = {0, 1, 2, 3};
    private static final int[] BLOCK_0_SYSTEM = {4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] BLOCK_0_SOURCE_ID = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32,
        33, 34, 35, 36, 37, 38, 39};
    private static final int[] BLOCK_0_PREVIOUS_LRA = {40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] BLOCK_0_GROUP_ADDRESS = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58,
        59, 60, 61, 62, 63};

    private Identifier mWacn;
    private Identifier mSystem;
    private Identifier mSourceId;
    private Identifier mSourceAddress;
    private Identifier mGroupAddress;
    private Identifier mPreviousLra;
    private List<Identifier> mIdentifiers;

    public AMBTCLocationRegistrationRequest(PDUSequence PDUSequence, int nac, long timestamp)
    {
        super(PDUSequence, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" FM ADDR:").append(getSourceAddress());
        if(getSourceId() != null)
        {
            sb.append(" FM ID:").append(getSourceId());
        }
        if(getGroupAddress() != null)
        {
            sb.append(" TO:").append(getGroupAddress());
        }
        if(getWacn() != null)
        {
            sb.append(" WACN:").append(getWacn());
        }
        if(getSystem() != null)
        {
            sb.append(" SYSTEM:").append(getSystem());
        }
        if(getPreviousLra() != null)
        {
            sb.append(" PREVIOUS LRA:").append(getPreviousLra());
        }
        return sb.toString();
    }

    public Identifier getGroupAddress()
    {
        if(mGroupAddress == null && hasDataBlock(0))
        {
            mGroupAddress = APCO25ToTalkgroup.createGroup(getDataBlock(0).getMessage().getInt(BLOCK_0_GROUP_ADDRESS));
        }

        return mGroupAddress;
    }

    public Identifier getPreviousLra()
    {
        if(mPreviousLra == null && hasDataBlock(0))
        {
            mPreviousLra = APCO25Lra.create(getDataBlock(0).getMessage().getInt(BLOCK_0_PREVIOUS_LRA));
        }

        return mPreviousLra;
    }

    public Identifier getSourceId()
    {
        if(mSourceId == null)
        {
            mSourceId = APCO25IdentifierTalkgroup.createIndividual(getHeader().getMessage().getInt(BLOCK_0_SOURCE_ID));
        }

        return mSourceId;
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

    public Identifier getSourceAddress()
    {
        if(mSourceAddress == null && hasDataBlock(0))
        {
            mSourceAddress = APCO25FromTalkgroup.createIndividual(getDataBlock(0).getMessage().getInt(HEADER_ADDRESS));
        }

        return mSourceAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            if(getSourceAddress() != null)
            {
                mIdentifiers.add(getSourceAddress());
            }
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
            if(getGroupAddress() != null)
            {
                mIdentifiers.add(getGroupAddress());
            }
            if(getPreviousLra() != null)
            {
                mIdentifiers.add(getPreviousLra());
            }
        }

        return mIdentifiers;
    }
}
