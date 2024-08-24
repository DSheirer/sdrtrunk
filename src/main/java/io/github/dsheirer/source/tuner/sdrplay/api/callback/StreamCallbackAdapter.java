/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.sdrplay.api.callback;

import io.github.dsheirer.source.tuner.sdrplay.api.util.Flag;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_StreamCallback_t;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_StreamCbParamsT;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * I/Q sample stream callback adapter.  Implements the native interface and transfers the native callback data to an
 * implementation of the Java IStreamListener interface.
 */
public class StreamCallbackAdapter implements sdrplay_api_StreamCallback_t.Function
{
    private static final Logger mLog = LoggerFactory.getLogger(StreamCallbackAdapter.class);
    private IStreamListener mStreamListener;
    private IStreamCallbackListener mStreamCallbackListener;

    /**
     * Constructs an instance of the callback implementation
     * @param streamListener to receive transferred I/Q samples and event details
     * @param listener to receive callback parameters
     */
    public StreamCallbackAdapter(IStreamListener streamListener, IStreamCallbackListener listener)
    {
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
    public void apply(MemorySegment iSamplesPointer, MemorySegment qSamplesPointer, MemorySegment parametersPointer,
                      int sampleCount, int reset, MemorySegment deviceContext)
    {
        if(mStreamListener != null || mStreamCallbackListener != null)
        {
            try(Arena confinedArena = Arena.ofConfined())
            {
                //Translate the callback parameters pointer to a memory segment and re-construct the parameters as a Java object
                MemorySegment memorySegment = parametersPointer.reinterpret(sdrplay_api_StreamCbParamsT.sizeof(), confinedArena, null);
                StreamCallbackParameters parameters = new StreamCallbackParameters(memorySegment);

                if(mStreamCallbackListener != null)
                {
                    mStreamCallbackListener.process(mStreamListener.getTunerSelect(), parameters, reset);
                }

                if(mStreamListener != null)
                {
                    //Allocate memory segments from I/Q pointers, transfer from native to JVM array, and send to listener
                    long arrayByteSize = ValueLayout.JAVA_SHORT.byteSize() * sampleCount;
                    MemorySegment iSamples = iSamplesPointer.reinterpret(arrayByteSize, confinedArena, null);
                    MemorySegment qSamples = qSamplesPointer.reinterpret(arrayByteSize, confinedArena, null);
//                    MemorySegment iSamples = MemorySegment.ofAddress(iSamplesPointer.address(), arrayByteSize, confinedArena.scope());
//                    MemorySegment qSamples = MemorySegment.ofAddress(qSamplesPointer.address(), arrayByteSize, confinedArena.scope());
                    short[] i = iSamples.toArray(ValueLayout.JAVA_SHORT);
                    short[] q = qSamples.toArray(ValueLayout.JAVA_SHORT);
                    mStreamListener.processStream(i, q, parameters, Flag.evaluate(reset));
                }
            }
        }
    }
}
