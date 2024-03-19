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
 * Radio unit monitor command extended VCH
 */
public class RadioUnitMonitorCommandExtendedVCH extends RadioUnitMonitorCommand
{
    private static final IntField SOURCE_SUID_WACN = IntField.length20(OCTET_8_BIT_56);
    private static final IntField SOURCE_SUID_SYSTEM = IntField.length12(OCTET_10_BIT_72 + 4);
    private static final IntField SOURCE_SUID_ID = IntField.length24(OCTET_12_BIT_88);
    private List<Identifier> mIdentifiers;
    private APCO25FullyQualifiedRadioIdentifier mSourceSuid;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public RadioUnitMonitorCommandExtendedVCH(CorrectedBinaryMessage message, int offset)
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

        if(isSilentMonitor())
        {
            sb.append(" SILENT MONITORING");
        }
        sb.append(" TIME:").append(getTransmitTime());
        sb.append(" MULTIPLIER:").append(getTransmitMultiplier());
        return sb.toString();
    }

    /**
     * From Radio Unit
     */
    public APCO25FullyQualifiedRadioIdentifier getSourceSuid()
    {
        if(mSourceSuid == null)
        {
            int wacn = getMessage().getInt(SOURCE_SUID_WACN, getOffset());
            int system = getMessage().getInt(SOURCE_SUID_SYSTEM, getOffset());
            int id = getMessage().getInt(SOURCE_SUID_ID, getOffset());
            //Fully qualified, but not aliased - reuse the ID as the persona.
            mSourceSuid = APCO25FullyQualifiedRadioIdentifier.createFrom(id, wacn, system, id);
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
