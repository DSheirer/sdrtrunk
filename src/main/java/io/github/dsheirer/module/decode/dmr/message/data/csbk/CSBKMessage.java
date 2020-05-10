/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2020 Dennis Sheirer, Zhenyu Mao
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.dmr.message.data.csbk;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.DataMessage;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.type.Vendor;

/**
 * Control Signalling Block (CSBK) Message
 *
 * ETSI 102 361-1 7.2.0
 */
public abstract class CSBKMessage extends DataMessage
{
    private static final int LAST_BLOCK = 0;
    private static final int PROTECT_FLAG = 1;
    private static final int[] OPCODE = new int[]{2, 3, 4, 5, 6, 7};
    private static final int[] VENDOR = new int[]{8, 9, 10, 11, 12, 13, 14, 15};

    /**
     * Constructs an instance
     * @param syncPattern for the CSBK
     * @param message bits
     * @param cach for the DMR burst
     * @param slotType for this message
     * @param timestamp
     * @param timeslot
     */
    public CSBKMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType,
                       long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);

        //Set message valid flag according to the corrected bit count for the CRC protected message
        setValid(getMessage().getCorrectedBitCount() != 2);
    }

    public boolean isLastBlock()
    {
        return getMessage().get(LAST_BLOCK);
    }

    public boolean isEncrypted()
    {
        return getMessage().get(PROTECT_FLAG);
    }

    /**
     * Utility method to lookup the opcode from a CSBK message
     * @param message containing CSBK bits
     * @return opcode
     */
    public static Opcode getOpcode(BinaryMessage message)
    {
        return Opcode.fromValue(message.getInt(OPCODE), getVendor(message));
    }

    /**
     * Opcode for this CSBK message
     */
    public Opcode getOpcode()
    {
        return getOpcode(getMessage());
    }

    /**
     * Opcode numeric value
     */
    protected int getOpcodeValue()
    {
        return getMessage().getInt(OPCODE);
    }

    /**
     * Utility method to lookup the vendor from a CSBK message
     * @param message containing CSBK bits
     * @return vendor
     */
    public static Vendor getVendor(BinaryMessage message)
    {
        return Vendor.fromValue(message.getInt(VENDOR));
    }

    /**
     * Vendor for this message
     */
    public Vendor getVendor()
    {
        return getVendor(getMessage());
    }

    /**
     * Numerical value for the vendor
     */
    protected int getVendorID()
    {
        return getMessage().getInt(VENDOR);
    }

//    @Override
//    public String toString()
//    {
//        if(!isValid())
//        {
//            return "[CSBK] Invalid CSBK";
//        }
//        StringBuilder sb = new StringBuilder();
//
//        featId = dataMessage.getInt(FEATURE_ID);
//        int csbkoId = dataMessage.getInt(OPCODE);
//        sb.append("[CSBK] FID: " + fids[fid_mapping[featId]] + " >>> ");
//        if(featId == 6 && csbkoId == 1)
//        {
//            int[] nbArray = new int[5];
//            nbArray[0] = (dataMessage.getByte(18) >> 2);
//            nbArray[1] = (dataMessage.getByte(26) >> 2);
//            nbArray[2] = (dataMessage.getByte(34) >> 2);
//            nbArray[3] = (dataMessage.getByte(42) >> 2);
//            nbArray[4] = (dataMessage.getByte(50) >> 2);
//            sb.append(String.format("Neighbors: %d %d %d %d %d ",
//                nbArray[0], nbArray[1], nbArray[2], nbArray[3], nbArray[4]));
//        }
//        else if(featId == 6 && csbkoId == 3)
//        {
//            int lcn = dataMessage.getInt(64, 68 - 1);
//            sb.append("Channel Grant TS = " + (dataMessage.get(68) ? "1" : "2") + ", LCN = " + lcn +
//                ", TG = " + dataMessage.getByte(40) +
//                ", ID = " + dataMessage.getByte(16));
//            // lcn_temp_needtoberemoved = lcn;
//        }
//        else if(featId == 16 && csbkoId == 62)
//        {
//            int lcn = dataMessage.getInt(20, 24 - 1);
//            byte[] groupArr = new byte[6];
//            groupArr[0] = dataMessage.getByte(32);
//            groupArr[1] = dataMessage.getByte(40);
//            groupArr[2] = dataMessage.getByte(48);
//            groupArr[3] = dataMessage.getByte(56);
//            groupArr[4] = dataMessage.getByte(64);
//            groupArr[5] = dataMessage.getByte(72);
//            if(dataMessage.getInt(24, 30 - 1) == 0)
//            {
//                sb.append("RestCh = " + lcn + " ");
//            }
//            else
//            {
//                for(int i = 0; i < 6; i++)
//                {
//                    if(dataMessage.get(24 + i))
//                    {
//                        sb.append(", LCN " + (i + 1) + " -> TG " + groupArr[i] + "; ");
//                    }
//                }
//            }
//        }
//        else
//        {
//            sb.append("CSBKO: " + csbkoId + ", Not decoded. ");
//        }
//        return sb.toString();
//    }

}
