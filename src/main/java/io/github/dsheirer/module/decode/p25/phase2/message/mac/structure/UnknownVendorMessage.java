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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;
import io.github.dsheirer.module.decode.p25.reference.Vendor;

import java.util.Collections;
import java.util.List;

/**
 * Unknown Vendor Message
 */
public class UnknownVendorMessage extends MacStructure
{
    private static final int[] VENDOR = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] LENGTH = {18, 19, 20, 21, 22, 23};

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public UnknownVendorMessage(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MANUFACTURER MESSAGE VENDOR:").append(getVendor());
        sb.append(" LENGTH:").append(getMessageLength());
        sb.append(" MSG:").append(getMessage().getSubMessage(getOffset(), getMessage().size()).toHexString());

        return sb.toString();
    }

    public Vendor getVendor()
    {
        return Vendor.fromValue(getMessage().getInt(VENDOR, getOffset()));
    }

    /**
     * Length of this message
     */
    public int getMessageLength()
    {
        return getMessage().getInt(LENGTH, getOffset());
    }


    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
