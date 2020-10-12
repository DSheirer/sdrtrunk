/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.dsp.symbol;

import io.github.dsheirer.bits.BitSetFullException;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Circular buffer for storing and accessing dibits.
 */
public class DibitDelayBuffer
{
    private final static Logger mLog = LoggerFactory.getLogger(DibitDelayBuffer.class);

    protected Dibit[] mBuffer;
    protected int mPointer;

    /**
     * Constructs a dibit delay buffer of the specified length
     */
    public DibitDelayBuffer(int length)
    {
        Validate.isTrue(length > 0, "Dibit buffer length must be greater than 0");
        mBuffer = new Dibit[length];
        reset();
    }

    /**
     * Resets the buffer and preloads the buffer with all zeros
     */
    public void reset()
    {
        for(int x = 0; x < mBuffer.length; x++)
        {
            mBuffer[x] = Dibit.D00_PLUS_1;
        }
    }

    /**
     * Length of this circular buffer
     */
    public int length()
    {
        return mBuffer.length;
    }

    /**
     * Returns the dibit at the specified index
     * @param index to get
     * @return dibit at the specified index
     */
    public Dibit get(int index)
    {
        if(index < 0 || index >= mBuffer.length)
        {
            throw new IllegalArgumentException("Index argument must be between 0 and buffer length [" + mBuffer.length + "]");
        }

        return mBuffer[index];
    }

    /**
     * Sets the dibit at the specified index
     * @param index of the dibit
     * @param dibit to load at the specified index
     */
    public void set(int index, Dibit dibit)
    {
        if(index < 0 || index >= mBuffer.length)
        {
            throw new IllegalArgumentException("Index argument must be between 0 and buffer length [" + mBuffer.length + "]");
        }

        mBuffer[index] = dibit;
    }

    /**
     * Returns an ordered view of the internal circular buffer contents.
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

    /**
     * Retrieves a view of the buffer from the starting index to the specified length.  Note: if the requested
     * length exceeds the length of this buffer, the contents will be unwrapped from the circular buffer and repeated.
     * @param start of the view where 0 is the first element.
     * @param length of the dibit view
     * @return an array of dibits
     */
    public Dibit[] getBuffer(int start, int length)
    {
        Dibit[] transferBuffer = new Dibit[length];

        int transferBufferPointer = 0;
        int bufferPointer = (mPointer + start) % mBuffer.length;

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

    /**
     * Extracts a corrected binary message from the dibit buffer
     *
     * @param start dibit index where 0 is the oldest dibit and the newest dibit is buffer length - 1
     * @param dibitLength number of dibits to include in the message, making the message length = dibitLength * 2
     * @return corrected binary message
     */
    public CorrectedBinaryMessage getMessage(int start, int dibitLength)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(dibitLength * 2);

        int dibitCount = 0;
        int bufferPointer = (mPointer + start) % mBuffer.length;

        try
        {
            while(dibitCount < dibitLength)
            {
                Dibit dibit = mBuffer[bufferPointer++];
                message.add(dibit.getBit1());
                message.add(dibit.getBit2());
                dibitCount++;

                if(bufferPointer >= mBuffer.length)
                {
                    bufferPointer = 0;
                }
            }
        }
        catch(BitSetFullException e)
        {
            //This shouldn't happen
            mLog.error("Bit set full ??", e);
        }

        return message;
    }

    /**
     * Utility method to view the buffer contents with the dibits decoded as an array of zeros and ones.
     */
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

    /**
     * Logs the contents of the buffer
     */
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
        mLog.debug("BUFFER: " + sb + " Length:" + mBuffer.length);
    }
}
