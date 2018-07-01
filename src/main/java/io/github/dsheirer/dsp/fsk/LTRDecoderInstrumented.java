/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.dsp.fsk;

public class LTRDecoderInstrumented extends LTRDecoder
{
    /**
     * Instrumented extension to LTRDecoder
     *
     * @param messageLength in symbols
     */
    public LTRDecoderInstrumented(int messageLength)
    {
        super(messageLength, new SampleBufferInstrumented(SAMPLES_PER_SYMBOL, COARSE_TIMING_GAIN),
            new ZeroCrossingErrorDetectorInstrumented(SAMPLES_PER_SYMBOL));
    }

    public SampleBufferInstrumented getSampleBuffer()
    {
        return (SampleBufferInstrumented)mSampleBuffer;
    }

    public ZeroCrossingErrorDetectorInstrumented getErrorDetector()
    {
        return (ZeroCrossingErrorDetectorInstrumented)mTimingErrorDetector;
    }
}
