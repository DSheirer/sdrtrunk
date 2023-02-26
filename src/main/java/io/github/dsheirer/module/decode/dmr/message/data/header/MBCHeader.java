/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.data.header;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.Opcode;
import io.github.dsheirer.module.decode.dmr.message.type.AnnouncementType;
import io.github.dsheirer.module.decode.dmr.message.type.Vendor;
import java.util.Collections;
import java.util.List;

/**
 * Multi-Block Header for Multiple Block CSBK Messages
 */
public class MBCHeader extends CSBKMessage
{
    private static final int[] ANNOUNCEMENT_TYPE = new int[]{16, 17, 18, 19, 20};

    /**
     * Constructs an instance
     *
     * @param syncPattern for the CSBK
     * @param message bits
     * @param cach for the DMR burst
     * @param slotType for this message
     * @param timestamp
     * @param timeslot
     */
    public MBCHeader(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    /**
     * CRC Mask to use for this message
     */
    protected int getCRCMask()
    {
        return 0xAAAA;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CC:").append(getSlotType().getColorCode());

        if(!isValid())
        {
            sb.append(" [CRC ERROR]");
        }

        if(isEncrypted())
        {
            sb.append(" ENCRYPTED");
        }

        sb.append(" MULTI-BLOCK CSBK HEADER");

        Vendor vendor = getVendor();

        if(vendor != Vendor.STANDARD)
        {
            if(vendor == Vendor.UNKNOWN)
            {
                sb.append(" VENDOR:UNKNOWN (").append(getVendorID()).append(")");
            }
            else
            {
                sb.append(" ").append(vendor);
            }
        }

        Opcode opcode = getOpcode();

        if(opcode == Opcode.UNKNOWN)
        {
            sb.append(" UNKNOWN CSBKO:").append(getOpcodeValue());
        }
        else if(opcode == Opcode.HYTERA_08_ANNOUNCEMENT)
        {
            sb.append(" ANNOUNCEMENT:").append(AnnouncementType.fromValue(getMessage().getInt(ANNOUNCEMENT_TYPE)));
        }
        else
        {
            sb.append(" ").append(opcode);
        }

        sb.append(" MSG:").append(getMessage().toHexString());

        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
