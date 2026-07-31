/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3.proprietary;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.Vendor;
import java.util.List;

/**
 * Vendor proprietary message format
 */
public class ProprietaryForm extends NXDNLayer3Message
{
    //OCTET_0 = 0x3F proprietary message type
    private static final IntField VENDOR = IntField.length8(OCTET_1);
    private static final IntField MESSAGE_TYPE = IntField.length8(OCTET_2);

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public ProprietaryForm(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder();
        sb.append("PROPRIETARY FORM - VENDOR:").append(getVendor());
        sb.append(" ID:").append(getVendorID());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Vendor ID
     */
    public int getVendorID()
    {
        return getMessage().getInt(VENDOR);
    }

    /**
     * Vendor ID mapped to the vendor enumeration.
     */
    public Vendor getVendor()
    {
        return Vendor.fromValue(getVendorID());
    }

    /**
     * Utility method to lookup the vendor from a proprietary form message
     * @param message that is proprietary form
     * @return vendor.
     */
    public static Vendor getVendor(CorrectedBinaryMessage message)
    {
        return Vendor.fromValue(message.getInt(VENDOR));
    }

    /**
     * Identifies the message type value from the proprietary form binary message.
     *
     * @param message with message type value in proprietary form.
     * @return value.
     */
    public static int getTypeValue(CorrectedBinaryMessage message)
    {
        return message.getInt(MESSAGE_TYPE);
    }


    @Override
    public NXDNMessageType getMessageType()
    {
        return NXDNMessageType.getProprietary(getVendor(), getMessage().getInt(MESSAGE_TYPE));
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of();
    }
}
