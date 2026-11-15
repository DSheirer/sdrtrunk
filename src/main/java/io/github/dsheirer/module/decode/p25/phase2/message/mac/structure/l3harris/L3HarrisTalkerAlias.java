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
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureVendor;
import io.github.dsheirer.module.decode.p25.reference.Vendor;
import java.util.ArrayList;
import java.util.List;

/**
 * L3Harris Talker Alias.
 */
public class L3HarrisTalkerAlias extends MacStructureVendor
{
    private static final int ALIAS_START = OCTET_4_BIT_24;
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
            sb.append("L3HARRIS");
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
            //The alias fills the rest of the structure after the opcode, vendor and length octets
            int start = ALIAS_START + getOffset();
            int end = Math.min(getOffset() + getLength() * 8, getMessage().size());

            if(end > start)
            {
                byte[] bytes = getMessage().getSubMessage(start, end).getBytes();
                int length = 0;

                //The alias is padded with zero bytes
                while(length < bytes.length && bytes[length] != 0)
                {
                    length++;
                }

                String alias = new String(bytes, 0, length).trim();
                mAliasIdentifier = P25TalkerAliasIdentifier.create(alias);
            }
        }

        return mAliasIdentifier;
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
