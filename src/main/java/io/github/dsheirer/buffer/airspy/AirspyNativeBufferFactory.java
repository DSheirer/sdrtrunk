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

package io.github.dsheirer.buffer.airspy;

import io.github.dsheirer.buffer.AbstractNativeBufferFactory;
import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Implements a factory for creating SignedByteNativeBuffer instances
 */
public class AirspyNativeBufferFactory extends AbstractNativeBufferFactory
{
    private boolean mSamplePacking = false;
    private short[] mResidualI = new short[AirspyBufferIterator.I_OVERLAP];
    private short[] mResidualQ = new short[AirspyBufferIterator.Q_OVERLAP];
    private IAirspySampleConverter mConverter;
    private Implementation mInterleavedIteratorImplementation;
    private Implementation mNonInterleavedIteratorImplementation;

    /**
     * Constructs an instance
     */
    public AirspyNativeBufferFactory()
    {
        updateConverter();

        mInterleavedIteratorImplementation = CalibrationManager.getInstance()
                .getImplementation(CalibrationType.AIRSPY_UNPACKED_INTERLEAVED_ITERATOR);
        mNonInterleavedIteratorImplementation = CalibrationManager.getInstance()
                .getImplementation(CalibrationType.AIRSPY_UNPACKED_ITERATOR);
    }

    /**
     * Sample packing places two 12-bit samples into 3 bytes when enabled or
     * places two 12-bit samples into 4 bytes when disabled.
     *
     * @param enabled
     */
    public void setSamplePacking(boolean enabled)
    {
        mSamplePacking = enabled;
        updateConverter();
    }

    /**
     * Creates an optimal sample converter instance.
     */
    private void updateConverter()
    {
        if(mSamplePacking)
        {
            if(!(mConverter instanceof ScalarPackedSampleConverter))
            {
                mConverter = new ScalarPackedSampleConverter();
            }
        }
        else
        {
            Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.AIRSPY_SAMPLE_CONVERTER);

            if(implementation == Implementation.VECTOR_SIMD_PREFERRED)
            {
                if(!(mConverter instanceof VectorUnpackedSampleConverter))
                {
                    mConverter = new VectorUnpackedSampleConverter();
                }
            }
            else
            {
                if(!(mConverter instanceof ScalarUnpackedSampleConverter))
                {
                    mConverter = new ScalarUnpackedSampleConverter();
                }
            }
        }
    }

    @Override
    public INativeBuffer getBuffer(ByteBuffer buffer, long timestamp)
    {
        short[] samples = mConverter.convert(buffer);

        INativeBuffer nativeBuffer = new AirspyNativeBuffer(samples,
                Arrays.copyOf(mResidualI, mResidualI.length),
                Arrays.copyOf(mResidualQ, mResidualQ.length), mConverter.getAverageDc(), timestamp,
                mInterleavedIteratorImplementation, mNonInterleavedIteratorImplementation, getSamplesPerMillisecond());

        extractResidual(samples);

        return nativeBuffer;
    }

    /**
     * Extracts the residual overlap samples needed for continuity in the Hilbert transform filter.
     * @param samples to extract residual from
     */
    private void extractResidual(short[] samples)
    {
        int offset = samples.length - (AirspyBufferIterator.I_OVERLAP * 2);

        for(int i = 0; i < AirspyBufferIterator.I_OVERLAP; i++)
        {
            mResidualI[i] = samples[offset + (2 * i)];
        }

        offset = samples.length - (AirspyBufferIterator.Q_OVERLAP * 2) + 1;

        for(int q = 0; q < AirspyBufferIterator.Q_OVERLAP; q++)
        {
            mResidualQ[q] = samples[offset + (2 * q)];
        }
    }
}
