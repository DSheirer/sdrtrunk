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

package io.github.dsheirer.vector.calibrate.channelizer;

import io.github.dsheirer.dsp.filter.channelizer.ChannelDFT;
import io.github.dsheirer.dsp.filter.channelizer.ChannelDFTFactory;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import java.util.List;

/**
 * Polyphase channelizer DFT calibration
 */
public class ChannelDFTCalibration extends Calibration
{
    private static final int CHANNEL_COUNT = 96;
    private static final int[] CHANNELS = {5, 6, 40, 90};
    private final float[] mSamples;

    /**
     * Constructs an instance
     */
    public ChannelDFTCalibration()
    {
        super(CalibrationType.CHANNEL_DFT);
        mSamples = getFloatSamples(CHANNEL_COUNT * 2);
    }

    @Override protected List<Candidate> getCandidates()
    {
        return List.of(
            candidate(Implementation.SCALAR, () -> {
                ChannelDFT operation = ChannelDFTFactory.getChannelDFT(Implementation.SCALAR, CHANNEL_COUNT);
                return () -> {
                    //The DFT is processed in place
                    float[] samples = mSamples.clone();
                    operation.process(samples, CHANNELS);
                    return samples;
                };
            }),
            candidate(Implementation.VECTOR_SIMD_PREFERRED, () -> {
                ChannelDFT operation = ChannelDFTFactory.getChannelDFT(
                    Implementation.VECTOR_SIMD_PREFERRED, CHANNEL_COUNT);
                return () -> {
                    //The DFT is processed in place
                    float[] samples = mSamples.clone();
                    operation.process(samples, CHANNELS);
                    return samples;
                };
            }));
    }
}
