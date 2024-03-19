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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import java.util.ArrayList;
import java.util.List;

/**
 * Talk complete (but channel will continue to transmit)
 */
public class LCMotorolaTalkComplete extends LinkControlWord
{
    private static final IntField UNKNOWN_FIELD_1 = IntField.length8(OCTET_2_BIT_16);
    private static final IntField UNKNOWN_FIELD_2 = IntField.length8(OCTET_2_BIT_16);
    private static final IntField ADDRESS = IntField.length24(OCTET_6_BIT_48);

    private Identifier mAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCMotorolaTalkComplete(CorrectedBinaryMessage message)
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
            sb.append("MOTOROLA TALK COMPLETE");
            sb.append(" BY:").append(getAddress());
            sb.append(" UNK1:").append(getUnknownField1());
            sb.append(" UNK2:").append(getUnknownField2());
        }
        return sb.toString();
    }

    public String getUnknownField1()
    {
        return getMessage().getHex(UNKNOWN_FIELD_1);
    }

    public String getUnknownField2()
    {
        return getMessage().getHex(UNKNOWN_FIELD_2);
    }

    /**
     * To/From radio identifier communicating with a landline
     */
    public Identifier getAddress()
    {
        if(mAddress == null)
        {
            mAddress = APCO25RadioIdentifier.createFrom(getInt(ADDRESS));
        }

        return mAddress;
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
            mIdentifiers.add(getAddress());
        }

        return mIdentifiers;
    }
}
