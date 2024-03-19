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
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
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
    private static final int[] BLOCK_0_RADIO_ID = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33,
        34, 35, 36, 37, 38, 39};
    private static final int[] BLOCK_0_RESPONSE = {46, 47};

    private Response mResponse;
    private APCO25FullyQualifiedRadioIdentifier mRegistrationAddress;
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
        if(getRegistrationAddress() != null)
        {
            sb.append(" FOR:").append(getRegistrationAddress());
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

    public Identifier getRegistrationAddress()
    {
        if(mRegistrationAddress == null && hasDataBlock(0))
        {
            int localAddress = getHeader().getMessage().getInt(HEADER_ADDRESS);
            int wacn = getHeader().getMessage().getInt(HEADER_WACN);
            wacn <<= 4;
            wacn += getDataBlock(0).getMessage().getInt(BLOCK_0_WACN);
            int system = getDataBlock(0).getMessage().getInt(BLOCK_0_SYSTEM);
            int id = getDataBlock(0).getMessage().getInt(BLOCK_0_RADIO_ID);

            mRegistrationAddress = APCO25FullyQualifiedRadioIdentifier.createTo(localAddress, wacn, system, id);
        }

        return mRegistrationAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();

            if(getRegistrationAddress() != null)
            {
                mIdentifiers.add(getRegistrationAddress());
            }
        }

        return mIdentifiers;
    }
}
