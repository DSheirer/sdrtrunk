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
import java.util.ArrayList;
import java.util.List;

/**
 * Call alert extended VCH
 */
public class CallAlertExtendedVCH extends MacStructure
{
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_2_BIT_8);
    private static final IntField SOURCE_SUID_WACN = IntField.length20(OCTET_5_BIT_32);
    private static final IntField SOURCE_SUID_SYSTEM = IntField.length12(OCTET_5_BIT_32 + 20);
    private static final IntField SOURCE_SUID_ADDRESS = IntField.length24(OCTET_9_BIT_64);

    private List<Identifier> mIdentifiers;
    private Identifier mTargetAddress;
    private Identifier mSourceSuid;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public CallAlertExtendedVCH(CorrectedBinaryMessage message, int offset)
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
        sb.append(" FM:").append(getSourceSuid());
        sb.append(" TO:").append(getTargetAddress());
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

    /**
     * From Radio Unit
     */
    public Identifier getSourceSuid()
    {
        if(mSourceSuid == null)
        {
            int wacn = getInt(SOURCE_SUID_WACN);
            int system = getInt(SOURCE_SUID_SYSTEM);
            int radio = getInt(SOURCE_SUID_ADDRESS);
            //Fully qualified, but not aliased - reuse the address as the persona.
            mSourceSuid = APCO25FullyQualifiedRadioIdentifier.createFrom(radio, wacn, system, radio);
        }

        return mSourceSuid;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
            mIdentifiers.add(getSourceSuid());
        }

        return mIdentifiers;
    }
}
