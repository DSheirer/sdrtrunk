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
import io.github.dsheirer.message.AbstractMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacOpcode;
import java.util.List;

/**
 * Structure parsing parent class for MAC message payload structures.
 */
public abstract class MacStructure extends AbstractMessage
{
    protected static final int OCTET_1_BIT_0 = 0;
    protected static final int OCTET_2_BIT_8 = 8;
    protected static final int OCTET_3_BIT_16 = 16;
    protected static final int OCTET_4_BIT_24 = 24;
    protected static final int OCTET_5_BIT_32 = 32;
    protected static final int OCTET_6_BIT_40 = 40;
    protected static final int OCTET_7_BIT_48 = 48;
    protected static final int OCTET_8_BIT_56 = 56;
    protected static final int OCTET_9_BIT_64 = 64;
    protected static final int OCTET_10_BIT_72 = 72;
    protected static final int OCTET_11_BIT_80 = 80;
    protected static final int OCTET_12_BIT_88 = 88;
    protected static final int OCTET_13_BIT_96 = 96;
    protected static final int OCTET_14_BIT_104 = 104;
    protected static final int OCTET_15_BIT_112 = 112;
    protected static final int OCTET_16_BIT_120 = 120;
    protected static final int OCTET_17_BIT_128 = 128;
    protected static final int OCTET_18_BIT_136 = 136;
    protected static final int OCTET_19_BIT_144 = 144;
    protected static final int OCTET_20_BIT_152 = 152;
    private static IntField OPCODE = IntField.length8(OCTET_1_BIT_0);
    private static IntField LCCH_NAC = IntField.length12(OCTET_20_BIT_152);

    /**
     * Constructs a MAC structure parser
     *
     * @param message containing a MAC structure
     * @param offset in the message to the start of the structure
     */
    protected MacStructure(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * List of identifiers provided by this structure
     */
    public abstract List<Identifier> getIdentifiers();

    /**
     * Opcode for the message argument
     * @param message containing a mac opcode message
     * @param offset into the message
     * @return opcode
     */
    public static MacOpcode getOpcode(CorrectedBinaryMessage message, int offset)
    {
        return MacOpcode.fromValue(message.getInt(OPCODE, offset));
    }

    /**
     * Numeric value of the opcode
     * @param message containing a mac opcode message
     * @param offset into the message to the start of the mac sequence
     * @return integer value
     */
    public static int getOpcodeNumber(CorrectedBinaryMessage message, int offset)
    {
        return message.getInt(OPCODE, offset);
    }

    /**
     * Opcode for this message
     */
    public MacOpcode getOpcode()
    {
        return getOpcode(getMessage(), getOffset());
    }

    /**
     * Opcode numeric value for this structure
     */
    public int getOpcodeNumber()
    {
        return getOpcodeNumber(getMessage(), getOffset());
    }

    /**
     * Parses the NAC code from a LCCH MAC PDU content payload.
     * @param message containing a LCCH MAC PDU content payload.
     * @return nac value.
     */
    public static int getLcchNac(CorrectedBinaryMessage message)
    {
        return message.getInt(LCCH_NAC);
    }
}
