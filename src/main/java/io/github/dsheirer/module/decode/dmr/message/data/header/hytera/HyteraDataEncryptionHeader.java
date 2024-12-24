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

package io.github.dsheirer.module.decode.dmr.message.data.header.hytera;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.header.ProprietaryDataHeader;
import io.github.dsheirer.module.decode.dmr.message.type.EncryptionAlgorithm;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;

/**
 * Hytera Proprietary Data Header for SAP Short Data
 */
public class HyteraDataEncryptionHeader extends ProprietaryDataHeader
{
    private static final int[] ALGORITHM = new int[]{16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] KEY = new int[]{24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] INITIALIZATION_VECTOR = new int[]{32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
            48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] UNKNOWN = new int[]{64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};

    /**
     * Constructs an instance.
     *
     * @param syncPattern either BASE_STATION_DATA or MOBILE_STATION_DATA
     * @param message containing extracted 196-bit payload.
     * @param cach for the DMR burst
     * @param slotType for this data message
     * @param timestamp message was received
     * @param timeslot for the DMR burst
     */
    public HyteraDataEncryptionHeader(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
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
        sb.append(" HYTERA DATA ENCRYPTION HEADER - SAP:").append(getServiceAccessPoint());

        switch(getAlgorithm())
        {
            case NO_ENCRYPTION:
                sb.append(" ALGORITHM:UNENCRYPTED");
                break;
            case UNKNOWN:
                sb.append(" ALGORITHM:").append(getMessage().getInt(ALGORITHM));
                break;
            default:
                sb.append(" ALGORITHM:").append(getAlgorithm());
                break;
        }

        if(getAlgorithm() != EncryptionAlgorithm.NO_ENCRYPTION)
        {
            sb.append(" KEY:").append(getKeyId());
            sb.append(" IV:").append(getIV());
            sb.append(" UNK:").append(getUnknown());
        }

        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Encryption algorithm.
     * @return algorithm or UNKNOWN if the value is not known.
     */
    public EncryptionAlgorithm getAlgorithm()
    {
        return EncryptionAlgorithm.fromValue(getMessage().getInt(ALGORITHM));
    }

    /**
     * Encryption key ID (1-255).
     * @return key ID
     */
    public int getKeyId()
    {
        return getMessage().getInt(KEY);
    }

    /**
     * Hex string representation of the initialization vector
     * @return IV in hex string.
     */
    public String getIV()
    {
        return getMessage().getHex(INITIALIZATION_VECTOR, 8);
    }

    /**
     * Unknown field.
     * @return hex string from the field contents.
     */
    public String getUnknown()
    {
        return getMessage().getHex(UNKNOWN, 4);
    }
}
