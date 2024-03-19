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
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import java.util.Collections;
import java.util.List;

/**
 * Motorola Link Control opcode 0x17 (23).  This is possibly a radio reprogramming record segment that is used
 * in combination with LCO 0x15.  See notes in header of LCMotorolaRadioReprogramHeader class.
 */
public class LCMotorolaRadioReprogramRecord extends LinkControlWord
{
    private static final IntField RECORD_NUMBER = IntField.length8(OCTET_2_BIT_16);
    private static final IntField SEQUENCE_NUMBER = IntField.length4(OCTET_3_BIT_24);

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCMotorolaRadioReprogramRecord(CorrectedBinaryMessage message)
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

        sb.append("MOTOROLA RADIO REPROGRAM RECORD:").append(getRecordNumber());
        sb.append(" OF SEQUENCE:").append(getSequenceNumber());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Record number
     */
    public int getRecordNumber()
    {
        return getInt(RECORD_NUMBER);
    }

    /**
     * Sequence number
     */
    public int getSequenceNumber()
    {
        return getInt(SEQUENCE_NUMBER);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
