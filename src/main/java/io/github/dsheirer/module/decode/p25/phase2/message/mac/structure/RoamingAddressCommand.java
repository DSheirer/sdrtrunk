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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.reference.StackOperation;
import java.util.ArrayList;
import java.util.List;

/**
 * Roaming address command
 */
public class RoamingAddressCommand extends MacStructure
{
    private static final IntField STACK_OPERATION = IntField.length8(OCTET_3_BIT_16);
    private static final IntField TARGET_SUID_WACN = IntField.length20(OCTET_4_BIT_24);
    private static final IntField TARGET_SUID_SYSTEM = IntField.length12(OCTET_6_BIT_40 + 4);
    private static final IntField TARGET_SUID_ID = IntField.length24(OCTET_8_BIT_56);
    private APCO25FullyQualifiedRadioIdentifier mTargetAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public RoamingAddressCommand(CorrectedBinaryMessage message, int offset)
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
        sb.append(" ").append(getStackOperation()).append(" ROAMING ADDRESS STACK");

        return sb.toString();
    }

    public StackOperation getStackOperation()
    {
        return StackOperation.fromValue(getInt(STACK_OPERATION));
    }

    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            int wacn = getInt(TARGET_SUID_WACN);
            int system = getInt(TARGET_SUID_SYSTEM);
            int id = getInt(TARGET_SUID_ID);
            mTargetAddress = APCO25FullyQualifiedRadioIdentifier.createTo(id, wacn, system, id);
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
