/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
 * ****************************************************************************
 */

package io.github.dsheirer.web.media;

import io.github.jaredmdobson.concentus.OpusApplication;
import io.github.jaredmdobson.concentus.OpusEncoder;
import io.github.jaredmdobson.concentus.OpusException;
import io.github.jaredmdobson.concentus.OpusSignal;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Encodes one call's audio (8 kHz mono float samples) as Opus packets of 20 milliseconds, for browsers to decode with
 * WebCodecs.  Uses Concentus, a pure Java port of libopus, so no native library is needed.
 *
 * Not thread safe: one call's audio is encoded on one thread.
 */
public class OpusStream
{
    public static final int SAMPLE_RATE = 8000;
    public static final int FRAME_MS = 20;
    public static final int FRAME_SAMPLES = SAMPLE_RATE * FRAME_MS / 1000;
    public static final int BITRATE = 16_000;
    private static final int MAX_PACKET_BYTES = 1275;

    private final OpusEncoder mEncoder;
    private final short[] mFrame = new short[FRAME_SAMPLES];
    private final byte[] mPacket = new byte[MAX_PACKET_BYTES];
    private int mFrameFill;

    /**
     * Constructs an instance
     * @throws IllegalStateException if the encoder can't be created
     */
    public OpusStream()
    {
        try
        {
            mEncoder = new OpusEncoder(SAMPLE_RATE, 1, OpusApplication.OPUS_APPLICATION_VOIP);
            mEncoder.setBitrate(BITRATE);
            mEncoder.setSignalType(OpusSignal.OPUS_SIGNAL_VOICE);
            mEncoder.setComplexity(5);
            mEncoder.setUseVBR(true);
        }
        catch(OpusException e)
        {
            throw new IllegalStateException("Unable to create the Opus encoder", e);
        }
    }

    /**
     * Adds samples and emits a packet for each complete 20 millisecond frame.
     * @param samples in the range -1.0 to 1.0 (values outside are clipped)
     * @param packets receives each encoded packet; the array belongs to the receiver
     */
    public void encode(float[] samples, Consumer<byte[]> packets)
    {
        for(float sample: samples)
        {
            float clipped = Math.max(-1.0f, Math.min(1.0f, sample));
            mFrame[mFrameFill++] = (short)(clipped * Short.MAX_VALUE);

            if(mFrameFill == FRAME_SAMPLES)
            {
                packets.accept(encodeFrame());
                mFrameFill = 0;
            }
        }
    }

    /**
     * Pads the last partial frame with silence and emits it.  Call at the end of the call.
     */
    public void flush(Consumer<byte[]> packets)
    {
        if(mFrameFill > 0)
        {
            Arrays.fill(mFrame, mFrameFill, FRAME_SAMPLES, (short)0);
            packets.accept(encodeFrame());
            mFrameFill = 0;
        }
    }

    private byte[] encodeFrame()
    {
        try
        {
            int length = mEncoder.encode(mFrame, 0, FRAME_SAMPLES, mPacket, 0, MAX_PACKET_BYTES);
            return Arrays.copyOf(mPacket, length);
        }
        catch(OpusException e)
        {
            throw new IllegalStateException("Opus encoding failed", e);
        }
    }
}
