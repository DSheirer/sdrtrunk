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
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.reference.ExtendedFunction;
import java.util.ArrayList;
import java.util.List;

/**
 * Extended function command to a target address
 */
public class LCExtendedFunctionCommand extends LinkControlWord
{
    private static final IntField EXTENDED_FUNCTION = IntField.length16(OCTET_1_BIT_8);
    private static final IntField EXTENDED_FUNCTION_ARGUMENTS = IntField.length24(OCTET_3_BIT_24);
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_6_BIT_48);

    private Identifier mTargetAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCExtendedFunctionCommand(CorrectedBinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" TO:").append(getTargetAddress());
        sb.append(" ").append(getExtendedFunction());
        sb.append(" ARGUMENTS:").append(getExtendedFunctionArguments());
        return sb.toString();
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
        }

        return mIdentifiers;
    }
}
