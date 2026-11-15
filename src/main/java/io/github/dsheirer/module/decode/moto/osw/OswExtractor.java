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

// Reference: OP25 rx_smartnet.cc (boatbod/op25)
package io.github.dsheirer.module.decode.moto.osw;

import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.moto.Bandplan;
import io.github.dsheirer.module.decode.moto.message.MotorolaTypeIIMessage;
import io.github.dsheirer.module.decode.moto.message.MotorolaTypeIIMessageFactory;
import io.github.dsheirer.module.decode.moto.message.OswQueue;
import io.github.dsheirer.module.decode.moto.message.OswQueue.OswEntry;
import io.github.dsheirer.sample.Listener;

import java.util.List;

/**
 * Extracts outbound signalling words (OSW) from 3,600 baud demodulated samples.
 *
 * The symbol timing is not recovered.  A framer runs on each sample phase of a symbol and each OSW is taken from
 * whichever phase decodes it first.  A single fixed phase drifts against the symbol clock of the transmitter and
 * loses the control channel for several seconds each time the sampling point crosses the symbol edges.
 */
public class OswExtractor
{
    private static final double BAUD_RATE = 3600.0;
    private static final int SYNC_WORD = 0xAC;
    private static final int SYNC_BITS = 8;
    private static final int PAYLOAD_BITS = 76;
    private static final int FRAME_BITS = PAYLOAD_BITS + SYNC_BITS;
    private static final int INFO_BITS = 38;
    private static final int DATA_BITS = 27;
    private static final int CRC_BITS = 10;
    private static final int INTERLEAVE_ROWS = 19;
    private static final int INTERLEAVE_COLS = 4;

    private static final int ID_XOR = 0xCC38;
    private static final int CMD_XOR = 0xCD5;
    private static final int CRC_INIT = 0x0393;
    private static final int CRC_POLY = 0x0225;

    private static final double AFC_ALPHA = 0.0005;

    private int mPhases;
    private int mFrameSamples;
    private Framer[] mFramers = new Framer[0];
    private float[] mSymbolWindow = new float[0];
    private int mSymbolWindowPointer;
    private float mSymbolSum;
    private double mAfc;
    private long mSampleIndex;
    private long mLastSlotSampleIndex;
    private int mConsecutiveMissedOsw;

    private final OswQueue mOswQueue;
    private final Bandplan mBandplan;

    public OswExtractor(Bandplan bandplan)
    {
        mBandplan = bandplan;
        mOswQueue = new OswQueue(bandplan);
    }

    /**
     * Sets the sample rate, which must be a whole number of samples per symbol.
     */
    public void setSampleRate(double sampleRate)
    {
        mPhases = Math.max(1, (int)Math.round(sampleRate / BAUD_RATE));
        mFrameSamples = FRAME_BITS * mPhases;
        mFramers = new Framer[mPhases];

        for(int x = 0; x < mPhases; x++)
        {
            mFramers[x] = new Framer();
        }

        mSymbolWindow = new float[mPhases];
        mSymbolWindowPointer = 0;
        mSymbolSum = 0;
        mAfc = 0;
        mSampleIndex = 0;
        mLastSlotSampleIndex = 0;
        mConsecutiveMissedOsw = 0;
    }

    public void process(float[] samples, Listener<IMessage> messageListener)
    {
        for(float sample : samples)
        {
            mAfc += AFC_ALPHA * (sample - mAfc);
            float corrected = (float)(sample - mAfc);

            //Sum of the most recent symbol period of samples, which is the symbol for the phase that ends here.
            mSymbolSum += corrected - mSymbolWindow[mSymbolWindowPointer];
            mSymbolWindow[mSymbolWindowPointer] = corrected;
            mSymbolWindowPointer++;

            if(mSymbolWindowPointer >= mPhases)
            {
                mSymbolWindowPointer = 0;

                //Recompute to stop rounding error building up in the running sum.
                float sum = 0;

                for(float value : mSymbolWindow)
                {
                    sum += value;
                }

                mSymbolSum = sum;
            }

            Framer framer = mFramers[(int)(mSampleIndex % mPhases)];

            if(framer.process(mSymbolSum >= 0 ? 1 : 0))
            {
                //Adjacent phases decode the same OSW within a few samples of each other.  Take the first.
                if(mSampleIndex - mLastSlotSampleIndex > mFrameSamples / 2)
                {
                    processFrame(framer.mPayloadBuffer, messageListener);
                }
            }
            else if(mSampleIndex - mLastSlotSampleIndex > mFrameSamples + mFrameSamples / 2)
            {
                //No phase decoded an OSW in this slot.
                mLastSlotSampleIndex += mFrameSamples;
                processMissedOsw(messageListener);
            }

            mSampleIndex++;
        }
    }

    /**
     * Frames the bits of one sample phase: sync word, payload, then the sync word of the following OSW.
     */
    private static class Framer
    {
        private int mSyncShiftReg;
        private int mPayloadBitCount;
        private int mTrailingSyncBitCount;
        private int mTrailingSyncShiftReg;
        private final int[] mPayloadBuffer = new int[PAYLOAD_BITS];
        private boolean mCollecting;
        private boolean mCollectingTrailingSync;

        /**
         * Processes the bit.
         * @return true when the payload buffer holds a complete frame that is followed by a sync word.
         */
        public boolean process(int bit)
        {
            if(mCollecting)
            {
                if(mCollectingTrailingSync)
                {
                    mTrailingSyncShiftReg = ((mTrailingSyncShiftReg << 1) | bit) & 0xFF;
                    mTrailingSyncBitCount++;

                    if(mTrailingSyncBitCount >= SYNC_BITS)
                    {
                        mCollectingTrailingSync = false;
                        mPayloadBitCount = 0;
                        mTrailingSyncBitCount = 0;

                        if(mTrailingSyncShiftReg == SYNC_WORD)
                        {
                            //The trailing sync is also the leading sync for the next OSW.
                            mTrailingSyncShiftReg = 0;
                            mSyncShiftReg = SYNC_WORD;
                            return true;
                        }

                        mCollecting = false;
                        mSyncShiftReg = mTrailingSyncShiftReg;
                        mTrailingSyncShiftReg = 0;
                    }
                }
                else
                {
                    mPayloadBuffer[mPayloadBitCount++] = bit;

                    if(mPayloadBitCount >= PAYLOAD_BITS)
                    {
                        mCollectingTrailingSync = true;
                        mTrailingSyncBitCount = 0;
                        mTrailingSyncShiftReg = 0;
                    }
                }
            }
            else
            {
                mSyncShiftReg = ((mSyncShiftReg << 1) | bit) & 0xFF;

                if(mSyncShiftReg == SYNC_WORD)
                {
                    mCollecting = true;
                    mPayloadBitCount = 0;
                }
            }

            return false;
        }
    }

    /**
     * Decodes the frame and queues the OSW when it passes the CRC.  A frame that fails is left for another phase.
     */
    private void processFrame(int[] payload, Listener<IMessage> messageListener)
    {
        int[] deinterleaved = deinterleave(payload);
        int[] info = new int[INFO_BITS];
        int[] parity = new int[INFO_BITS];

        for(int i = 0; i < INFO_BITS; i++)
        {
            info[i] = deinterleaved[2 * i];
            parity[i] = deinterleaved[2 * i + 1];
        }

        correctErrors(info, parity);

        if(computeCrc(info) != extractReceivedCrc(info))
        {
            return;
        }

        mLastSlotSampleIndex = mSampleIndex;
        mConsecutiveMissedOsw = 0;

        int address = 0;
        for(int i = 0; i < 16; i++)
        {
            address = (address << 1) | info[i];
        }
        address ^= ID_XOR;
        address &= 0xFFFF;

        boolean isGroup = (info[16] ^ 1) != 0;

        int command = 0;
        for(int i = 17; i < 27; i++)
        {
            command = (command << 1) | info[i];
        }
        command ^= CMD_XOR;
        command &= 0x3FF;

        OswEntry entry = new OswEntry(address, isGroup, command, System.currentTimeMillis(), mBandplan);
        mOswQueue.add(entry);
        processQueue(messageListener);
    }

    /**
     * Marks a missed OSW in the queue so that a multi-OSW message is not assembled across the gap.
     */
    private void processMissedOsw(Listener<IMessage> messageListener)
    {
        //Once the queue holds nothing but markers there is nothing left to flush.
        if(mConsecutiveMissedOsw < OswQueue.QUEUE_SIZE)
        {
            mConsecutiveMissedOsw++;
            mOswQueue.addReset();
            processQueue(messageListener);
        }
    }

    private void processQueue(Listener<IMessage> messageListener)
    {
        if(!mOswQueue.isFull())
        {
            return;
        }

        List<MotorolaTypeIIMessage> messages = MotorolaTypeIIMessageFactory.process(mOswQueue, mBandplan);

        for(MotorolaTypeIIMessage message : messages)
        {
            messageListener.receive(message);
        }
    }

    private int[] deinterleave(int[] payload)
    {
        int[] deinterleaved = new int[PAYLOAD_BITS];
        for(int i = 0; i < PAYLOAD_BITS; i++)
        {
            deinterleaved[i] = payload[(i % INTERLEAVE_COLS) * INTERLEAVE_ROWS + (i / INTERLEAVE_COLS)];
        }
        return deinterleaved;
    }

    private int correctErrors(int[] info, int[] parity)
    {
        int[] syndrome = new int[INFO_BITS];
        int corrected = 0;

        for(int i = 0; i < INFO_BITS; i++)
        {
            int expected = (i == 0) ? info[0] : (info[i] ^ info[i - 1]);
            syndrome[i] = expected ^ parity[i];
        }

        for(int i = 0; i < INFO_BITS - 1; i++)
        {
            if(syndrome[i] == 1 && syndrome[i + 1] == 1)
            {
                info[i] ^= 1;
                corrected++;
            }
        }

        return corrected;
    }

    static int computeCrc(int[] info)
    {
        // OP25 CRC algorithm - shift right with polynomial 0x0225
        int crcAccum = CRC_INIT;  // 0x0393
        int crcOp = 0x036E;

        for(int j = 0; j < DATA_BITS; j++)
        {
            if((crcOp & 0x01) != 0)
            {
                crcOp = (crcOp >> 1) ^ CRC_POLY;  // 0x0225
            }
            else
            {
                crcOp >>= 1;
            }

            if((info[j] & 0x01) != 0)
            {
                crcAccum ^= crcOp;
            }
        }

        return crcAccum;
    }

    private int extractReceivedCrc(int[] info)
    {
        // CRC bits are inverted in the frame
        int crcGiven = 0;
        for(int j = 0; j < CRC_BITS; j++)
        {
            crcGiven <<= 1;
            crcGiven += (~info[DATA_BITS + j]) & 0x01;
        }
        return crcGiven;
    }
}
