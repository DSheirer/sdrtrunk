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

package io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.isp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.VendorISPMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Inbound extended function command or acknowledgement from the SU.
 */
public class MotorolaExtendedFunctionResponse extends VendorISPMessage
{
    private static final IntField CLASS = IntField.length8(OCTET_2_BIT_16);
    private static final IntField OPERAND = IntField.length8(OCTET_3_BIT_24);
    private static final IntField ARGUMENTS = IntField.length24(OCTET_4_BIT_32);
    private static final IntField SOURCE_ADDRESS = IntField.length24(OCTET_7_BIT_56);

    private TalkgroupIdentifier mSupergroup;
    private RadioIdentifier mSourceAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an inbound (ISP) TSBK from the binary message sequence.
     *
     * @param dataUnitID TSBK1/2/3
     * @param message binary sequence
     * @param nac decoded from the NID
     * @param timestamp for the message
     */
    public MotorolaExtendedFunctionResponse(P25P1DataUnitID dataUnitID, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitID, message, nac, timestamp);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" ").append(getDescription());
        return super.toString();
    }

    public String getDescription()
    {
        if(isAcknowledgeSupergroupCancel())
        {
            return "ACKNOWLEDGE CANCEL SUPERGROUP:" + getSuperGroup();
        }
        else if(isAcknowledgeSupergroupCreate())
        {
            return "ACKNOWLEDGE CREATE SUPERGROUP:" + getSuperGroup();
        }
        else
        {
            return "UNRECOGNIZED EXTENDED FUNCTION CLASS:" + getFunctionClass() + " OPERAND:" + getFunctionOperand() +
                    " ARGUMENTS:" + getFunctionArguments();
        }
    }

    /**
     * Indicates if this is an acknowledge supergroup create command.
     */
    public boolean isAcknowledgeSupergroupCreate()
    {
        return getFunctionClass() == 0x02 && getFunctionOperand() == 0x80;
    }

    /**
     * Indicates if this is an acknowledge supergroup cancel command.
     */
    public boolean isAcknowledgeSupergroupCancel()
    {
        return getFunctionClass() == 0x02 && getFunctionOperand() == 0x81;
    }

    /**
     *
     * @return
     */
    public TalkgroupIdentifier getSuperGroup()
    {
        if(mSupergroup == null)
        {
            mSupergroup = APCO25Talkgroup.create(getInt(ARGUMENTS));
        }

        return mSupergroup;
    }

    /**
     * Class
     */
    public int getFunctionClass()
    {
        return getInt(CLASS);
    }

    /**
     * Operand
     */
    public int getFunctionOperand()
    {
        return getInt(OPERAND);
    }

    /**
     * Arguments
     */
    public int getFunctionArguments()
    {
        return getInt(ARGUMENTS);
    }

    /**
     * Requesting SU radio.
     */
    public RadioIdentifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25RadioIdentifier.createFrom(getInt(SOURCE_ADDRESS));
        }

        return mSourceAddress;
    }

    /**
     * Collective identifiers available in this message.
     */
    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getSourceAddress());
            if(getFunctionClass() == 0x02)
            {
                mIdentifiers.add(getSuperGroup());
            }
        }

        return mIdentifiers;
    }
}
