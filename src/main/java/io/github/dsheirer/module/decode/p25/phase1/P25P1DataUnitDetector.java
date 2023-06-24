/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.dsp.psk.pll.IPhaseLockedLoop;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
import io.github.dsheirer.edac.BCH_63_16_11;
import io.github.dsheirer.sample.Listener;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P25P1DataUnitDetector implements Listener<Dibit>, ISyncDetectListener
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P1DataUnitDetector.class);
    private static final int DATA_UNIT_DIBIT_LENGTH = 57; //56 dibits plus 1 status symbol
    private static final int SYNC_DIBIT_LENGTH = 24;
    private static final int MAXIMUM_SYNC_MATCH_BIT_ERRORS = 9;
    private P25P1SyncDetector mSyncDetector;
    private NIDDelayBuffer mDataUnitBuffer = new NIDDelayBuffer();
    private DibitDelayBuffer mSyncDelayBuffer = new DibitDelayBuffer(DATA_UNIT_DIBIT_LENGTH - SYNC_DIBIT_LENGTH);
    private IP25P1DataUnitDetectListener mDataUnitDetectListener;
    private boolean mInitialSyncTestProcessed = false;
    private int mDibitsProcessed = 0;
    private BCH_63_16_11 mNIDDecoder = new BCH_63_16_11();
    private P25P1DataUnitID mPreviousDataUnitId = P25P1DataUnitID.TERMINATOR_DATA_UNIT;
    private int mNIDDetectionCount;

    public P25P1DataUnitDetector(IP25P1DataUnitDetectListener dataUnitDetectListener, IPhaseLockedLoop phaseLockedLoop)
    {
        mDataUnitDetectListener = dataUnitDetectListener;
        mSyncDetector = new P25P1SyncDetector(this, phaseLockedLoop);
    }

    /**
     * Sets the sample rate for the phase inversion sync detector
     */
    public void setSampleRate(double sampleRate)
    {
        mSyncDetector.setSampleRate(sampleRate);
    }

    public void reset()
    {
        mDibitsProcessed = 0;
        mInitialSyncTestProcessed = false;
    }

    @Override
    public void syncDetected(int bitErrors)
    {
        mInitialSyncTestProcessed = true;
        checkForNid(bitErrors, false);
    }

    @Override
    public void syncLost(int bitsProcessed)
    {
        dispatchSyncLoss(bitsProcessed);
    }

    private void dispatchSyncLoss(int bitsProcessed)
    {
        if(mDataUnitDetectListener != null)
        {
            mDataUnitDetectListener.syncLost(bitsProcessed);
        }
    }

    @Override
    public void receive(Dibit dibit)
    {
        mDibitsProcessed++;

        //Broadcast a sync loss every 4800 dibits/9600 bits ... or 1x per second for phase 1
        if(mDibitsProcessed > 4864)
        {
            dispatchSyncLoss(9600);
            mDibitsProcessed -= 4800;
        }

        mDataUnitBuffer.put(dibit);

        //Feed the sync detect with a 32 dibit delay from the data unit buffer so that if/when
        //a sync detect occurs, the data unit buffer is already filled with both the sync dibits
        //and the NID dibits and we can test for a valid NID
        mSyncDetector.receive(mSyncDelayBuffer.getAndPut(dibit));

        //If the sync detector doesn't fire and we've processed enough dibits for a sync/nid sequence
        //immediately following a valid message, then test for a NID anyway ... maybe the sync was corrupted
        if(!mInitialSyncTestProcessed && mDibitsProcessed == DATA_UNIT_DIBIT_LENGTH)
        {
            mInitialSyncTestProcessed = true;
            checkForNid(mSyncDetector.getPrimarySyncMatchErrorCount(), true);
        }
    }

    /**
     * Chects/tests the contents of the data unit buffer for a valid NID when a sync pattern is detected
     * or when commanded following a valid message sequence
     *
     * @param bitErrorCount when comparing the sync pattern to the received bit sequence
     * @param forcedCheck indicates if the NID check was forced, meaning that the primary sync detector
     * did not detect a sync, however we expect there to be a message sync following a successfully
     * decoded and framed preceeding messsage.  In these instances, the framer will allow for a less
     * than perfect match on the sync pattern, as long as the NID passes error check.
     */
    private void checkForNid(int bitErrorCount, boolean forcedCheck)
    {
        if(bitErrorCount <= MAXIMUM_SYNC_MATCH_BIT_ERRORS)
        {
            int[] nid = mSyncDelayBuffer.getNID();
            int[] correctedNid = new int[63];

            //If decoder indicates there are no unrecoverable errors ....
            if(!mNIDDecoder.decode(nid, correctedNid))
            {
                mNIDDetectionCount++;

                int nidBitErrorCount = getBitErrorCount(nid, correctedNid);

                if(mDataUnitDetectListener != null)
                {
                    mPreviousDataUnitId = getDataUnitID(correctedNid);

                    mDataUnitDetectListener.dataUnitDetected(mPreviousDataUnitId, getNAC(correctedNid),
                        (bitErrorCount + nidBitErrorCount), (mDibitsProcessed - DATA_UNIT_DIBIT_LENGTH), correctedNid);
                }
            }
            else if(mPreviousDataUnitId == P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_1)
            {
                //We have a good sync match, but the NID didn't pass error control and we're in the middle
                //of voice call, so treat this message as voice message, but set the previous duid to
                //terminator so we can end if there isn't a subsequent voice message
                mDataUnitDetectListener.dataUnitDetected(P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_2, -1,
                    (bitErrorCount + 64), (mDibitsProcessed - DATA_UNIT_DIBIT_LENGTH), new int[63]);

                mPreviousDataUnitId = P25P1DataUnitID.TERMINATOR_DATA_UNIT;
            }
            else if(mPreviousDataUnitId == P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_2)
            {
                //We have a good sync match, but the NID didn't pass error control and we're in the middle
                //of voice call, so treat this message as voice message, but set the previous duid to
                //terminator so we can end if there isn't a subsequent voice message
                mDataUnitDetectListener.dataUnitDetected(P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_1, -1,
                    (bitErrorCount + 64), (mDibitsProcessed - DATA_UNIT_DIBIT_LENGTH), new int[63]);

                mPreviousDataUnitId = P25P1DataUnitID.TERMINATOR_DATA_UNIT;
            }
        }
    }

    /**
     * Determines the data unit ID present in the nid value.
     * @param nid in reverse bit order
     * @return
     */
    public P25P1DataUnitID getDataUnitID(int[] nid)
    {
        int duid = 0;

        if(nid[47] == 1)
        {
            duid ^= 1;
        }

        if(nid[48] == 1)
        {
            duid ^= 2;
        }

        if(nid[49] == 1)
        {
            duid ^= 4;
        }

        if(nid[50] == 1)
        {
            duid ^= 8;
        }

        return P25P1DataUnitID.fromValue(duid);
    }

    /**
     * Determines the Network Access Code (NAC) present in the nid value.
     * @param nid in reverse bit order
     * @return nac
     */
    public int getNAC(int[] nid)
    {
        int nac = 0;

        if(nid[51] == 1)
        {
            nac ^= 1;
        }

        if(nid[52] == 1)
        {
            nac ^= 2;
        }

        if(nid[53] == 1)
        {
            nac ^= 4;
        }

        if(nid[54] == 1)
        {
            nac ^= 8;
        }

        if(nid[55] == 1)
        {
            nac ^= 16;
        }

        if(nid[56] == 1)
        {
            nac ^= 32;
        }

        if(nid[57] == 1)
        {
            nac ^= 64;
        }

        if(nid[58] == 1)
        {
            nac ^= 128;
        }

        if(nid[59] == 1)
        {
            nac ^= 256;
        }

        if(nid[60] == 1)
        {
            nac ^= 512;
        }

        if(nid[61] == 1)
        {
            nac ^= 1024;
        }

        if(nid[62] == 1)
        {
            nac ^= 2048;
        }

        return nac;
    }

    public int getNIDDetectionCount()
    {
        return mNIDDetectionCount;
    }

    public class NIDDelayBuffer extends DibitDelayBuffer
    {
        public NIDDelayBuffer()
        {
            super(DATA_UNIT_DIBIT_LENGTH);
        }

        public int[] getNID()
        {
            int[] nid = new int[64];

            int nidPointer = 0;
            int bufferPointer = mPointer;

            while(nidPointer < 64)
            {
                if(mBuffer[bufferPointer].getBit2())
                {
                    nid[nidPointer] = 1;
                }

                nidPointer++;

                if(mBuffer[bufferPointer].getBit1())
                {
                    nid[nidPointer] = 1;
                }

                nidPointer++;

                bufferPointer--;

                if(bufferPointer < 0)
                {
                    bufferPointer += mBuffer.length;
                }

                //Check for the status symbol
                if(nidPointer == 70)
                {
                    nidPointer += 2;
                }
            }

            return nid;
        }
    }


    /**
     * Circular buffer for storing and accessing dibits.
     */
    public class DibitDelayBuffer
    {
        protected Dibit[] mBuffer;
        protected int mPointer;

        /**
         * Constructs a dibit delay buffer of the specified length
         */
        public DibitDelayBuffer(int length)
        {
            mBuffer = new Dibit[length];

            //Preload the buffer to avoid null pointers
            for(int x = 0; x < length; x++)
            {
                mBuffer[x] = Dibit.D00_PLUS_1;
            }
        }

        /**
         * Fetches the NID in reverse order format required for the ECC code
         */
        public int[] getNID()
        {
            int[] nid = new int[63];

            int nidPointer = 0;
            int bufferPointer = mPointer - 1;

            if(bufferPointer < 0)
            {
                bufferPointer += mBuffer.length;
            }

            //Skip bit 2 of the starting (ie last) dibit ... we only want 63 of the 64 bits from the dibit buffer
            if(mBuffer[bufferPointer].getBit1())
            {
                nid[nidPointer] = 1;
            }

            nidPointer++;

            bufferPointer--;

            if(bufferPointer < 0)
            {
                bufferPointer += mBuffer.length;
            }

            while(nidPointer < 63)
            {
                if(mBuffer[bufferPointer].getBit2())
                {
                    nid[nidPointer] = 1;
                }

                nidPointer++;

                if(mBuffer[bufferPointer].getBit1())
                {
                    nid[nidPointer] = 1;
                }

                nidPointer++;

                bufferPointer--;

                //Check for and skip the status symbol that gets inserted after 70 bits, meaning
                //it will be located in the buffer after 22 NID bits
                if(nidPointer == 41)
                {
                    bufferPointer--;
                }

                if(bufferPointer < 0)
                {
                    bufferPointer += mBuffer.length;
                }
            }

            return nid;
        }

        public void log()
        {
            StringBuilder sb = new StringBuilder();

            int counter = 0;
            int pointer = mPointer;

            while(counter < mBuffer.length)
            {
                sb.append(mBuffer[pointer].getBit1() ? "1" : "0");
                sb.append(mBuffer[pointer++].getBit2() ? "1" : "0");

                if(pointer >= mBuffer.length)
                {
                    pointer = 0;
                }

                counter++;
            }

            if(this instanceof NIDDelayBuffer)
            {
                mLog.debug("NIDBUF: " + sb + " Length:" + mBuffer.length);
            }
            else
            {
                mLog.debug("SYNBUF: " + sb + " Length:" + mBuffer.length);
            }
        }

        /**
         * Returns an ordered buffer of the internal circular buffer contents.
         */
        public Dibit[] getBuffer()
        {
            Dibit[] transferBuffer = new Dibit[mBuffer.length];

            int transferBufferPointer = 0;
            int bufferPointer = mPointer;

            while(transferBufferPointer < transferBuffer.length)
            {
                transferBuffer[transferBufferPointer++] = mBuffer[bufferPointer++];

                if(bufferPointer >= mBuffer.length)
                {
                    bufferPointer = 0;
                }
            }

            return transferBuffer;
        }

        public int[] getBufferAsArray()
        {
            Dibit[] dibits = getBuffer();

            int[] bits = new int[dibits.length * 2];

            for(int x = 0; x < dibits.length; x++)
            {
                if(dibits[x].getBit1())
                {
                    bits[x * 2] = 1;
                }
                if(dibits[x].getBit2())
                {
                    bits[x * 2 + 1] = 1;
                }
            }

            return bits;
        }

        /**
         * Places the dibit into the internal circular buffer, overwriting the oldest dibit.
         */
        public void put(Dibit dibit)
        {
            //Note: this check is necessary where the previous runnable was interrupted and left the buffer
            //pointer pointing past the end of the array length.
            if(mPointer >= mBuffer.length)
            {
                mPointer = 0;
            }

            mBuffer[mPointer++] = dibit;

            if(mPointer >= mBuffer.length)
            {
                mPointer = 0;
            }
        }

        /**
         * Places the dibit into the internal circular buffer, overwriting and returning the
         * oldest dibit.
         */
        public Dibit getAndPut(Dibit dibit)
        {
            Dibit toReturn = mBuffer[mPointer];
            put(dibit);
            return toReturn;
        }
    }

    public static int[] reverse(int[] values)
    {
        int[] reversed = new int[values.length];

        for(int x = 0; x < values.length; x++)
        {
            reversed[values.length - x - 1] = values[x];
        }

        return reversed;
    }

    public static void logNID(int[] nid, boolean corrected)
    {
        StringBuilder sb = new StringBuilder();
        for(int value: nid)
        {
            sb.append(value);
        }

        if(corrected)
        {
            mLog.debug("C* NID: " + sb);
        }
        else
        {
            mLog.debug("   NID: " + sb);
        }
    }

    public static int getBitErrorCount(int[] a, int[] b)
    {
        Validate.isTrue(a.length == b.length, "Array lengths must be the same");

        int count = 0;

        for(int x = 0; x < a.length; x++)
        {
            if(a[x] != b[x])
            {
                count++;
            }
        }

        return count;
    }
}
