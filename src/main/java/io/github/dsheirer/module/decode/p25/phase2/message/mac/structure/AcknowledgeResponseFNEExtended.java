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
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacOpcode;
import java.util.ArrayList;
import java.util.List;

/**
 * Acknowledge response FNE extended
 */
public class AcknowledgeResponseFNEExtended extends MacStructureMultiFragment
{
    private static final IntField SERVICE_TYPE = IntField.range(26, 31);
    private static final IntField SOURCE_SUID_WACN = IntField.range(OCTET_5_BIT_32, OCTET_5_BIT_32 + 20);
    private static final IntField SOURCE_SUID_SYSTEM = IntField.range(52, 63);
    private static final IntField SOURCE_SUID_ID = IntField.length24(OCTET_9_BIT_64);
    private static final IntField TARGET_SUID_WACN = IntField.range(OCTET_12_BIT_88, OCTET_12_BIT_88 + 20);
    private static final IntField TARGET_SUID_SYSTEM = IntField.range(108, 119);
    private static final IntField TARGET_SUID_ID = IntField.length24(OCTET_16_BIT_120);
    private static final IntField FRAGMENT_0_SOURCE_ADDRESS = IntField.length24(OCTET_3_BIT_16);
    private static final IntField FRAGMENT_0_TARGET_ADDRESS = IntField.length24(OCTET_6_BIT_40);

    private Identifier mSourceAddress;
    private Identifier mTargetAddress;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public AcknowledgeResponseFNEExtended(CorrectedBinaryMessage message, int offset)
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

        if(getTargetAddress() != null)
        {
            sb.append(" TO:").append(getTargetAddress());
        }

        if(getSourceAddress() != null)
        {
            sb.append(" FM:").append(getSourceAddress());
        }

        sb.append(" ACKNOWLEDGING:").append(getServiceType()).append(" OPCODE:").append(getInt(SERVICE_TYPE));
        return sb.toString();
    }

    /**
     * The service type opcode associated with the acknowledgment
     */
    public MacOpcode getServiceType()
    {
        return MacOpcode.fromValue(getInt(SERVICE_TYPE));
    }

    /**
     * To Talkgroup
     */
    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null && hasFragment(0))
        {
            int address = getFragment(0).getInt(FRAGMENT_0_TARGET_ADDRESS);
            int wacn = getInt(TARGET_SUID_WACN);
            int system = getInt(TARGET_SUID_SYSTEM);
            int id = getInt(TARGET_SUID_ID);
            mTargetAddress = APCO25FullyQualifiedRadioIdentifier.createTo(address, wacn, system, id);
        }

        return mTargetAddress;
    }

    /**
     * From Radio Unit
     */
    public Identifier getSourceAddress()
    {
        if(mSourceAddress == null && hasFragment(0))
        {
            int address = getFragment(0).getInt(FRAGMENT_0_SOURCE_ADDRESS);
            int wacn = getInt(SOURCE_SUID_WACN);
            int system = getInt(SOURCE_SUID_SYSTEM);
            int id = getInt(SOURCE_SUID_ID);
            mSourceAddress = APCO25FullyQualifiedRadioIdentifier.createFrom(address, wacn, system, id);
        }

        return mSourceAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        //Note: this has to be dynamically constructed each time to account for late-add continuation fragments.
        List<Identifier> identifiers = new ArrayList<>();

        if(getSourceAddress() != null)
        {
            identifiers.add(getSourceAddress());
        }

        if(getTargetAddress() != null)
        {
            identifiers.add(getTargetAddress());
        }

        return identifiers;
    }
}
