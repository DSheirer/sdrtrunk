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

package io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25Radio;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCMessage;
import io.github.dsheirer.module.decode.p25.reference.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit registration response - when the source unit does not match the WACN id for the current site
 */
public class AMBTCUnitRegistrationResponse extends AMBTCMessage
{
    private static final int[] HEADER_WACN = {64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] BLOCK_0_WACN = {0, 1, 2, 3};
    private static final int[] BLOCK_0_SYSTEM = {4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] BLOCK_0_SOURCE_ID = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33,
        34, 35, 36, 37, 38, 39};
    private static final int[] BLOCK_0_SOURCE_ADDRESS = {40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55,
        56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] BLOCK_0_RESPONSE = {70, 71};

    private Response mResponse;
    private Identifier mTargetAddress;
    private APCO25FullyQualifiedRadioIdentifier mFullyQualifiedTargetAddress;
    private Identifier mSourceAddress;
    private List<Identifier> mIdentifiers;

    public AMBTCUnitRegistrationResponse(PDUSequence PDUSequence, int nac, long timestamp)
    {
        super(PDUSequence, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append("REGISTRATION ").append(getResponse());
        if(getFullyQualifiedTargetAddress() != null)
        {
            sb.append(" TO:").append(getFullyQualifiedTargetAddress());
        }
        if(getTargetAddress() != null)
        {
            sb.append(" ALIASED AS:").append(getTargetAddress());
        }
        if(getSourceAddress() != null)
        {
            sb.append(" FROM:").append(getSourceAddress());
        }

        return sb.toString();
    }

    public Response getResponse()
    {
        if(mResponse == null && hasDataBlock(0))
        {
            mResponse = Response.fromValue(getDataBlock(0).getMessage().getInt(BLOCK_0_RESPONSE));
        }

        return mResponse;
    }

    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null && hasDataBlock(0))
        {
            mTargetAddress = APCO25Radio.createTo(getDataBlock(0).getMessage().getInt(HEADER_ADDRESS));
        }

        return mTargetAddress;
    }

    public Identifier getFullyQualifiedTargetAddress()
    {
        if(mFullyQualifiedTargetAddress == null && hasDataBlock(0))
        {
            int wacn = getHeader().getMessage().getInt(HEADER_WACN);
            wacn <<= 4;
            wacn += getDataBlock(0).getMessage().getInt(BLOCK_0_WACN);
            int system = getDataBlock(0).getMessage().getInt(BLOCK_0_SYSTEM);
            int id = getDataBlock(0).getMessage().getInt(BLOCK_0_SOURCE_ID);

            mFullyQualifiedTargetAddress = APCO25FullyQualifiedRadioIdentifier.createTo(wacn, system, id);
        }

        return mFullyQualifiedTargetAddress;
    }

    public Identifier getSourceAddress()
    {
        if(mSourceAddress == null && hasDataBlock(0))
        {
            mSourceAddress = APCO25Radio.createFrom(getDataBlock(0).getMessage().getInt(BLOCK_0_SOURCE_ADDRESS));
        }

        return mSourceAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            if(getFullyQualifiedTargetAddress() != null)
            {
                mIdentifiers.add(getFullyQualifiedTargetAddress());
            }
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
