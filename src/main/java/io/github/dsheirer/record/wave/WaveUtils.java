/*
 * *********************************************************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2017 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 * *********************************************************************************************************************
 */
package io.github.dsheirer.record.wave;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class WaveUtils
{
    public static final String RIFF_ID = "RIFF";
    public static final String WAVE_ID = "WAVE";
    public static final int WAVE_HEADER_SIZE = 54; //Includes the 8 byte data chunk header

    public static final String FORMAT_CHUNK = "fmt ";
    public static final int FORMAT_CHUNK_SIZE = 16;
    public static final short FORMAT_UNCOMPRESSED_PCM = 1;

    public static final String DATA_CHUNK = "data";

    public static final byte[] RIFF_CHUNK = {(byte)0x52, (byte)0x49, (byte)0x46, (byte)0x46}; //RIFF
    public static final byte[] WAV_FORMAT = {(byte)0x57, (byte)0x41, (byte)0x56, (byte)0x45}; //WAVE
    public static final byte[] CHUNK_FORMAT = {(byte)0x66, (byte)0x6D, (byte)0x74, (byte)0x20}; //fmt space
    public static final byte[] CHUNK_DATA = {(byte)0x64, (byte)0x61, (byte)0x74, (byte)0x61}; //data
    public static final int PCM_FORMAT = 1;

    /**
     * Creates a wave file header with the RIFF chunk descriptor and the WAVE
     * format chunk 1 and chunk 2 header
     */
    public static ByteBuffer getWaveHeader(AudioFormat format)
    {
        ByteBuffer descriptor2 = ByteBuffer.allocate(36).order(ByteOrder.LITTLE_ENDIAN);
        descriptor2.put(RIFF_ID.getBytes());
        descriptor2.putInt(WAVE_HEADER_SIZE);
        descriptor2.put(WAVE_ID.getBytes());

        //Chunk #1 - format descriptor
        descriptor2.put(FORMAT_CHUNK.getBytes());
        descriptor2.putInt(FORMAT_CHUNK_SIZE);
        descriptor2.putShort(FORMAT_UNCOMPRESSED_PCM);
        descriptor2.putShort((short)format.getChannels());
        descriptor2.putInt((int)format.getSampleRate());

        int frameByteRate2 = format.getChannels() * format.getSampleSizeInBits() / 8;

        /* Byte Rate = sample rate * channels * bits per sample / 8 */
        int byteRate2 = (int)(format.getSampleRate() * frameByteRate2);
        descriptor2.putInt(byteRate2);

        /* Block Align */
        descriptor2.putShort((short)frameByteRate2);

        /* Bits per Sample */
        descriptor2.putShort((short)format.getSampleSizeInBits());
//Finished with the header here

        ByteBuffer descriptor = ByteBuffer.allocate(44);
        descriptor.order(ByteOrder.LITTLE_ENDIAN);

        /* Chunk ID = RIFF */
        descriptor.put((byte)0x52); //R
        descriptor.put((byte)0x49); //I
        descriptor.put((byte)0x46); //F
        descriptor.put((byte)0x46); //F

        /* Chunk Size = 36 bytes (initial empty size) */
        descriptor.put((byte)36);
        descriptor.put((byte)0);
        descriptor.put((byte)0);
        descriptor.put((byte)0);

        /* Format = WAVE */
        descriptor.put((byte)0x57); //W
        descriptor.put((byte)0x41); //A
        descriptor.put((byte)0x56); //V
        descriptor.put((byte)0x45); //E

        /* Sub Chunk 1 ID */
        descriptor.put((byte)0x66); //f
        descriptor.put((byte)0x6D); //m
        descriptor.put((byte)0x74); //t
        descriptor.put((byte)0x20); //space

        /* SubChunk1Size = 1 */
        descriptor.put((byte)0x10);
        descriptor.put((byte)0x0);
        descriptor.put((byte)0x0);
        descriptor.put((byte)0x0);

        /* Audio format = 1 (Uncompressed PCM) */
        descriptor.put((byte)0x1);
        descriptor.put((byte)0x0);

        /* Number of Channels */
        descriptor.asShortBuffer().put((short)format.getChannels());
        descriptor.position(descriptor.position() + 2);

        /* Sample Rate - assumes integral sample rate */
        descriptor.asIntBuffer().put((int)format.getSampleRate());
        descriptor.position(descriptor.position() + 4);

        int frameByteRate = format.getChannels() * format.getSampleSizeInBits() / 8;

        /* Byte Rate = sample rate * channels * bits per sample / 8 */
        int byteRate = (int)(format.getSampleRate() * frameByteRate);
        descriptor.asIntBuffer().put(byteRate);
        descriptor.position(descriptor.position() + 4);

        /* Block Align */
        descriptor.asShortBuffer().put((short)frameByteRate);
        descriptor.position(descriptor.position() + 2);

        /* Bits per Sample */
        descriptor.asShortBuffer().put((short)(format.getSampleSizeInBits()));
        descriptor.position(descriptor.position() + 2);

        /* Sub Chunk 2 ID */
        descriptor.put((byte)0x64); //d
        descriptor.put((byte)0x61); //a
        descriptor.put((byte)0x74); //t
        descriptor.put((byte)0x61); //a

        /* Sub Chunk 2 Size */
        descriptor.put((byte)0x0);
        descriptor.put((byte)0x0);
        descriptor.put((byte)0x0);
        descriptor.put((byte)0x0);

        return descriptor;
    }
}
