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

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRCDMR;
import io.github.dsheirer.edac.CRCP25;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import io.github.dsheirer.module.decode.dmr.message.data.DataMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.ShortLCMessage;
import io.github.dsheirer.protocol.Protocol;

import java.util.List;

public class CSBKMessage extends DataMessage {

    private static final int[] LB = new int[]{0};
    private static final int[] PF = new int[]{1};
    private static final int[] CSBKO = new int[]{2,3,4,5,6,7};
    private static final int[] FEATURE_ID = new int[]{8,9,10,11,12,13,14,15};
    static byte[] fid_mapping = {
        1, 0, 0, 0, 2, 3, 4, 5, 6, 0, 0, 0, 0, 0, 0, 0,
        7, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 11, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 12, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 13, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 14, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    static String [] fids = {
        "Unknown",
        "Standard",
        "Flyde Micro",
        "PROD-EL SPA",
        "Motorola Connect+",
        "RADIODATA GmbH",
        "PDT",
        "Motorola Capacity+",
        "EMC S.p.A (19)",
        "EMC S.p.A (28)",
        "Radio Activity Srl (51)",
        "Radio Activity Srl (60)",
        "Tait Electronics",
        "Hyteria (104)",
        "Vertex Standard"
    };
    /*
    CSBKO Description Alias
    000100 Unit to Unit Voice Service Request UU_V_Req
    000101 Unit to Unit Voice Service Answer Response UU_Ans_Rsp
    100110 Negative Acknowledgement Response NACK_Rsp
    111000 BS Outbound Activation BS_Dwn_Act
    111101 Preamble CSBK Pre_CSBK
     */
    private int featId;
    public CSBKMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(syncPattern, message, timestamp, timeslot);
        dataMessage = getMessageBody(message);
        if(dataMessage == null) {
            return;
        }
        setValid(CRCDMR.correctCCITT80(dataMessage,0,80, 0xa5a5) !=2);
    }
    public int getFeatId() {
        return featId;
    }
    @Override
    public String toString() {
        if(!isValid()){
            return "[CSBK] Invalid CSBK";
        }
        StringBuilder sb = new StringBuilder();

        featId = dataMessage.getInt(FEATURE_ID);
        int csbkoId = dataMessage.getInt(CSBKO);
        sb.append("[CSBK] FID: " + fids[fid_mapping[featId]] + " >>> ");
        if(featId == 6 && csbkoId == 1) {
            int [] nbArray = new int[5];
            nbArray[0] = (dataMessage.getByte(18) >> 2);
            nbArray[1] = (dataMessage.getByte(26) >> 2);
            nbArray[2] = (dataMessage.getByte(34) >> 2);
            nbArray[3] = (dataMessage.getByte(42) >> 2);
            nbArray[4] = (dataMessage.getByte(50) >> 2);
            sb.append(String.format("Neighbors: %d %d %d %d %d ",
                    nbArray[0], nbArray[1], nbArray[2], nbArray[3], nbArray[4]));
        } else if(featId == 6 && csbkoId == 3) {
            int lcn = dataMessage.getInt(64, 68 - 1);
            sb.append("Channel Grant TS = " + (dataMessage.get(68) ? "1" : "2") +", LCN = " + lcn +
                    ", TG = " + dataMessage.getByte(40) +
                    ", ID = " + dataMessage.getByte(16) );
            // lcn_temp_needtoberemoved = lcn;
        } else if(featId == 16 && csbkoId == 62) {
            int lcn = dataMessage.getInt(20, 24 - 1);
            byte [] groupArr = new byte[6];
            groupArr[0] = dataMessage.getByte(32);
            groupArr[1] = dataMessage.getByte(40);
            groupArr[2] = dataMessage.getByte(48);
            groupArr[3] = dataMessage.getByte(56);
            groupArr[4] = dataMessage.getByte(64);
            groupArr[5] = dataMessage.getByte(72);
            if(dataMessage.getInt(24, 30 - 1) == 0) {
                sb.append("RestCh = " + lcn + " ");
            } else {
                for(int i = 0; i < 6; i++) {
                    if(dataMessage.get(24 + i)) {
                        sb.append(", LCN " + (i + 1) + " -> TG " + groupArr[i] + "; ");
                    }
                }
            }
        } else {
            sb.append("CSBKO: " + csbkoId + ", Not decoded. " );
        }
        return sb.toString();
    }

}
