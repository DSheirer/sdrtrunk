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

package io.github.dsheirer.module.decode.p25.phase1.message.tsbk;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Vendor;

/**
 * Vendor custom TSBK Outbound message
 */
public abstract class VendorOSPMessage extends OSPMessage
{
    private static final IntField VENDOR = IntField.length8(OCTET_1_BIT_8);

    /**
     * Constructs an inbound (ISP) TSBK from the binary message sequence.
     *
     * @param dataUnitID TSBK1/2/3
     * @param message binary sequence
     * @param nac decoded from the NID
     * @param timestamp for the message
     */
    public VendorOSPMessage(P25P1DataUnitID dataUnitID, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitID, message, nac, timestamp);
    }

    /**
     * Utility method to identify the vendor for the specified message.
     * @param message containing a vendor code.
     * @return vendor or UNKNOWN.
     */
    public static Vendor getVendor(CorrectedBinaryMessage message)
    {
        return Vendor.fromValue(message.getInt(VENDOR));
    }

    /**
     * Vendor for this message
     * @return vendor
     */
    public Vendor getVendor()
    {
        return Vendor.fromValue(getInt(VENDOR));
    }
}
