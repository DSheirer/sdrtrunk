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

package com.github.dsheirer.sdrplay.callback;

import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_StreamCallback_t;
import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_StreamCbParamsT;
import com.github.dsheirer.sdrplay.util.Flag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;

/**
 * I/Q sample stream callback adapter.  Implements the native interface and transfers the native callback data to an
 * implementation of the Java IStreamListener interface.
 */
public class StreamCallbackAdapter implements sdrplay_api_StreamCallback_t
{
    private static final Logger mLog = LoggerFactory.getLogger(StreamCallbackAdapter.class);
    private MemorySession mMemorySession;
    private IStreamListener mStreamListener;
    private IStreamCallbackListener mStreamCallbackListener;

    /**
     * Constructs an instance of the callback implementation
     * @param memorySession for defining new foreign memory segments
     * @param streamListener to receive transferred I/Q samples and event details
     * @param listener to receive callback parameters
     */
    public StreamCallbackAdapter(MemorySession memorySession, IStreamListener streamListener,
                                 IStreamCallbackListener listener)
    {
        if(memorySession == null)
        {
            throw new IllegalArgumentException("Resource scope must be non-null");
        }

        mMemorySession = memorySession;
        mStreamCallbackListener = listener;
        setListener(streamListener);
    }

    /**
     * Updates the listener for receiving samples
     * @param listener to receive stream samples
     */
    public void setListener(IStreamListener listener)
    {
        mStreamListener = listener;
    }

    /**
     * Receives callback of foreign memory data, transfers it to Java, and passes to the listener.
     * @param iSamplesPointer array foreign memory address
     * @param qSamplesPointer array foreign memory address
     * @param parametersPointer associated with the callback - foreign memory address
     * @param sampleCount number of samples in each of the I and Q arrays
     * @param reset 0 or 1, translated to a boolean
     * @param deviceContext of the device that sourced the samples
     */
    @Override
    public void apply(MemoryAddress iSamplesPointer, MemoryAddress qSamplesPointer, MemoryAddress parametersPointer,
                      int sampleCount, int reset, MemoryAddress deviceContext)
    {
        if(mStreamListener != null || mStreamCallbackListener != null)
        {
            //Translate the callback parameters pointer to a memory segment and re-construct the parameters as a Java object
            StreamCallbackParameters parameters = new StreamCallbackParameters(sdrplay_api_StreamCbParamsT
                    .ofAddress(parametersPointer, mMemorySession));

            if(mStreamCallbackListener != null)
            {
                mStreamCallbackListener.process(mStreamListener.getTunerSelect(), parameters, reset);
            }

            if(mStreamListener != null)
            {
                //Allocate memory segments from I/Q pointers, transfer from native to JVM array, and send to listener
                long arrayByteSize = ValueLayout.JAVA_SHORT.byteSize() * sampleCount;
                MemorySegment iSamples = MemorySegment.ofAddress(iSamplesPointer, arrayByteSize, mMemorySession);
                MemorySegment qSamples = MemorySegment.ofAddress(qSamplesPointer, arrayByteSize, mMemorySession);
                short[] i = iSamples.toArray(ValueLayout.JAVA_SHORT);
                short[] q = qSamples.toArray(ValueLayout.JAVA_SHORT);
                mStreamListener.processStream(i, q, parameters, Flag.evaluate(reset));
            }
        }
    }
}
