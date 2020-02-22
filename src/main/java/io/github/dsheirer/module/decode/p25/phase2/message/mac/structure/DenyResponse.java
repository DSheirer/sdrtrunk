/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacOpcode;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;
import io.github.dsheirer.module.decode.p25.reference.DenyReason;

import java.util.ArrayList;
import java.util.List;

/**
 * Deny response
 */
public class DenyResponse extends MacStructure
{
    private static final int ADDITIONAL_INFORMATION_INDICATOR = 8;
    private static final int[] SERVICE_TYPE = {10, 11, 12, 13, 14, 15};
    private static final int[] REASON = {24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] ADDITIONAL_INFO = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48,
        49, 50, 51, 52, 53, 54, 55};
    private static final int[] TARGET_ADDRESS = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73,
        74, 75, 76, 77, 78, 79};

    private DenyReason mDenyReason;
    private String mAdditionalInfo;
    private Identifier mTargetAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public DenyResponse(CorrectedBinaryMessage message, int offset)
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
        sb.append(" SERVICE:").append(getDeniedServiceType());
        sb.append(" REASON:").append(getDenyReason());

        if(hasAdditionalInformation())
        {
            sb.append(" INFO:").append(getAdditionalInfo());
        }

        return sb.toString();
    }

    private boolean hasAdditionalInformation()
    {
        return getMessage().get(ADDITIONAL_INFORMATION_INDICATOR + getOffset());
    }

    public String getAdditionalInfo()
    {
        if(mAdditionalInfo == null)
        {
            int arguments = getMessage().getInt(ADDITIONAL_INFO, getOffset());
            mAdditionalInfo = Integer.toHexString(arguments).toUpperCase();
        }

        return mAdditionalInfo;
    }

    /**
     * Opcode representing the service type that is being acknowledged by the radio unit.
     */
    public MacOpcode getDeniedServiceType()
    {
        return MacOpcode.fromValue(getMessage().getInt(SERVICE_TYPE, getOffset()));
    }

    public DenyReason getDenyReason()
    {
        if(mDenyReason == null)
        {
            mDenyReason = DenyReason.fromCode(getMessage().getInt(REASON, getOffset()));
        }

        return mDenyReason;
    }

    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25RadioIdentifier.createTo(getMessage().getInt(TARGET_ADDRESS, getOffset()));
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
