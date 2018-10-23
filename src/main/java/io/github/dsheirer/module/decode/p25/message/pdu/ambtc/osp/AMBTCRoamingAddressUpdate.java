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

package io.github.dsheirer.module.decode.p25.message.pdu.ambtc.osp;

import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.node.APCO25System;
import io.github.dsheirer.identifier.integer.node.APCO25Wacn;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.pdu.PDUSequence;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.AMBTCMessage;

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

    private IIdentifier mWacn;
    private IIdentifier mSystem;
    private IIdentifier mTargetAddress;
    private List<IIdentifier> mIdentifiers;

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
        if(getWacn() != null)
        {
            sb.append(" WACN A:").append(getWacn());
        }
        if(getSystem() != null)
        {
            sb.append(" SYSTEM A:").append(getSystem());
        }
        sb.append(" MSN:").append(getMessageSequenceNumber());
        if(isLastMessage())
        {
            sb.append(" (FINAL)");
        }

        sb.append(" TOTAL WACN/SYSTEM COMBOS IN MESSAGE:").append(getWacnCount());

        return sb.toString();
    }

    private int getWacnCount()
    {
        int dataBlockCount = getPDUSequence().getDataBlocks().size();

        switch(dataBlockCount)
        {
            case 1:
                return 2;
            case 2:
                return 5;
            case 3:
                return 8;
            default:
                return 2;
        }
    }

    public boolean isLastMessage()
    {
        return getHeader().getMessage().get(LAST_MESSAGE_FLAG);
    }

    private int getMessageSequenceNumber()
    {
        return getHeader().getMessage().getInt(HEADER_MSN);
    }

    public IIdentifier getWacn()
    {
        if(mWacn == null && hasDataBlock(0))
        {
            int value = getHeader().getMessage().getInt(HEADER_WACN_A);
            value <<= 12;
            value += getDataBlock(0).getMessage().getInt(BLOCK_0_WACN_A);
            mWacn = APCO25Wacn.create(value);
        }

        return mWacn;
    }

    public IIdentifier getSystem()
    {
        if(mSystem == null && hasDataBlock(0))
        {
            mSystem = APCO25System.create(getDataBlock(0).getMessage().getInt(BLOCK_0_SYSTEM_A));
        }

        return mSystem;
    }

    public IIdentifier getTargetAddress()
    {
        if(mTargetAddress == null && hasDataBlock(0))
        {
            mTargetAddress = APCO25ToTalkgroup.createIndividual(getDataBlock(0).getMessage().getInt(HEADER_ADDRESS));
        }

        return mTargetAddress;
    }

    @Override
    public List<IIdentifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            if(getWacn() != null)
            {
                mIdentifiers.add(getWacn());
            }
            if(getSystem() != null)
            {
                mIdentifiers.add(getSystem());
            }
            if(getTargetAddress() != null)
            {
                mIdentifiers.add(getTargetAddress());
            }
        }

        return mIdentifiers;
    }
}
