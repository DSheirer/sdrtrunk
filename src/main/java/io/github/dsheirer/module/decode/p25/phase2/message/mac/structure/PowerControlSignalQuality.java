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
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.BER;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.RFLevel;
import java.util.Collections;
import java.util.List;

/**
 * Power control signal quality
 */
public class PowerControlSignalQuality extends MacStructure
{
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_2_BIT_8);
    private static final IntField RF_LEVEL = IntField.length4(OCTET_5_BIT_32);
    private static final IntField BIT_ERROR_RATE = IntField.length4(OCTET_5_BIT_32 + 4);

    private List<Identifier> mIdentifiers;
    private Identifier mTargetAddress;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public PowerControlSignalQuality(CorrectedBinaryMessage message, int offset)
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
        sb.append(" RF-LEVEL:").append(getRFLevel());
        sb.append(" BER:").append(getBitErrorRate());
        return sb.toString();
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

    public RFLevel getRFLevel()
    {
        return RFLevel.fromValue(getInt(RF_LEVEL));
    }

    public BER getBitErrorRate()
    {
        return BER.fromValue(getInt(BIT_ERROR_RATE));
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
