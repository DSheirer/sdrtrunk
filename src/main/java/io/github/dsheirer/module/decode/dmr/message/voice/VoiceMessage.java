package io.github.dsheirer.module.decode.dmr.message.voice;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;

import java.util.ArrayList;
import java.util.List;

public class VoiceMessage extends DMRMessage {

    /**
     * DMR message frame.  This message is comprised of a 24-bit prefix and a 264-bit message frame.  Outbound base
     * station frames transmit a Common Announcement Channel (CACH) in the 24-bit prefix, whereas Mobile inbound frames
     * do not use the 24-bit prefix.
     *
     * @param syncPattern
     * @param message     containing 288-bit DMR message with preliminary bit corrections indicated.
     */
    public VoiceMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message) {
        super(syncPattern, message);
    }

    public List<byte[]> getAMBEFrames(List<byte[]> frames)
    {
        byte [] frame_1 = new byte[9];
        byte [] frame_2 = new byte[9];
        byte [] frame_3 = new byte[9];
        for(int i =0; i<9; i++) {
            frame_1[i] = mMessage.getByte(24 + i*8);
        }
        // copy first 4 byte
        for(int i =0; i<4; i++) {
            frame_2[i] = mMessage.getByte(96 + i*8);
        }
        // 4 bits and 4 bits
        frame_3[4] = (byte) ((mMessage.getByte(128) & 0xF0) | (mMessage.getByte(180) >> 4));
        // copy last 4 byte
        for(int i =0; i<4; i++) {
            frame_2[5+i] = mMessage.getByte(184 + i*8);
        }

        for(int i =0; i<9; i++) {
            frame_3[i] = mMessage.getByte(216 + i*8);
        }
        frames.add(frame_1);
        frames.add(frame_2);
        frames.add(frame_3);
        return frames;
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
        return new ArrayList<Identifier>();
    }
}
