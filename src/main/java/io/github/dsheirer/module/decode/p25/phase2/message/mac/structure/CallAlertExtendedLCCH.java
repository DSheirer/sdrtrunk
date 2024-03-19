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
import java.util.ArrayList;
import java.util.List;

/**
 * Call alert extended LCCH
 */
public class CallAlertExtendedLCCH extends MacStructureMultiFragment
{
    private static final IntField SOURCE_ADDRESS = IntField.length24(OCTET_4_BIT_24);
    private static final IntField SOURCE_SUID_WACN = IntField.length20(OCTET_7_BIT_48);
    private static final IntField SOURCE_SUID_SYSTEM = IntField.length12(OCTET_7_BIT_48 + 20);
    private static final IntField SOURCE_SUID_ID = IntField.length24(OCTET_11_BIT_80);
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_14_BIT_104);
    private static final IntField TARGET_SUID_WACN = IntField.length16(OCTET_17_BIT_128);
    private static final IntField FRAGMENT_0_TARGET_SUID_WACN = IntField.length4(OCTET_3_BIT_16);
    private static final IntField FRAGMENT_0_TARGET_SUID_SYSTEM = IntField.length12(OCTET_3_BIT_16 + 4);
    private static final IntField FRAGMENT_0_TARGET_SUID_ID = IntField.length4(OCTET_5_BIT_32);
    private APCO25FullyQualifiedRadioIdentifier mTargetSUID;
    private APCO25FullyQualifiedRadioIdentifier mSourceSUID;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public CallAlertExtendedLCCH(CorrectedBinaryMessage message, int offset)
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
        sb.append(" FM:").append(getSourceSUID());
        if(getTargetSUID() != null)
        {
            sb.append(" TO:").append(getTargetSUID());
        }
        return sb.toString();
    }

    /**
     * To Radio SUID
     */
    public Identifier getTargetSUID()
    {
        if(mTargetSUID == null && hasFragment(0))
        {
            int address = getInt(TARGET_ADDRESS);
            int wacn = getInt(TARGET_SUID_WACN);
            wacn <<= 4;
            wacn += getFragment(0).getInt(FRAGMENT_0_TARGET_SUID_WACN);
            int system = getFragment(0).getInt(FRAGMENT_0_TARGET_SUID_SYSTEM);
            int id = getFragment(0).getInt(FRAGMENT_0_TARGET_SUID_ID);
            mTargetSUID = APCO25FullyQualifiedRadioIdentifier.createTo(address, wacn, system, id);
        }

        return mTargetSUID;
    }

    /**
     * From Radio SUID
     */
    public Identifier getSourceSUID()
    {
        if(mSourceSUID == null)
        {
            int address = getInt(SOURCE_ADDRESS);
            int wacn = getInt(SOURCE_SUID_WACN);
            int system = getInt(SOURCE_SUID_SYSTEM);
            int id = getInt(SOURCE_SUID_ID);
            mSourceSUID = APCO25FullyQualifiedRadioIdentifier.createFrom(address, wacn, system, id);
        }

        return mSourceSUID;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        //Note: this has to be dynamically constructed each time to account for late-add continuation fragments.
        List<Identifier> identifiers = new ArrayList<>();

        if(getSourceSUID() != null)
        {
            identifiers.add(getSourceSUID());
        }

        if(getTargetSUID() != null)
        {
            identifiers.add(getTargetSUID());
        }

        return identifiers;
    }
}
