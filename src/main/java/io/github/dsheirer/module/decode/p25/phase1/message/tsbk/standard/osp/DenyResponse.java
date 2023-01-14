/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.OSPMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.Opcode;
import io.github.dsheirer.module.decode.p25.reference.DenyReason;
import io.github.dsheirer.module.decode.p25.reference.Direction;
import io.github.dsheirer.module.decode.p25.reference.Vendor;

import java.util.ArrayList;
import java.util.List;

/**
 * Deny response
 */
public class DenyResponse extends OSPMessage
{
    private static final int ADDITIONAL_INFORMATION_FLAG = 16;
    private static final int[] SERVICE_TYPE = {18, 19, 20, 21, 22, 23};
    private static final int[] REASON = {24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] ADDITIONAL_INFO = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48,
            49, 50, 51, 52, 53, 54, 55};
    private static final int[] TARGET_ADDRESS = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73,
            74, 75, 76, 77, 78, 79};

    private DenyReason mDenyReason;
    private String mAdditionalInfo;
    private Identifier mTargetAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public DenyResponse(P25P1DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" TO:").append(getTargetAddress());
        sb.append(" SERVICE:").append(getDeniedServiceType());
        sb.append(" REASON:").append(getDenyReason());

        if(hasAdditionalInformation())
        {
            sb.append(" INFO:").append(getAdditionalInfo());
        }

        return sb.toString();
    }

    private boolean hasAdditionalInformation()
    {
        return getMessage().get(ADDITIONAL_INFORMATION_FLAG);
    }

    public String getAdditionalInfo()
    {
        if(mAdditionalInfo == null)
        {
            mAdditionalInfo = getMessage().getHex(ADDITIONAL_INFO, 6);
        }

        return mAdditionalInfo;
    }

    /**
     * Opcode representing the service type that is being acknowledged by the radio unit.
     */
    public Opcode getDeniedServiceType()
    {
        return Opcode.fromValue(getMessage().getInt(SERVICE_TYPE), Direction.INBOUND, Vendor.STANDARD);
    }

    public DenyReason getDenyReason()
    {
        if(mDenyReason == null)
        {
            mDenyReason = DenyReason.fromCode(getMessage().getInt(REASON));
        }

        return mDenyReason;
    }

    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25RadioIdentifier.createTo(getMessage().getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
        }

        return mIdentifiers;
    }
}
