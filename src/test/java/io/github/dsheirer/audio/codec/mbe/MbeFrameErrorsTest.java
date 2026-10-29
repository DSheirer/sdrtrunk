package io.github.dsheirer.audio.codec.mbe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MbeFrameErrorsTest
{
    private static void flip(byte[] frame, int bit)
    {
        frame[bit / 8] ^= (byte)(0x80 >>> (bit % 8));
    }

    @Test
    public void cleanFramesHaveNoErrors()
    {
        assertEquals(0, MbeFrameErrors.imbe(new byte[18]));
        assertEquals(0, MbeFrameErrors.ambe(new byte[9]));
    }

    @Test
    public void imbeCountsFlippedFirstBlockBits()
    {
        byte[] frame = new byte[18];
        flip(frame, 0);
        assertEquals(1, MbeFrameErrors.imbe(frame));
        flip(frame, 7);
        assertEquals(2, MbeFrameErrors.imbe(frame));
    }

    @Test
    public void imbeIgnoresBitsOutsideFirstBlock()
    {
        byte[] frame = new byte[18];
        flip(frame, 1);
        assertEquals(0, MbeFrameErrors.imbe(frame));
    }

    @Test
    public void ambeCountsFlippedFirstBlockBits()
    {
        byte[] frame = new byte[9];
        flip(frame, 0);
        assertEquals(1, MbeFrameErrors.ambe(frame));
        flip(frame, 4);
        flip(frame, 8);
        assertEquals(3, MbeFrameErrors.ambe(frame));
    }

    @Test
    public void ambeIgnoresBitsOutsideFirstBlock()
    {
        byte[] frame = new byte[9];
        flip(frame, 2);
        assertEquals(0, MbeFrameErrors.ambe(frame));
    }

    @Test
    public void malformedFramesAreSkipped()
    {
        assertEquals(-1, MbeFrameErrors.imbe(new byte[4]));
        assertEquals(-1, MbeFrameErrors.ambe(null));
    }
}
