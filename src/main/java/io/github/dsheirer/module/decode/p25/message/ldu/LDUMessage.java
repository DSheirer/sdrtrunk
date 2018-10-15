package io.github.dsheirer.module.decode.p25.message.ldu;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.message.P25Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class LDUMessage extends P25Message
{
    private final static Logger mLog = LoggerFactory.getLogger(LDUMessage.class);

    public static final int IMBE_FRAME_1 = 0;
    public static final int IMBE_FRAME_2 = 144;
    public static final int IMBE_FRAME_3 = 328;
    public static final int IMBE_FRAME_4 = 512;
    public static final int IMBE_FRAME_5 = 696;
    public static final int IMBE_FRAME_6 = 880;
    public static final int IMBE_FRAME_7 = 1064;
    public static final int IMBE_FRAME_8 = 1248;
    public static final int IMBE_FRAME_9 = 1424;

    public static final int[] LOW_SPEED_DATA = {1392, 1393, 1394, 1395, 1396, 1397,
            1398, 1399, 1408, 1409, 1410, 1411, 1412, 1413, 1414, 1415};

    public LDUMessage(CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(message, nac, timestamp);
    }

    /**
     * Low speed data field contents.
     */
    public String getLowSpeedData()
    {
        return getMessage().getHex(LOW_SPEED_DATA, 4);
    }

    public String getMessageStub()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(super.getMessageStub());
        sb.append(" VOICE LSD:");
        sb.append(getLowSpeedData());
        sb.append(" ");

        return sb.toString();
    }

    /**
     * Returns a 162 byte array containing 9 IMBE voice frames of 18-bytes
     * (144-bits) each.  Each frame is intact as transmitted and requires
     * deinterleaving, error correction, derandomizing, etc.
     */
    public List<byte[]> getIMBEFrames()
    {
        List<byte[]> frames = new ArrayList<byte[]>();

        frames.add(getMessage().get(IMBE_FRAME_1, IMBE_FRAME_1 + 144).toByteArray());
        frames.add(getMessage().get(IMBE_FRAME_2, IMBE_FRAME_2 + 144).toByteArray());
        frames.add(getMessage().get(IMBE_FRAME_3, IMBE_FRAME_3 + 144).toByteArray());
        frames.add(getMessage().get(IMBE_FRAME_4, IMBE_FRAME_4 + 144).toByteArray());
        frames.add(getMessage().get(IMBE_FRAME_5, IMBE_FRAME_5 + 144).toByteArray());
        frames.add(getMessage().get(IMBE_FRAME_6, IMBE_FRAME_6 + 144).toByteArray());
        frames.add(getMessage().get(IMBE_FRAME_7, IMBE_FRAME_7 + 144).toByteArray());
        frames.add(getMessage().get(IMBE_FRAME_8, IMBE_FRAME_8 + 144).toByteArray());
        frames.add(getMessage().get(IMBE_FRAME_9, IMBE_FRAME_9 + 144).toByteArray());

        return frames;
    }
}
