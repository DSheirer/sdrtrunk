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
import io.github.dsheirer.module.decode.p25.identifier.status.APCO25UnitStatus;
import io.github.dsheirer.module.decode.p25.identifier.status.APCO25UserStatus;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Status Update
 */
public class AMBTCStatusUpdate extends AMBTCMessage
{
    private static final int[] HEADER_SOURCE_WACN = {64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] BLOCK_0_SOURCE_WACN = {0, 1, 2, 3};
    private static final int[] BLOCK_0_SOURCE_SYSTEM = {4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] BLOCK_0_SOURCE_ID = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33,
        34, 35, 36, 37, 38, 39};
    private static final int[] BLOCK_0_UNIT_STATUS = {40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] BLOCK_0_USER_STATUS = {48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] BLOCK_0_SOURCE_ADDRESS = {80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95};
    private static final int[] BLOCK_1_SOURCE_ADDRESS = {0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] BLOCK_1_TARGET_WACN = {8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
            25, 26, 27};
    private static final int[] BLOCK_1_TARGET_SYSTEM = {28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] BLOCK_1_TARGET_ID = {40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56,
            57, 58, 59, 60, 61, 62, 63};

    private APCO25FullyQualifiedRadioIdentifier mSourceAddress;
    private APCO25FullyQualifiedRadioIdentifier mTargetAddress;
    private Identifier mUnitStatus;
    private Identifier mUserStatus;
    private List<Identifier> mIdentifiers;

    public AMBTCStatusUpdate(PDUSequence PDUSequence, int nac, long timestamp)
    {
        super(PDUSequence, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        if(getTargetAddress() != null)
        {
            sb.append(" TO:").append(getTargetAddress());
        }
        if(getSourceAddress() != null)
        {
            sb.append(" FM:").append(getSourceAddress());
        }
        if(getUnitStatus() != null)
        {
            sb.append(" UNIT STATUS:").append(getUnitStatus());
        }
        if(getUserStatus() != null)
        {
            sb.append(" USER STATUS:").append(getUserStatus());
        }

        return sb.toString();
    }

    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null && hasDataBlock(0))
        {
            int localAddress = getHeader().getMessage().getInt(HEADER_ADDRESS);
            int wacn = getDataBlock(1).getMessage().getInt(BLOCK_1_TARGET_WACN);
            int system = getDataBlock(1).getMessage().getInt(BLOCK_1_TARGET_SYSTEM);
            int id = getDataBlock(1).getMessage().getInt(BLOCK_1_TARGET_ID);
            mTargetAddress = APCO25FullyQualifiedRadioIdentifier.createTo(localAddress, wacn, system, id);
        }

        return mTargetAddress;
    }

    public Identifier getSourceAddress()
    {
        if(mSourceAddress == null && hasDataBlock(0) && hasDataBlock(1))
        {
            int wacn = getHeader().getMessage().getInt(HEADER_SOURCE_WACN);
            wacn <<= 4;
            wacn += getDataBlock(0).getMessage().getInt(BLOCK_0_SOURCE_WACN);
            int system = getDataBlock(0).getMessage().getInt(BLOCK_0_SOURCE_SYSTEM);
            int id = getDataBlock(0).getMessage().getInt(BLOCK_0_SOURCE_ID);
            int localAddress = getDataBlock(0).getMessage().getInt(BLOCK_0_SOURCE_ADDRESS) << 8;
            localAddress += getDataBlock(1).getMessage().getInt(BLOCK_1_SOURCE_ADDRESS);

            mSourceAddress = APCO25FullyQualifiedRadioIdentifier.createFrom(localAddress, wacn, system, id);
        }

        return mSourceAddress;
    }

    public Identifier getUnitStatus()
    {
        if(mUnitStatus == null && hasDataBlock(0))
        {
            mUnitStatus = APCO25UnitStatus.create(getDataBlock(0).getMessage().getInt(BLOCK_0_UNIT_STATUS));
        }

        return mUnitStatus;
    }

    public Identifier getUserStatus()
    {
        if(mUserStatus == null && hasDataBlock(0))
        {
            mUserStatus = APCO25UserStatus.create(getDataBlock(0).getMessage().getInt(BLOCK_0_USER_STATUS));
        }

        return mUserStatus;
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
            if(getTargetAddress() != null)
            {
                mIdentifiers.add(getTargetAddress());
            }
            if(getUserStatus() != null)
            {
                mIdentifiers.add(getUserStatus());
            }
            if(getUnitStatus() != null)
            {
                mIdentifiers.add(getUnitStatus());
            }
        }

        return mIdentifiers;
    }
}
