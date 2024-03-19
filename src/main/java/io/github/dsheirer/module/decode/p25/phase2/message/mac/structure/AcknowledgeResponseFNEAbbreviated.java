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
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacOpcode;
import java.util.ArrayList;
import java.util.List;

/**
 * Acknowledge response FNE abbreviated
 */
public class AcknowledgeResponseFNEAbbreviated extends MacStructure
{
    private static final int ADDITIONAL_INFORMATION_INDICATOR = 8;
    private static final int EXTENDED_ADDRESS = 9;
    private static final IntField SERVICE_TYPE = IntField.range(10, 15);
    private static final IntField TARGET_WACN = IntField.range(16, 35);
    private static final IntField TARGET_SYSTEM = IntField.range(36, 48);
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_7_BIT_48);
    private static final IntField SOURCE_ADDRESS = IntField.length24(OCTET_4_BIT_24);
    
    private List<Identifier> mIdentifiers;
    private Identifier mTargetAddress;
    private Identifier mSourceAddress;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public AcknowledgeResponseFNEAbbreviated(CorrectedBinaryMessage message, int offset)
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
     * Indicates if this message contains additional information fields.
     */
    public boolean hasAdditionalInformation()
    {
        return getMessage().get(ADDITIONAL_INFORMATION_INDICATOR + getOffset());
    }

    /**
     * Indicates if this message has extended addressing
     */
    public boolean hasExtendedAddressing()
    {
        return getMessage().get(EXTENDED_ADDRESS + getOffset());
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
        if(mTargetAddress == null)
        {
            int address = getInt(TARGET_ADDRESS);

            if(hasAdditionalInformation() && hasExtendedAddressing())
            {
                int wacn = getInt(TARGET_WACN);
                int system = getInt(TARGET_SYSTEM);
                mTargetAddress = APCO25FullyQualifiedRadioIdentifier.createTo(address, wacn, system, address);
            }
            else
            {
                mTargetAddress = APCO25RadioIdentifier.createTo(address);
            }
        }

        return mTargetAddress;
    }

    /**
     * From Radio Unit
     */
    public Identifier getSourceAddress()
    {
        if(hasAdditionalInformation() && !hasExtendedAddressing() && mSourceAddress == null)
        {
            mSourceAddress = APCO25RadioIdentifier.createFrom(getInt(SOURCE_ADDRESS));
        }

        return mSourceAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            if(getTargetAddress() != null)
            {
                mIdentifiers.add(getTargetAddress());
            }
            if(getSourceAddress() != null)
            {
                mIdentifiers.add(getSourceAddress());
            }
        }

        return mIdentifiers;
    }
}
