package io.github.dsheirer.module.decode.dmr.message.voice;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;

import java.util.List;

public class VoiceEMBMessage extends VoiceMessage{
    private static final int[] COLOR_CODE = new int[]{132,133,134,135}; //NO CACH
    private static final int[] PI = new int[]{136,137};
    private static final int[] LCSS = new int[]{138,139};
    /**
     * DMR message frame.  This message is comprised of a 24-bit prefix and a 264-bit message frame.  Outbound base
     * station frames transmit a Common Announcement Channel (CACH) in the 24-bit prefix, whereas Mobile inbound frames
     * do not use the 24-bit prefix.
     *
     * @param syncPattern
     * @param message     containing 288-bit DMR message with preliminary bit corrections indicated.
     */
    public VoiceEMBMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message) {
        super(syncPattern, message);
        System.out.print("EMB Frame, CC = " + message.getInt(COLOR_CODE) + "\n");
        // verify will be implemented
        //4 cc, 1 pi, 2 lcss, 9 embparity
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public List<Identifier> getIdentifiers() {
        return null;
    }
}
