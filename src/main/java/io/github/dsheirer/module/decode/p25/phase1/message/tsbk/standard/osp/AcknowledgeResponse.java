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

package io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.APCO25System;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Wacn;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.OSPMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.Opcode;
import io.github.dsheirer.module.decode.p25.reference.Direction;
import io.github.dsheirer.module.decode.p25.reference.Vendor;
import java.util.ArrayList;
import java.util.List;

/**
 * Acknowledge response
 */
public class AcknowledgeResponse extends OSPMessage
{
    private static final int ADDITIONAL_INFORMATION_FLAG = 16;
    private static final int EXTENDED_INFORMATION_FLAG = 17;
    private static final int[] SERVICE_TYPE = {18, 19, 20, 21, 22, 23};
    private static final int[] WACN = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43};
    private static final int[] SYSTEM = {44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] SOURCE_ADDRESS = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
            48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] TARGET_ADDRESS = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71,
            72, 73, 74, 75, 76, 77, 78, 79};

    private Identifier mWACN;
    private Identifier mSystemId;
    private Identifier mTargetAddress;
    private Identifier mSourceAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public AcknowledgeResponse(P25P1DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        if(hasSourceAddress())
        {
            sb.append(" FM:").append(getSourceAddress());
        }
        sb.append(" TO:").append(getTargetAddress());
        sb.append(" ACKNOWLEDGING:").append(getAcknowledgedService());
        if(hasWACN())
        {
            sb.append(" WACN:").append(getWACN());
        }
        if(hasSystem())
        {
            sb.append(" SYSTEM:").append(getSystemId());
        }
        return sb.toString();
    }

    private boolean hasAdditionalInformation()
    {
        return getMessage().get(ADDITIONAL_INFORMATION_FLAG);
    }

    private boolean isExtendedSystemInformation()
    {
        return getMessage().get(EXTENDED_INFORMATION_FLAG);
    }

    public boolean hasWACN()
    {
        return hasAdditionalInformation() && isExtendedSystemInformation();
    }

    public Identifier getWACN()
    {
        if(hasWACN())
        {
            if(mWACN == null)
            {
                mWACN = APCO25Wacn.create(getMessage().getInt(WACN));
            }

            return mWACN;
        }

        return null;
    }

    public boolean hasSystem()
    {
        return hasAdditionalInformation() && isExtendedSystemInformation();
    }

    public Identifier getSystemId()
    {
        if(hasSystem())
        {
            if(mSystemId == null)
            {
                mSystemId = APCO25System.create(getMessage().getInt(SYSTEM));
            }

            return mSystemId;
        }

        return null;
    }

    /**
     * Opcode representing the service type that is being acknowledged by the radio unit.
     */
    public Opcode getAcknowledgedService()
    {
        return Opcode.fromValue(getMessage().getInt(SERVICE_TYPE), Direction.INBOUND, Vendor.STANDARD);
    }

    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25RadioIdentifier.createTo(getMessage().getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    public boolean hasSourceAddress()
    {
        return hasAdditionalInformation() && !isExtendedSystemInformation();
    }

    public Identifier getSourceAddress()
    {
        if(hasSourceAddress())
        {
            if(mSourceAddress == null)
            {
                mSourceAddress = APCO25RadioIdentifier.createFrom(getMessage().getInt(SOURCE_ADDRESS));
            }

            return mSourceAddress;
        }

        return null;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
            if(hasSourceAddress())
            {
                mIdentifiers.add(getSourceAddress());
            }

            if(hasWACN())
            {
                mIdentifiers.add(getWACN());
            }
            if(hasSystem())
            {
                mIdentifiers.add(getSystemId());
            }
        }

        return mIdentifiers;
    }
}
