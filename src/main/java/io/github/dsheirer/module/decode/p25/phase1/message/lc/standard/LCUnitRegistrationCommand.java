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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.standard;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import java.util.Collections;
import java.util.List;

/**
 * Unit registration command
 * Note: this message is obsolete as of TIA-102.AABF-A-1
 */
public class LCUnitRegistrationCommand extends LinkControlWord
{
    private static final IntField WACN = IntField.length20(OCTET_1_BIT_8);
    private static final IntField SYSTEM_ID = IntField.length12(OCTET_3_BIT_24 + 4);
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_5_BIT_40);
    private FullyQualifiedRadioIdentifier mTargetAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCUnitRegistrationCommand(CorrectedBinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" RADIO:").append(getTargetAddress());
        return sb.toString();
    }

    /**
     * Target address
     */
    public FullyQualifiedRadioIdentifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            //No persona ... reuse the radio ID.
            int wacn = getInt(WACN);
            int system = getInt(SYSTEM_ID);
            int radio = getInt(TARGET_ADDRESS);
            mTargetAddress = APCO25FullyQualifiedRadioIdentifier.createTo(radio, wacn, system, radio);
        }

        return mTargetAddress;
    }

    /**
     * List of identifiers contained in this message
     */
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
