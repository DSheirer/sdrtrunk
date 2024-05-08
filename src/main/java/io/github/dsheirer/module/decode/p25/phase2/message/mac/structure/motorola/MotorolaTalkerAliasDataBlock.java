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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureVendor;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Talker Alias continuation block Opcode 149 (0x95).  This follows Motorola Talker Alias header message.
 *
 * Examples:
 * 959011 018E58B82BAB4D3B70E9A8457F9D C67
 * 959011 0287000000000000000000000000 E26
 *
 * The opcode, vendor and length octets are consistent.  The first octet seems to be an identifier, 01, 02, etc.
 * Nothing else seems to match any of the other identifiers that were active at call time.
 */
public class MotorolaTalkerAliasDataBlock extends MacStructureVendor
{
    private static final IntField BLOCK_NUMBER = IntField.length8(OCTET_4_BIT_24);
    private static final IntField SEQUENCE = IntField.length4(OCTET_5_BIT_32);
    private static final int FRAGMENT_START = OCTET_5_BIT_32 + 4;
    private static final int FRAGMENT_END = OCTET_18_BIT_136;

    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public MotorolaTalkerAliasDataBlock(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MOTOROLA TALKER ALIAS DATA BLOCK:").append(getBlockNumber());
        sb.append(" OF SEQUENCE:").append(getSequence());
        sb.append(" FRAGMENT:").append(getFragment().toHexString());
        sb.append(" MSG:").append(getMessage().get(getOffset(), getMessage().length()).toHexString());
        return sb.toString();
    }

    /**
     * Fragment of encoded alias.
     */
    public BinaryMessage getFragment()
    {
        return getMessage().getSubMessage(getOffset() + FRAGMENT_START, getOffset() + FRAGMENT_END);
    }

    /**
     * Block number
     */
    public int getBlockNumber()
    {
        return getInt(BLOCK_NUMBER);
    }

    /**
     * Sequence number that identifies the header and data blocks as all part of the same sequence.
     */
    public int getSequence()
    {
        return getInt(SEQUENCE);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
        }

        return mIdentifiers;
    }
}