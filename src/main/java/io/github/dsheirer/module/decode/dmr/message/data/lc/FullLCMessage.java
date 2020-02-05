package io.github.dsheirer.module.decode.dmr.message.data.lc;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.ReedSolomon_12_9;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.data.DataMessage;

public class FullLCMessage extends DataMessage {
    private static final int[] FLCO = new int[]{2, 3, 4, 5, 6, 7};
    private static final int[] FID = new int[]{8, 9, 10, 11, 12, 13, 14, 15};
    /**
     * DMR Data Message.
     *
     * @param syncPattern either BASE_STATION_DATA or MOBILE_STATION_DATA
     * @param message     containing 288-bit DMR message with preliminary bit corrections indicated.
     */
    public FullLCMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message) {
        super(syncPattern, message);
        dataMessage = getMessageBody(message);
    }
    public static String getServiceOption(byte so)
    {
        int priority;
        StringBuilder sb=new StringBuilder(300);
        sb.append("Service Options : ");
        if ((so & 0x80) > 0) sb.append("EMG");           // Emergency
        else sb.append("NONEMG");
        if ((so & 0x40) > 0) sb.append("/E2EE");         // Privacy
        if ((so & 0x08) > 0) sb.append("/Broadcast");
        if ((so & 0x04) > 0) sb.append("/OVCM Call");
        priority = so & 0b11;
        if (priority==0) sb.append("/No priority");
        else sb.append("/Priority " + Integer.toString(priority));
        return sb.toString();
    }
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if(dataMessage!=null) {
            //TO BE CHECK
            int result = ReedSolomon_12_9.checkReedSolomon(dataMessage, 0, 72, 0x96);
            int fid = dataMessage.getInt(FID);
            int flco = dataMessage.getInt(FLCO);
            if(fid == 0) { //standard DMR
                if(flco == 0) {
                    sb.append("Group Voice "+ getServiceOption(dataMessage.getByte(16)) +
                            ", TG = " + dataMessage.getInt(24,48 - 1) +
                            ", ID = " + dataMessage.getInt(48,72 - 1) +
                            ", channelId = " + dataMessage.getInt(16,24 - 1) + " >>> ");
                } else if(flco == 3) {
                    sb.append("Unit Voice "+ getServiceOption(dataMessage.getByte(16)) +
                            ", TG = " + dataMessage.getInt(24,48 - 1) +
                            ", ID = " + dataMessage.getInt(48,72 - 1) +
                            ", channelId = " + dataMessage.getInt(16,24 - 1) + " >>> ");
                } else if(flco == 48) {
                    sb.append("Data PDU: UnParsed >>> ");
                }
            } else if(fid == 16) { //Motorola Capc+
                sb.append("Capacity+: ");
                if(flco == 4) {
                    sb.append("Group Voice, TG = " + dataMessage.getInt(24,48 - 1) +
                            ", fromID = " + dataMessage.getInt(48,72 - 1) +
                            ", channelId = " + dataMessage.getInt(16,24 - 1) + " >>> ");
                } else {
                    sb.append("FLCO = "+flco+"\n");
                }
            } else {
                sb.append("VH, CCK = " + result+
                        ", FLCO = " +flco+
                        ", FID = " + fid +
                        ">>> ");
            }

        }
        return sb.toString();
    }
}
