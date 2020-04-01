/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola;

import io.github.dsheirer.bits.BinaryMessage;
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
    private static final int[] UNKNOWN_FIELD_1 = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] UNKNOWN_FIELD_2 = {40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] ADDRESS = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66,
        67, 68, 69, 70, 71};

    private Identifier mAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCMotorolaTalkComplete(BinaryMessage message)
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
            sb.append(" MSG:").append(getMessage().toHexString());
        }
        return sb.toString();
    }

    public String getUnknownField1()
    {
        return getMessage().getHex(UNKNOWN_FIELD_1, 2);
    }

    public String getUnknownField2()
    {
        return getMessage().getHex(UNKNOWN_FIELD_2, 2);
    }

    /**
     * To/From radio identifier communicating with a landline
     */
    public Identifier getAddress()
    {
        if(mAddress == null)
        {
            mAddress = APCO25RadioIdentifier.createFrom(getMessage().getInt(ADDRESS));
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
