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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.alias.P25TalkerAliasIdentifier;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;
import io.github.dsheirer.module.decode.p25.reference.Vendor;
import java.util.ArrayList;
import java.util.List;

/**
 * L3Harris Talker Alias.
 */
public class L3HarrisTalkerAlias extends MacStructure
{
    private static final int[] VENDOR = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] LENGTH = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int ALIAS_START = 24;

    private List<Identifier> mIdentifiers;
    private P25TalkerAliasIdentifier mAliasIdentifier;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public L3HarrisTalkerAlias(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(getVendor() == Vendor.HARRIS)
        {
            sb.append("L3HARRIS ");
        }
        else
        {
            sb.append("WARNING: UNKNOWN VENDOR:").append(getVendor());
        }

        sb.append(" TALKER ALIAS: ");
        sb.append(getAlias());
        return sb.toString();
    }

    /**
     * Alias identifier
     */
    public P25TalkerAliasIdentifier getAlias()
    {
        if(mAliasIdentifier == null)
        {
            int length = getLength();

            if(length > 0)
            {
                length *= 8;
                length += getOffset();

                if(length > getMessage().size())
                {
                    length = getMessage().size();
                }

                String alias = new String(getMessage().getSubMessage(ALIAS_START + getOffset(), length).getBytes()).trim();
                mAliasIdentifier = P25TalkerAliasIdentifier.create(alias);
            }
        }

        return mAliasIdentifier;
    }

    /**
     * Vendor ID.  This should be L3Harris unless another vendor is also using this Opcode.
     */
    public Vendor getVendor()
    {
        return Vendor.fromValue(getMessage().getInt(VENDOR, getOffset()));
    }

    /**
     * Message length.
     *
     * @return length in bytes, including the opcode.
     */
    public int getLength()
    {
        return getMessage().getInt(LENGTH, getOffset());
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getAlias());
        }

        return mIdentifiers;
    }
}
