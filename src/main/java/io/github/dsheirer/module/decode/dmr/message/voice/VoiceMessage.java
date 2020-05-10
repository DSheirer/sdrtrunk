package io.github.dsheirer.module.decode.dmr.message.voice;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.DMRBurst;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base DMR Voice Message
 */
public abstract class VoiceMessage extends DMRBurst
{
    /**
     * DMR message frame.  This message is comprised of a 24-bit prefix and a 264-bit message frame.  Outbound base
     * station frames transmit a Common Announcement Channel (CACH) in the 24-bit prefix, whereas Mobile inbound frames
     * do not use the 24-bit prefix.
     *
     * @param syncPattern
     * @param message containing 288-bit DMR message with preliminary bit corrections indicated.
     */
    public VoiceMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, long timestamp,
                        int timeslot)
    {
        super(syncPattern, message, cach, timestamp, timeslot);
    }

    /**
     * AMBE audio CODEC encoded frames.
     */
    public List<byte[]> getAMBEFrames()
    {
        List<byte[]> frames = new ArrayList<>();
        byte[] frame_1 = new byte[9];
        byte[] frame_2 = new byte[9];
        byte[] frame_3 = new byte[9];
        for(int i = 0; i < 9; i++)
        {
            frame_1[i] = getMessage().getByte(24 + i * 8);
        }
        // copy first 4 byte
        for(int i = 0; i < 4; i++)
        {
            frame_2[i] = getMessage().getByte(96 + i * 8);
        }
        // 4 bits and 4 bits
        frame_3[4] = (byte)((getMessage().getByte(128) & 0xF0) | (getMessage().getByte(180) >> 4));
        // copy last 4 byte
        for(int i = 0; i < 4; i++)
        {
            frame_2[5 + i] = getMessage().getByte(184 + i * 8);
        }

        for(int i = 0; i < 9; i++)
        {
            frame_3[i] = getMessage().getByte(216 + i * 8);
        }
        frames.add(frame_1);
        frames.add(frame_2);
        frames.add(frame_3);
        return frames;
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
