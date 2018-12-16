/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.dsp.symbol;

import io.github.dsheirer.bits.IBinarySymbolProcessor;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IReusableByteBufferProvider;
import io.github.dsheirer.sample.buffer.ReusableByteBuffer;
import io.github.dsheirer.sample.buffer.ReusableByteBufferQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Assembles reusable byte buffers from an incoming stream of boolean values.
 */
public class BinaryToByteBufferAssembler implements IBinarySymbolProcessor, IReusableByteBufferProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(BinaryToByteBufferAssembler.class);

    private ReusableByteBufferQueue mBufferQueue = new ReusableByteBufferQueue("BinaryToByteBufferAssembler");
    private ReusableByteBuffer mCurrentBuffer;
    private int mBufferPointer;
    private int mBufferSize;
    private byte mCurrentByte;
    private int mBitCount;
    private Listener<ReusableByteBuffer> mBufferListener;

    /**
     * Constructs an assembler to produce reusable byte buffers of the specified size
     *
     * @param bufferSize
     */
    public BinaryToByteBufferAssembler(int bufferSize)
    {
        mBufferSize = bufferSize;
        getNextBuffer();
    }

    /**
     * Broadcasts the current buffer to the registered listener and creates a new buffer, resetting
     * the buffer pointer to zero so that new dibits can be loaded.
     */
    private void getNextBuffer()
    {
        if(mCurrentBuffer != null && mBufferListener != null)
        {
            mBufferListener.receive(mCurrentBuffer);
        }

        mCurrentBuffer = mBufferQueue.getBuffer(mBufferSize);
        mBufferPointer = 0;
    }

    @Override
    public void process(boolean symbol)
    {
        mCurrentByte <<= 1;

        if(symbol)
        {
            mCurrentByte++;
        }

        mBitCount++;

        if(mBitCount >= 8)
        {
            mCurrentBuffer.getBytes()[mBufferPointer++] = mCurrentByte;
            mCurrentByte = 0;
            mBitCount = 0;

            if(mBufferPointer >= mBufferSize)
            {
                getNextBuffer();
            }
        }

    }

    /**
     * Registers the listener to receive fully assembled byte buffers from this assembler.
     */
    @Override
    public void setBufferListener(Listener<ReusableByteBuffer> listener)
    {
        mBufferListener = listener;
    }

    /**
     * Removes the listener from receiving buffers from this assembler
     */
    @Override
    public void removeBufferListener(Listener<ReusableByteBuffer> listener)
    {
        mBufferListener = null;
    }

    @Override
    public boolean hasBufferListeners()
    {
        return mBufferListener != null;
    }
}
