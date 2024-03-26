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

package io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.IServiceOptionsProvider;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCMessage;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;
import java.util.ArrayList;
import java.util.List;

public class AMBTCUnitToUnitAnswerRequest extends AMBTCMessage implements IServiceOptionsProvider
{
    private static final int[] HEADER_SERVICE_OPTIONS = {64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] HEADER_RESERVED = {72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] BLOCK_0_RESERVED = {0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] BLOCK_0_SOURCE_WACN = {8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
            25, 26, 27};
    private static final int[] BLOCK_0_SOURCE_SYSTEM = {28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] BLOCK_0_SOURCE_ID = {40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56,
        57, 58, 59, 60, 61, 62, 63};

    private VoiceServiceOptions mVoiceServiceOptions;
    private APCO25FullyQualifiedRadioIdentifier mSourceAddress;
    private Identifier mTargetAddress;
    private List<Identifier> mIdentifiers;

    public AMBTCUnitToUnitAnswerRequest(PDUSequence PDUSequence, int nac, long timestamp)
    {
        super(PDUSequence, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        if(getSourceAddress() != null)
        {
            sb.append(" FM:").append(getSourceAddress());
        }
        sb.append(" TO:").append(getTargetAddress());
        sb.append(" SERVICE OPTIONS:").append(getServiceOptions());
        return sb.toString();
    }

    public VoiceServiceOptions getServiceOptions()
    {
        if(mVoiceServiceOptions == null)
        {
            mVoiceServiceOptions = new VoiceServiceOptions(getHeader().getMessage().getInt(HEADER_SERVICE_OPTIONS));
        }

        return mVoiceServiceOptions;
    }

    public APCO25FullyQualifiedRadioIdentifier getSourceAddress()
    {
        if(mSourceAddress == null && hasDataBlock(0))
        {
            int wacn = getDataBlock(0).getMessage().getInt(BLOCK_0_SOURCE_WACN);
            int system = getDataBlock(0).getMessage().getInt(BLOCK_0_SOURCE_SYSTEM);
            int id = getDataBlock(0).getMessage().getInt(BLOCK_0_SOURCE_ID);
            mSourceAddress = APCO25FullyQualifiedRadioIdentifier.createFrom(id, wacn, system, id);
        }

        return mSourceAddress;
    }

    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25RadioIdentifier.createTo(getHeader().getMessage().getInt(HEADER_ADDRESS));
        }

        return mTargetAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            if(getTargetAddress() != null)
            {
                mIdentifiers.add(getTargetAddress());
            }
            if(getSourceAddress() != null)
            {
                mIdentifiers.add(getSourceAddress());
            }
        }

        return mIdentifiers;
    }
}
