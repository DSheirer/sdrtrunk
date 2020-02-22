/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.APCO25System;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Wacn;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCMessage;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;

import java.util.ArrayList;
import java.util.List;

public class AMBTCUnitToUnitVoiceServiceRequest extends AMBTCMessage
{
    private static final int[] HEADER_SERVICE_OPTIONS = {64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] HEADER_RESERVED = {72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] BLOCK_0_RESERVED = {0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] BLOCK_0_WACN = {8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26,
        27};
    private static final int[] BLOCK_0_SYSTEM = {28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] BLOCK_0_TARGET_ID = {40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56,
        57, 58, 59, 60, 61, 62, 63};

    private VoiceServiceOptions mVoiceServiceOptions;
    private Identifier mWacn;
    private Identifier mSystem;
    private Identifier mSourceAddress;
    private Identifier mTargetId;
    private List<Identifier> mIdentifiers;

    public AMBTCUnitToUnitVoiceServiceRequest(PDUSequence PDUSequence, int nac, long timestamp)
    {
        super(PDUSequence, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" FM:").append(getSourceAddress());
        if(getTargetId() != null)
        {
            sb.append(" TO:").append(getTargetId());
        }
        if(getWacn() != null)
        {
            sb.append(" WACN:").append(getWacn());
        }
        if(getSystem() != null)
        {
            sb.append(" SYSTEM:").append(getSystem());
        }
        sb.append(" SERVICE OPTIONS:").append(getVoiceServiceOptions());
        return sb.toString();
    }

    public Identifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25RadioIdentifier.createFrom(getHeader().getMessage().getInt(HEADER_ADDRESS));
        }

        return mSourceAddress;
    }

    public VoiceServiceOptions getVoiceServiceOptions()
    {
        if(mVoiceServiceOptions == null)
        {
            mVoiceServiceOptions = new VoiceServiceOptions(getHeader().getMessage().getInt(HEADER_SERVICE_OPTIONS));
        }

        return mVoiceServiceOptions;
    }

    public Identifier getWacn()
    {
        if(mWacn == null && hasDataBlock(0))
        {
            mWacn = APCO25Wacn.create(getDataBlock(0).getMessage().getInt(BLOCK_0_WACN));
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

    public Identifier getTargetId()
    {
        if(mTargetId == null && hasDataBlock(0))
        {
            mTargetId = APCO25RadioIdentifier.createTo(getDataBlock(0).getMessage().getInt(BLOCK_0_TARGET_ID));
        }

        return mTargetId;
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
            if(getTargetId() != null)
            {
                mIdentifiers.add(getTargetId());
            }
        }

        return mIdentifiers;
    }
}
