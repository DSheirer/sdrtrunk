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
import io.github.dsheirer.identifier.radio.FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.ExtendedSourceLinkControlWord;
import io.github.dsheirer.module.decode.p25.reference.ExtendedFunction;
import java.util.ArrayList;
import java.util.List;

/**
 * Extended function command to a target address with an extended SUID
 */
public class LCExtendedFunctionCommandExtended extends ExtendedSourceLinkControlWord
{
    private static final IntField EXTENDED_FUNCTION = IntField.length16(OCTET_1_BIT_8);
    private static final IntField EXTENDED_FUNCTION_ARGUMENTS = IntField.length24(OCTET_3_BIT_24);
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_6_BIT_48);

    private Identifier mTargetAddress;
    private FullyQualifiedRadioIdentifier mSourceAddress;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     * @param timestamp of the carrier message
     * @param isTerminator to indicate if message is carried by a TDULC terminator message
     */
    public LCExtendedFunctionCommandExtended(CorrectedBinaryMessage message, long timestamp, boolean isTerminator)
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
        sb.append(" ").append(getExtendedFunction());
        sb.append(" ARGUMENTS:").append(getExtendedFunctionArguments());
        return sb.toString();
    }

    /**
     * Source address.  Doesn't use the parent class method since the source address value is not available.
     */
    @Override
    public FullyQualifiedRadioIdentifier getSourceAddress()
    {
        if(mSourceAddress == null && hasSourceIDExtension())
        {
            int wacn = getSourceIDExtension().getWACN();
            int system = getSourceIDExtension().getSystem();
            int id = getSourceIDExtension().getId();
            mSourceAddress = APCO25FullyQualifiedRadioIdentifier.createFrom(id, wacn, system, id);
        }

        return mSourceAddress;
    }


    /**
     * Indicates the type of extended function
     */
    public ExtendedFunction getExtendedFunction()
    {
        return ExtendedFunction.fromValue(getInt(EXTENDED_FUNCTION));
    }

    /**
     * Argument(s).  May not be required for the class/function and will be set to null (0) if not required.
     */
    public String getExtendedFunctionArguments()
    {
        return getMessage().getHex(EXTENDED_FUNCTION_ARGUMENTS);
    }

    /**
     * Talkgroup address
     */
    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25RadioIdentifier.createTo(getInt(TARGET_ADDRESS));
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
        }

        return mIdentifiers;
    }
}
