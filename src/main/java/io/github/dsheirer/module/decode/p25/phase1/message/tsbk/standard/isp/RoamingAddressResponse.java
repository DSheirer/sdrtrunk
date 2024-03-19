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

package io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.ISPMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Roaming address response
 */
public class RoamingAddressResponse extends ISPMessage
{
    private static final int LAST_MESSAGE_FLAG = 16;
    private static final int[] MESSAGE_SEQUENCE_NUMBER = {20, 21, 22, 23};
    private static final int[] WACN = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43};
    private static final int[] SYSTEM = {44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] SOURCE_ID = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73,
            74, 75, 76, 77, 78, 79};

    private APCO25FullyQualifiedRadioIdentifier mRoamingAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public RoamingAddressResponse(P25P1DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" ROAMING AS:").append(getRoamingAddress());
        sb.append(" MESSAGE #").append(getMessageSequenceNumber());
        if(isLastMessage())
        {
            sb.append("-FINAL ");
        }
        return sb.toString();
    }

    public boolean isLastMessage()
    {
        return getMessage().get(LAST_MESSAGE_FLAG);
    }

    public int getMessageSequenceNumber()
    {
        return getMessage().getInt(MESSAGE_SEQUENCE_NUMBER);
    }

    public Identifier getRoamingAddress()
    {
        if(mRoamingAddress == null)
        {
            int wacn = getMessage().getInt(WACN);
            int system = getMessage().getInt(SYSTEM);
            int id = getMessage().getInt(SOURCE_ID);
            mRoamingAddress = APCO25FullyQualifiedRadioIdentifier.createTo(id, wacn, system, id);
        }

        return mRoamingAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getRoamingAddress());
        }

        return mIdentifiers;
    }
}
