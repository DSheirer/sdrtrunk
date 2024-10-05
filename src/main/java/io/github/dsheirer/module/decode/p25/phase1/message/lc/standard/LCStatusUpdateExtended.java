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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.standard;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.status.APCO25UnitStatus;
import io.github.dsheirer.module.decode.p25.identifier.status.APCO25UserStatus;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.ExtendedSourceLinkControlWord;
import java.util.ArrayList;
import java.util.List;

/**
 * Status Update with extended SUID
 */
public class LCStatusUpdateExtended extends ExtendedSourceLinkControlWord
{
    private static final IntField UNIT_STATUS = IntField.length8(OCTET_1_BIT_8);
    private static final IntField USER_STATUS = IntField.length8(OCTET_2_BIT_16);
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_3_BIT_24);

    private Identifier mUnitStatus;
    private Identifier mUserStatus;
    private Identifier mTargetAddress;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     * @param timestamp of the carrier message
     * @param isTerminator to indicate if message is carried by a TDULC terminator message
     */
    public LCStatusUpdateExtended(CorrectedBinaryMessage message, long timestamp, boolean isTerminator)
    {
        super(message, timestamp, isTerminator);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" TO:").append(getTargetAddress());
        if(hasSourceIDExtension())
        {
            sb.append(" FM:").append(getSourceAddress());
        }
        sb.append(" UNIT:").append(getUnitStatus());
        sb.append(" USER:").append(getUserStatus());
        return sb.toString();
    }

    public Identifier getUnitStatus()
    {
        if(mUnitStatus == null)
        {
            mUnitStatus = APCO25UnitStatus.create(getMessage().getInt(UNIT_STATUS));
        }

        return mUnitStatus;
    }

    public Identifier getUserStatus()
    {
        if(mUserStatus == null)
        {
            mUserStatus = APCO25UserStatus.create(getMessage().getInt(USER_STATUS));
        }

        return mUserStatus;
    }

    /**
     * Talkgroup address
     */
    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25RadioIdentifier.createTo(getMessage().getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    /**
     * List of identifiers contained in this message
     */
    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
            if(hasSourceIDExtension())
            {
                mIdentifiers.add(getSourceAddress());
            }
            mIdentifiers.add(getUnitStatus());
            mIdentifiers.add(getUserStatus());
        }

        return mIdentifiers;
    }
}
