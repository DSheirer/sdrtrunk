/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Wacn;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacOpcode;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;

import java.util.ArrayList;
import java.util.List;

/**
 * Acknowledge response
 */
public class AcknowledgeResponse extends MacStructure
{
    private static final int ADDITIONAL_INFORMATION_INDICATOR = 8;
    private static final int EXTENDED_ADDRESS = 9;
    private static final int[] SERVICE_TYPE = {10, 11, 12, 13, 14, 15};

    private static final int[] TARGET_WACN = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33,
        34, 35};
    private static final int[] TARGET_SYSTEM = {36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] TARGET_ADDRESS = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64,
        65, 66, 67, 68, 69, 70, 71};
    private static final int[] SOURCE_ADDRESS = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
        41, 42, 43, 44, 45, 46, 47};

    private List<Identifier> mIdentifiers;

    private Identifier mTargetAddress;
    private Identifier mTargetWacn;
    private Identifier mTargetSystem;
    private Identifier mSourceAddress;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public AcknowledgeResponse(CorrectedBinaryMessage message, int offset)
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

        if(hasAdditionalInformation())
        {
            if(hasExtendedAddressing())
            {
                sb.append(" WACN:").append(getTargetWacn());
                sb.append(" SYS:").append(getTargetSystem());
            }
            else
            {
                sb.append(" FM:").append(getSourceAddress());
            }
        }

        sb.append(" ACKNOWLEDGING:").append(getServiceType().toString());

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
        return MacOpcode.fromValue(getMessage().getInt(SERVICE_TYPE, getOffset()));
    }

    /**
     * To Talkgroup
     */
    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25RadioIdentifier.createTo(getMessage().getInt(TARGET_ADDRESS, getOffset()));
        }

        return mTargetAddress;
    }

    public Identifier getTargetWacn()
    {
        if(hasAdditionalInformation() && hasExtendedAddressing() && mTargetWacn == null)
        {
            mTargetWacn = APCO25Wacn.create(getMessage().getInt(TARGET_WACN, getOffset()));
        }

        return mTargetWacn;
    }

    public Identifier getTargetSystem()
    {
        if(hasAdditionalInformation() && hasExtendedAddressing() && mTargetSystem == null)
        {
            mTargetSystem = APCO25Wacn.create(getMessage().getInt(TARGET_SYSTEM, getOffset()));
        }

        return mTargetSystem;
    }

    /**
     * From Radio Unit
     */
    public Identifier getSourceAddress()
    {
        if(hasAdditionalInformation() && !hasExtendedAddressing() && mSourceAddress == null)
        {
            mSourceAddress = APCO25RadioIdentifier.createFrom(getMessage().getInt(SOURCE_ADDRESS, getOffset()));
        }

        return mSourceAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());

            if(hasAdditionalInformation())
            {
                if(hasExtendedAddressing())
                {
                    mIdentifiers.add(getTargetWacn());
                    mIdentifiers.add(getTargetSystem());
                }
                else
                {
                    mIdentifiers.add(getSourceAddress());
                }
            }
        }

        return mIdentifiers;
    }
}
