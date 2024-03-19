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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import java.util.ArrayList;
import java.util.List;

/**
 * L3Harris Unknown Opcode 10 (0x0A)
 * <p>
 * Observed in a traffic channel carried by a TDU during an SNDCP packet data session.  The controller send an 'All
 * blocks received' PDU to the radio in the middle of a stream of these TDU/LC messages and the traffic channel ended
 * with Call Termination TDULC messages.  So, this may be some form of current data session user information update.
 */
public class LCHarrisUnknownOpcode10 extends LinkControlWord
{
    private static final IntField UNKNOWN_1 = IntField.length8(OCTET_2_BIT_16);
    private static final IntField TARGET_RADIO = IntField.length24(OCTET_3_BIT_24);
    private static final IntField SOURCE_RADIO = IntField.length24(OCTET_6_BIT_48);
    private RadioIdentifier mSourceRadio;
    private RadioIdentifier mTargetRadio;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCHarrisUnknownOpcode10(CorrectedBinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("**CRC-FAILED** ");
        }

        if(isEncrypted())
        {
            sb.append(" ENCRYPTED");
        }
        else
        {
            sb.append("L3HARRIS UNKNOWN OPCODE 10 ");
            sb.append(" FM:").append(getSourceRadio());
            sb.append(" TO:").append(getTargetRadio());
            sb.append(" UNK:").append(getUnknown());
            sb.append(" MSG:").append(getMessage().toHexString());
        }
        return sb.toString();
    }

    public String getUnknown()
    {
        return getMessage().getHex(UNKNOWN_1);
    }

    /**
     * Source radio identifier
     */
    public RadioIdentifier getSourceRadio()
    {
        if(mSourceRadio == null)
        {
            mSourceRadio = APCO25RadioIdentifier.createFrom(getInt(SOURCE_RADIO));
        }

        return mSourceRadio;
    }

    /**
     * Target radio identifier
     */
    public RadioIdentifier getTargetRadio()
    {
        if(mTargetRadio == null)
        {
            mTargetRadio = APCO25RadioIdentifier.createTo(getInt(TARGET_RADIO));
        }

        return mTargetRadio;
    }

    /**
     * List of identifiers contained in this message
     */
    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getSourceRadio());
        }

        return mIdentifiers;
    }
}
