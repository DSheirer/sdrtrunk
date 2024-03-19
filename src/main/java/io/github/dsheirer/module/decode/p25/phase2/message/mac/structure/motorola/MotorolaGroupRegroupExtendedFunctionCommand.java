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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureVendor;
import io.github.dsheirer.module.decode.p25.reference.ArgumentType;
import io.github.dsheirer.module.decode.p25.reference.ExtendedFunction;
import java.util.Collections;
import java.util.List;

/**
 * Motorola Group Regroup Extended Function Command - Create or Cancel Supergroup
 */
public class MotorolaGroupRegroupExtendedFunctionCommand extends MacStructureVendor
{
    private static final IntField EXTENDED_FUNCTION = IntField.length16(OCTET_4_BIT_24); //Class & Operand
    private static final IntField ARGUMENTS = IntField.length24(OCTET_6_BIT_40);
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_9_BIT_64);
    private ExtendedFunction mExtendedFunction;
    private Identifier mArgumentAddress;
    private Identifier mTargetAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public MotorolaGroupRegroupExtendedFunctionCommand(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getOpcode());
        sb.append(" TO:").append(getTargetAddress());
        ExtendedFunction function = getExtendedFunction();

        if(function == ExtendedFunction.UNKNOWN)
        {
            sb.append(" UNKNOWN FUNCTION CODE:0x").append(Integer.toHexString(getInt(EXTENDED_FUNCTION)).toUpperCase());
        }
        else
        {
            sb.append(" ").append(function);
        }

        if(getArgumentAddress() != null)
        {
            sb.append(" ").append(getArgumentAddress());
        }

        return sb.toString();
    }


    public ExtendedFunction getExtendedFunction()
    {
        if(mExtendedFunction == null)
        {
            mExtendedFunction = ExtendedFunction.fromValue(getInt(EXTENDED_FUNCTION));
        }

        return mExtendedFunction;
    }

    public Identifier getArgumentAddress()
    {
        if(mArgumentAddress == null)
        {
            ArgumentType type = getExtendedFunction().getArgumentType();

            switch(type)
            {
                case TALKGROUP:
                    mArgumentAddress = APCO25Talkgroup.create(getInt(ARGUMENTS));
                case SOURCE_RADIO:
                    mArgumentAddress = APCO25RadioIdentifier.createFrom(getInt(ARGUMENTS));
                case TARGET_RADIO:
                    mArgumentAddress = APCO25RadioIdentifier.createTo(getInt(ARGUMENTS));
                default:
                    //Do nothing and leave the variable null.
            }
        }

        return mArgumentAddress;
    }

    /**
     * To Talkgroup
     */
    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25RadioIdentifier.createTo(getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = Collections.singletonList(getTargetAddress());
        }

        return mIdentifiers;
    }
}
