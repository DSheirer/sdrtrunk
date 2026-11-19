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

package io.github.dsheirer.module.decode.smartnet;

import java.util.function.Consumer;

/**
 * Finds Motorola Type II outbound signalling words in a 3600 baud bit stream.
 *
 * Each OSW is an 84-bit frame: 8-bit sync 0xAC followed by 76 interleaved bits (19 x 4).  The 76 bits are a rate 1/2
 * code - each data bit followed by the XOR of it and the previous data bit - carrying 27 data bits (16 address, group,
 * 10 command) and a 10-bit CRC.  A frame is accepted only when the next sync follows 84 bits later, as OP25 does.
 * Deinterleave, error correction, CRC and the address/command masks follow OP25's rx_smartnet (Graham Norbury, GPL).
 * Both polarities are searched.
 */
public class SmartNetOSWFramer
{
    public static final int SYNC = 0xAC;
    public static final int FRAME_LENGTH = 84;
    public static final int PAYLOAD_LENGTH = 76;
    public static final int DATA_LENGTH = 27;
    public static final int CRC_LENGTH = 10;
    static final int ID_XOR = 0x33C7;
    static final int CMD_XOR = 0x32A;

    private final Consumer<SmartNetOSW> mListener;
    private final boolean[] mHistory = new boolean[FRAME_LENGTH + 8];
    private int mHistoryIndex;
    private long mBitCount;
    private long mSuppressUntil;
    private long mLastValidBit;

    //Statistics
    private long mFrameCount, mValidCount, mInvalidCount;
    private boolean mInverted;

    public SmartNetOSWFramer(Consumer<SmartNetOSW> listener)
    {
        mListener = listener;
    }

    /**
     * Processes one demodulated bit.
     */
    public void process(boolean bit, long timestamp)
    {
        mHistory[mHistoryIndex] = bit;
        mHistoryIndex = (mHistoryIndex + 1) % mHistory.length;
        mBitCount++;

        if(mBitCount < mHistory.length || mBitCount < mSuppressUntil)
        {
            return;
        }

        //Oldest bit in history is at mHistoryIndex.  Leading sync = bits 0-7, payload 8-83, trailing sync 84-91.
        int leading = byteAt(0);
        int trailing = byteAt(FRAME_LENGTH);

        boolean normal = leading == SYNC && trailing == SYNC;
        boolean inverted = leading == (~SYNC & 0xFF) && trailing == (~SYNC & 0xFF);

        if(normal || inverted)
        {
            mFrameCount++;
            mInverted = inverted;
            int[] payload = new int[PAYLOAD_LENGTH];

            for(int x = 0; x < PAYLOAD_LENGTH; x++)
            {
                payload[x] = (bitAt(8 + x) ^ inverted) ? 1 : 0;
            }

            SmartNetOSW osw = decode(payload, timestamp);

            if(osw != null)
            {
                mValidCount++;
                //One frame after the last decoded OSW = nothing was lost in between
                osw.setFollowsPrevious(mLastValidBit > 0 && mBitCount - mLastValidBit == FRAME_LENGTH);
                mLastValidBit = mBitCount;
                mListener.accept(osw);
                mSuppressUntil = mBitCount + FRAME_LENGTH - 8; //trailing sync starts the next frame
            }
            else
            {
                mInvalidCount++;
            }
        }
    }

    private boolean bitAt(int offset)
    {
        return mHistory[(mHistoryIndex + offset) % mHistory.length];
    }

    private int byteAt(int offset)
    {
        int value = 0;

        for(int x = 0; x < 8; x++)
        {
            value = (value << 1) | (bitAt(offset + x) ? 1 : 0);
        }

        return value;
    }

    /**
     * Deinterleaves, corrects, checks the CRC and extracts the OSW from 76 payload bits (0/1 values).
     * @return the OSW or null when the CRC fails
     */
    static SmartNetOSW decode(int[] payload, long timestamp)
    {
        //Deinterleave: incoming {1,2,3,...76} -> {1,20,39,58,2,21,40,59,...}
        int[] raw = new int[PAYLOAD_LENGTH];

        for(int k = 0; k < PAYLOAD_LENGTH / 4; k++)
        {
            for(int l = 0; l < 4; l++)
            {
                raw[k * 4 + l] = payload[k + l * 19];
            }
        }

        //Error correction: parity bit = data bit XOR previous data bit
        int[] expected = new int[PAYLOAD_LENGTH];
        expected[0] = raw[0];
        expected[1] = raw[0];

        for(int k = 2; k < PAYLOAD_LENGTH; k += 2)
        {
            expected[k] = raw[k];
            expected[k + 1] = raw[k] ^ raw[k - 2];
        }

        int[] syndrome = new int[PAYLOAD_LENGTH];

        for(int k = 0; k < PAYLOAD_LENGTH; k++)
        {
            syndrome[k] = expected[k] ^ raw[k];
        }

        int[] ecc = new int[DATA_LENGTH + CRC_LENGTH];

        for(int k = 0; k < PAYLOAD_LENGTH / 2 - 1; k++)
        {
            ecc[k] = (syndrome[2 * k + 1] == 1 && syndrome[2 * k + 3] == 1) ? raw[2 * k] ^ 1 : raw[2 * k];
        }

        //CRC
        int accumulator = 0x0393;
        int operator = 0x036E;

        for(int j = 0; j < DATA_LENGTH; j++)
        {
            operator = (operator & 1) == 1 ? (operator >> 1) ^ 0x0225 : operator >> 1;

            if(ecc[j] == 1)
            {
                accumulator ^= operator;
            }
        }

        int given = 0;

        for(int j = 0; j < CRC_LENGTH; j++)
        {
            given = (given << 1) | (ecc[j + DATA_LENGTH] ^ 1);
        }

        if(given != accumulator)
        {
            return null;
        }

        int address = 0;

        for(int j = 0; j < 16; j++)
        {
            address = (address << 1) | ecc[j];
        }

        address ^= ~ID_XOR & 0xFFFF;
        boolean group = ecc[16] == 0;
        int command = 0;

        for(int j = 17; j < 27; j++)
        {
            command = (command << 1) | ecc[j];
        }

        command ^= ~CMD_XOR & 0x3FF;
        return new SmartNetOSW(address, group, command, timestamp);
    }

    /**
     * Builds the 84-bit on-air frame (sync + interleaved payload) for an OSW - the inverse of decode, for tests and
     * synthetic signals.
     */
    static int[] encode(int address, boolean group, int command)
    {
        int[] ecc = new int[DATA_LENGTH + CRC_LENGTH];
        int a = (address ^ (~ID_XOR & 0xFFFF)) & 0xFFFF;
        int c = (command ^ (~CMD_XOR & 0x3FF)) & 0x3FF;

        for(int j = 0; j < 16; j++)
        {
            ecc[j] = (a >> (15 - j)) & 1;
        }
        ecc[16] = group ? 0 : 1;
        for(int j = 0; j < 10; j++)
        {
            ecc[17 + j] = (c >> (9 - j)) & 1;
        }

        int accumulator = 0x0393;
        int operator = 0x036E;

        for(int j = 0; j < DATA_LENGTH; j++)
        {
            operator = (operator & 1) == 1 ? (operator >> 1) ^ 0x0225 : operator >> 1;
            if(ecc[j] == 1)
            {
                accumulator ^= operator;
            }
        }

        for(int j = 0; j < CRC_LENGTH; j++)
        {
            ecc[DATA_LENGTH + j] = ((accumulator >> (9 - j)) & 1) ^ 1;
        }

        int[] raw = new int[PAYLOAD_LENGTH];
        int previous = 0;

        for(int k = 0; k < PAYLOAD_LENGTH / 2; k++)
        {
            int data = k < ecc.length ? ecc[k] : 0;
            raw[2 * k] = data;
            raw[2 * k + 1] = data ^ previous;
            previous = data;
        }

        int[] frame = new int[FRAME_LENGTH];

        for(int x = 0; x < 8; x++)
        {
            frame[x] = (SYNC >> (7 - x)) & 1;
        }

        for(int k = 0; k < PAYLOAD_LENGTH / 4; k++)
        {
            for(int l = 0; l < 4; l++)
            {
                frame[8 + k + l * 19] = raw[k * 4 + l];
            }
        }

        return frame;
    }

    public long getFrameCount()
    {
        return mFrameCount;
    }

    /**
     * Number of bits processed.  One OSW is sent every FRAME_LENGTH bits, so this gives the number of OSWs the
     * control channel sent, including the ones that were never found.
     */
    public long getBitCount()
    {
        return mBitCount;
    }

    public long getValidCount()
    {
        return mValidCount;
    }

    public long getInvalidCount()
    {
        return mInvalidCount;
    }

    public boolean isInverted()
    {
        return mInverted;
    }
}
