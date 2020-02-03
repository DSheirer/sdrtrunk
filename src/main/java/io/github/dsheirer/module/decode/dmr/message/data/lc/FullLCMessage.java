package io.github.dsheirer.module.decode.dmr.message.data.lc;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.ReedSolomon_12_9;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.data.DataMessage;

public class FullLCMessage extends DataMessage {
    private static final int[] FLCO = new int[]{2, 3, 4, 5, 6, 7};
    private static final int[] FID = new int[]{8, 9, 10, 11, 12, 13, 14, 15};
    private CorrectedBinaryMessage _message;
    /**
     * DMR Data Message.
     *
     * @param syncPattern either BASE_STATION_DATA or MOBILE_STATION_DATA
     * @param message     containing 288-bit DMR message with preliminary bit corrections indicated.
     */
    public FullLCMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message) {
        super(syncPattern, message);
        _message = getMessageBody(message);
    }
    protected void printLC()
    {
        if(_message!=null) {
            int result = ReedSolomon_12_9.checkReedSolomon(_message, 0, 72, 0x96);
            int fid = _message.getInt(FID);
            int flco = _message.getInt(FLCO);
            if(fid == 16) { //Motorola Capc+
                System.out.print("Capacity+: ");
                if(flco == 4) {
                    System.out.print("Group Voice, TG = " + _message.getInt(24,48) +
                            ", fromID = " + _message.getInt(48,62) +
                            ", channelId = " + _message.getInt(16,24) + " >>> ");
                } else {
                    System.out.print("FLCO = "+flco+"\n");
                }
            } else {
                System.out.print("VH, CCK = "+result+
                        ", FLCO = " +flco+
                        ", FID = " + fid +
                        ">>> ");
            }

        }
    }
}
