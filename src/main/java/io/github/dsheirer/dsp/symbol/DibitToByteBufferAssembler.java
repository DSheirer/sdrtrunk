/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.dsp.symbol;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IByteBufferProvider;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Assembles reusable byte buffers from an incoming stream of Dibits.
 */
public class DibitToByteBufferAssembler implements Listener<Dibit>, IByteBufferProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(DibitToByteBufferAssembler.class);

    private ByteBuffer mCurrentBuffer;
    private int mBufferSize;
    private byte mCurrentByte;
    private int mDibitCount;
    private Listener<ByteBuffer> mBufferListener;

    /**
     * Constructs an assembler to produce reusable byte buffers of the specified size
     *
     * @param bufferSize
     */
    public DibitToByteBufferAssembler(int bufferSize)
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
            mCurrentBuffer.flip();
            mBufferListener.receive(mCurrentBuffer);
        }

        mCurrentBuffer = ByteBuffer.allocate(mBufferSize);
    }

    @Override
    public void receive(Dibit dibit)
    {
        mCurrentByte <<= 2;

        switch(dibit)
        {
            case D01_PLUS_3:
                mCurrentByte |= 0x01;
                break;
            case D00_PLUS_1:
                //no-op - value is already 00
                break;
            case D10_MINUS_1:
                mCurrentByte |= 0x02;
                break;
            case D11_MINUS_3:
                mCurrentByte |= 0x03;
                break;
        }

        mDibitCount++;

        if(mDibitCount >= 4)
        {
            mCurrentBuffer.put(mCurrentByte);
            mCurrentByte = 0x00;
            mDibitCount = 0;

            if(!mCurrentBuffer.hasRemaining())
            {
                getNextBuffer();
            }
        }
    }

    /**
     * Registers the listener to receive fully assembled byte buffers from this assembler.
     */
    @Override
    public void setBufferListener(Listener<ByteBuffer> listener)
    {
        mBufferListener = listener;
    }

    /**
     * Removes the listener from receiving buffers from this assembler
     */
    @Override
    public void removeBufferListener(Listener<ByteBuffer> listener)
    {
        mBufferListener = null;
    }

    @Override
    public boolean hasBufferListeners()
    {
        return mBufferListener != null;
    }
}
