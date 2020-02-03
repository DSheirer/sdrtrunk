package io.github.dsheirer.module.decode.dmr.message.data.csbk;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRCDMR;
import io.github.dsheirer.edac.CRCP25;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import io.github.dsheirer.module.decode.dmr.message.data.DataMessage;
import io.github.dsheirer.protocol.Protocol;

import java.util.List;

public class CSBKMessage extends DataMessage {

    private static final int[] LB = new int[]{0};
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
                "Hyteria (8)",
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
    public CSBKMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message)
    {
        super(syncPattern, message);
        message = getMessageBody(message);
        if(message == null) {
            return;
        }
        if(CRCDMR.correctCCITT80(message,0,80, 0xa5a5) !=2) {
            int featId = message.getInt(FEATURE_ID);
            int csbkoId = message.getInt(CSBKO);
            System.out.print(//"LB: " + message.getInt(LB) +
                    "[CSBK] CSBKO: "+String.format("%s", Integer.toBinaryString(csbkoId)+
                    ", FID: " + fids[fid_mapping[featId]]) + " ");
            if(featId == 6) { //Connect Plus
                if(csbkoId == 1) {
                    System.out.print("Trident MS (Motorola) - Connect+ Neighbors: ? ? ? ? ?  >>> ");
                }
            }

        } else {
            System.out.print("Invalid CSBK \n");

        }
    }
    @Override
    public String toString() {
        return null;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public List<Identifier> getIdentifiers() {
        return null;
    }
}
