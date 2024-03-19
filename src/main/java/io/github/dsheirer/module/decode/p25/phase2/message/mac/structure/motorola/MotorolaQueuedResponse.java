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
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacOpcode;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureVendor;
import io.github.dsheirer.module.decode.p25.reference.QueuedResponseReason;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Queued response
 */
public class MotorolaQueuedResponse extends MacStructureVendor
{
    private static final int ADDITIONAL_INFORMATION_INDICATOR = 24;
    private static final IntField SERVICE_TYPE = IntField.range(26, 31);
    private static final IntField REASON = IntField.length8(OCTET_5_BIT_32);
    private static final IntField ADDITIONAL_INFO = IntField.length24(OCTET_6_BIT_40);
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_9_BIT_64);

    private QueuedResponseReason mQueuedResponseReason;
    private String mAdditionalInfo;
    private Identifier mTargetAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public MotorolaQueuedResponse(CorrectedBinaryMessage message, int offset)
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
        sb.append(" SERVICE:").append(getQueuedResponseServiceType());
        sb.append(" REASON:").append(getQueuedResponseReason());

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
            mAdditionalInfo = Integer.toHexString(getInt(ADDITIONAL_INFO)).toUpperCase();
        }

        return mAdditionalInfo;
    }

    /**
     * Opcode representing the service type that is being acknowledged by the radio unit.
     */
    public MacOpcode getQueuedResponseServiceType()
    {
        return MacOpcode.fromValue(getInt(SERVICE_TYPE));
    }

    public QueuedResponseReason getQueuedResponseReason()
    {
        if(mQueuedResponseReason == null)
        {
            mQueuedResponseReason = QueuedResponseReason.fromCode(getInt(REASON));
        }

        return mQueuedResponseReason;
    }

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
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
        }

        return mIdentifiers;
    }
}
