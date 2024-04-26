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
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Link Control Opcode 0x15.
 *
 * Note: I suspect this is some form of radio reprogramming message, possibly used to update encryption.
 *
 * I've seen LCOpcode 0x15 and 0x17 twice and in both cases it was sent in a TDULC at the end of a call for the same
 * radio on the CNYICC system.  The header references the talkgroup that the radio is calling, so it may be some form
 * of dynamic regrouping, but the total content is quite large.
 *
 * Note: this seems to only be targeted to certain radios since it wasn't sent at the end of every call.
 *
 * Examples observed on traffic channels in TDULC at the end of a call following the Motorola Talk Complete message
 * for Radio: 15,104,082 (0xE67852) and TG:7101 (0x1BBD)
 * Example 1
 *
 * 1590 1BBD 07 0100 F 8BA / 1BBD=TG, 07=record count, 0100=?, F=sequence number, 8BA=crc checksum
 * 1790 01 F BEE00 2AE E67 /  BEE00=WACN, 2AE=SYS, E67...= RADIO ID
 * 1790 02 F 852 83ED1081 / ...852=RADIO ID cont.
 * 1790 03 F E33C03E9B3E
 * 1790 04 F 35647DE0C00
 * 1790 05 F C8A83E351E4
 * 1790 06 F 079F592CF37
 * 1790 07 F 94B30000000
 *
 * Example 2
 * 1590 1BBD070100 4 955
 * 1790 01 4 BEE002AEE67
 * 1790 02 4 85283ED1081
 * 1790 03 4 E33C03E9B3E
 * 1790 04 4 35647DE0C00
 * 1790 05 4 C8A83E351E4
 * 1790 06 4 079F592CF37
 * 1790 07 4 94B30000000
 *
 * The LCO 15 header starts with the Talkgroup value and seems to identify the count of continuation messages to
 * follow (0x07).  In the LCO 17 continuation messages, the first octet after the vendor seems to be a message record
 * count ID, ranging from 1 to 7.  The next nibble in the continuation messages seems to be a continuation message
 * sequence number to let you know that the continuation messages are all part of the same sequence.
 *
 * The reprogramming payload sequence starts by sending the full SUID for the radio (BEE00.2AE.E67852).
 */
public class LCMotorolaRadioReprogramHeader extends LinkControlWord
{
    private static final IntField TALKGROUP = IntField.length16(OCTET_2_BIT_16);
    private static final IntField RECORD_COUNT = IntField.length8(OCTET_4_BIT_32);
    private static final IntField SEQUENCE_NUMBER = IntField.length4(OCTET_7_BIT_56);
    private static final IntField SEQUENCE_CHECKSUM = IntField.length12(OCTET_7_BIT_56 + 4);
    private Identifier mTalkgroup;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCMotorolaRadioReprogramHeader(CorrectedBinaryMessage message)
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
        sb.append("MOTOROLA RADIO REPROGRAM HEADER");
        sb.append(" TG:").append(getTalkgroup());
        sb.append(" RECORD COUNT:").append(getRecordCount());
        sb.append(" SEQUENCE:").append(getSequenceNumber());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Talkgroup
     */
    public Identifier getTalkgroup()
    {
        if(mTalkgroup == null)
        {
            mTalkgroup = APCO25Talkgroup.create(getInt(TALKGROUP));
        }

        return mTalkgroup;
    }

    /**
     * Count of continuation messages to follow
     * @return
     */
    public int getRecordCount()
    {
        return getInt(RECORD_COUNT);
    }

    /**
     * Sequence number
     */
    public int getSequenceNumber()
    {
        return getInt(SEQUENCE_NUMBER);
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
            mIdentifiers.add(getTalkgroup());
        }

        return mIdentifiers;
    }
}
