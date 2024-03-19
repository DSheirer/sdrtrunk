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
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Roaming address update message.  Assigns WACN and SYSTEM combinations to a target address.
 *
 * Note: this is a variable block length message containing between 2 and 8 possible combination
 * values.  This class only parses out the first WACN an SYSTEM combination.  Further parsing
 * support is required to fully extract the additional combinations, but they are basically stacked
 * one after another in each of the subsequent data blocks with the final data block containing the
 * 4 CRC octets.
 */
public class AMBTCRoamingAddressUpdate extends AMBTCMessage
{
    private static final int LAST_MESSAGE_FLAG = 64;
    private static final int[] HEADER_MSN = {68, 69, 70, 71};
    private static final int[] HEADER_WACN_A = {72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] BLOCK_0_WACN_A = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    private static final int[] BLOCK_0_SYSTEM_A = {12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] BLOCK_0_WACN_B = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41,
            42, 43};
    private static final int[] BLOCK_0_SYSTEM_B = {44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] BLOCK_0_WACN_C = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73,
            74, 75};
    private static final int[] BLOCK_0_SYSTEM_C = {76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87};
    private static final int[] BLOCK_0_WACN_D = {88, 89, 90, 91, 92, 93, 94, 95};
    private static final int[] BLOCK_1_WACN_D = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    private static final int[] BLOCK_1_SYSTEM_D = {12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] BLOCK_1_WACN_E = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41,
            42, 43};
    private static final int[] BLOCK_1_SYSTEM_E = {44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] BLOCK_2_WACN_F = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73,
            74, 75};
    private static final int[] BLOCK_2_SYSTEM_F = {76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87};
    private static final int[] BLOCK_2_WACN_G = {88, 89, 90, 91, 92, 93, 94, 95};
    private static final int[] BLOCK_3_WACN_G = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    private static final int[] BLOCK_3_SYSTEM_G = {12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};

    //Source ID is in the same location in Blocks 1, 2, and 3
    private static final int[] SOURCE_ID = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42,
            43, 44, 45, 46, 47};

    private Identifier mTargetAddress;
    private APCO25FullyQualifiedRadioIdentifier mRoamingAddressA;
    private APCO25FullyQualifiedRadioIdentifier mRoamingAddressB;
    private APCO25FullyQualifiedRadioIdentifier mRoamingAddressC;
    private APCO25FullyQualifiedRadioIdentifier mRoamingAddressD;
    private APCO25FullyQualifiedRadioIdentifier mRoamingAddressE;
    private APCO25FullyQualifiedRadioIdentifier mRoamingAddressF;
    private APCO25FullyQualifiedRadioIdentifier mRoamingAddressG;
    private List<Identifier> mIdentifiers;

    public AMBTCRoamingAddressUpdate(PDUSequence PDUSequence, int nac, long timestamp)
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
        sb.append(getMessageStub()).append(" ROAMING ADDRESSES");
        if(getRoamingAddressA() != null)
        {
            sb.append(" A:").append(getRoamingAddressA());
        }
        if(getRoamingAddressB() != null)
        {
            sb.append(" B:").append(getRoamingAddressB());
        }
        if(getRoamingAddressC() != null)
        {
            sb.append(" C:").append(getRoamingAddressC());
        }
        if(getRoamingAddressD() != null)
        {
            sb.append(" D:").append(getRoamingAddressD());
        }
        if(getRoamingAddressE() != null)
        {
            sb.append(" E:").append(getRoamingAddressE());
        }
        if(getRoamingAddressF() != null)
        {
            sb.append(" F:").append(getRoamingAddressF());
        }
        if(getRoamingAddressG() != null)
        {
            sb.append(" G:").append(getRoamingAddressG());
        }
        sb.append(" MSN:").append(getMessageSequenceNumber());
        if(isLastMessage())
        {
            sb.append(" (FINAL)");
        }

        return sb.toString();
    }

    public boolean isLastMessage()
    {
        return getHeader().getMessage().get(LAST_MESSAGE_FLAG);
    }

    private int getMessageSequenceNumber()
    {
        return getHeader().getMessage().getInt(HEADER_MSN);
    }

    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null && hasDataBlock(0))
        {
            mTargetAddress = APCO25RadioIdentifier.createTo(getHeader().getMessage().getInt(HEADER_ADDRESS));
        }

        return mTargetAddress;
    }

    private int getAddress()
    {
        if(hasDataBlock(3))
        {
            return getDataBlock(3).getMessage().getInt(SOURCE_ID);
        }
        else if(hasDataBlock(2))
        {
            return getDataBlock(2).getMessage().getInt(SOURCE_ID);
        }
        else if(hasDataBlock(1))
        {
            return getDataBlock(1).getMessage().getInt(SOURCE_ID);
        }

        return 0;
    }

    public APCO25FullyQualifiedRadioIdentifier getRoamingAddressA()
    {
        if(mRoamingAddressA == null && hasDataBlock(0))
        {
            int wacn = getHeader().getMessage().getInt(HEADER_WACN_A) << 12;
            wacn += getDataBlock(0).getMessage().getInt(BLOCK_0_WACN_A);
            int system = getDataBlock(0).getMessage().getInt(BLOCK_0_SYSTEM_A);
            int id = getAddress();
            mRoamingAddressA = APCO25FullyQualifiedRadioIdentifier.createFrom(id, wacn, system, id);
        }

        return mRoamingAddressA;
    }

    public APCO25FullyQualifiedRadioIdentifier getRoamingAddressB()
    {
        if(mRoamingAddressB == null && hasDataBlock(0))
        {
            int wacn = getHeader().getMessage().getInt(BLOCK_0_WACN_B);
            int system = getDataBlock(0).getMessage().getInt(BLOCK_0_SYSTEM_B);
            int id = getAddress();
            mRoamingAddressB = APCO25FullyQualifiedRadioIdentifier.createFrom(id, wacn, system, id);
        }

        return mRoamingAddressB;
    }

    public APCO25FullyQualifiedRadioIdentifier getRoamingAddressC()
    {
        if(mRoamingAddressC == null && hasDataBlock(0))
        {
            int wacn = getHeader().getMessage().getInt(BLOCK_0_WACN_C);
            int system = getDataBlock(0).getMessage().getInt(BLOCK_0_SYSTEM_C);
            int id = getAddress();
            mRoamingAddressC = APCO25FullyQualifiedRadioIdentifier.createFrom(id, wacn, system, id);
        }

        return mRoamingAddressC;
    }

    public APCO25FullyQualifiedRadioIdentifier getRoamingAddressD()
    {
        if(mRoamingAddressD == null && hasDataBlock(0) && hasDataBlock(1))
        {
            int wacn = getDataBlock(0).getMessage().getInt(BLOCK_0_WACN_D) << 12;
            wacn += getDataBlock(1).getMessage().getInt(BLOCK_1_WACN_D);
            int system = getDataBlock(1).getMessage().getInt(BLOCK_1_SYSTEM_D);
            int id = getAddress();
            mRoamingAddressD = APCO25FullyQualifiedRadioIdentifier.createFrom(id, wacn, system, id);
        }

        return mRoamingAddressD;
    }

    public APCO25FullyQualifiedRadioIdentifier getRoamingAddressE()
    {
        if(mRoamingAddressE == null && hasDataBlock(1))
        {
            int wacn = getHeader().getMessage().getInt(BLOCK_1_WACN_E);
            int system = getDataBlock(0).getMessage().getInt(BLOCK_1_SYSTEM_E);
            int id = getAddress();
            mRoamingAddressE = APCO25FullyQualifiedRadioIdentifier.createFrom(id, wacn, system, id);
        }

        return mRoamingAddressE;
    }

    public APCO25FullyQualifiedRadioIdentifier getRoamingAddressF()
    {
        if(mRoamingAddressF == null && hasDataBlock(2))
        {
            int wacn = getHeader().getMessage().getInt(BLOCK_2_WACN_F);
            int system = getDataBlock(0).getMessage().getInt(BLOCK_2_SYSTEM_F);
            int id = getAddress();
            mRoamingAddressF = APCO25FullyQualifiedRadioIdentifier.createFrom(id, wacn, system, id);
        }

        return mRoamingAddressF;
    }

    public APCO25FullyQualifiedRadioIdentifier getRoamingAddressG()
    {
        if(mRoamingAddressG == null && hasDataBlock(2) && hasDataBlock(3))
        {
            int wacn = getDataBlock(0).getMessage().getInt(BLOCK_2_WACN_G) << 12;
            wacn += getDataBlock(1).getMessage().getInt(BLOCK_3_WACN_G);
            int system = getDataBlock(1).getMessage().getInt(BLOCK_3_SYSTEM_G);
            int id = getAddress();
            mRoamingAddressG = APCO25FullyQualifiedRadioIdentifier.createFrom(id, wacn, system, id);
        }

        return mRoamingAddressG;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            if(getRoamingAddressA() != null)
            {
                mIdentifiers.add(getRoamingAddressA());
            }
            if(getRoamingAddressB() != null)
            {
                mIdentifiers.add(getRoamingAddressB());
            }
            if(getRoamingAddressC() != null)
            {
                mIdentifiers.add(getRoamingAddressC());
            }
            if(getRoamingAddressD() != null)
            {
                mIdentifiers.add(getRoamingAddressD());
            }
            if(getRoamingAddressE() != null)
            {
                mIdentifiers.add(getRoamingAddressE());
            }
            if(getRoamingAddressF() != null)
            {
                mIdentifiers.add(getRoamingAddressF());
            }
            if(getRoamingAddressG() != null)
            {
                mIdentifiers.add(getRoamingAddressG());
            }
        }

        return mIdentifiers;
    }
}
