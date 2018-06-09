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

import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;

public class AFSK1200DecoderInstrumented extends AFSK1200Decoder
{
    private Broadcaster<Boolean> mSymbolBroadcaster = new Broadcaster<>();

    public AFSK1200DecoderInstrumented(int messageLength, SampleBuffer sampleBuffer, ZeroCrossingErrorDetector detector, boolean invertOutput)
    {
        super(messageLength, new SampleBufferInstrumented(SAMPLES_PER_SYMBOL, COARSE_TIMING_GAIN),
            new ZeroCrossingErrorDetectorInstrumented(SAMPLES_PER_SYMBOL), invertOutput);
    }

    public SampleBufferInstrumented getSampleBuffer()
    {
        return (SampleBufferInstrumented)mSampleBuffer;
    }

    public ZeroCrossingErrorDetectorInstrumented getErrorDetector()
    {
        return (ZeroCrossingErrorDetectorInstrumented)mTimingErrorDetector;
    }

    protected void dispatch(boolean symbol)
    {
        if(mMessageFramer != null)
        {
            mMessageFramer.receive(mNormalOutput ? symbol : !symbol);
        }

        mSymbolBroadcaster.broadcast(symbol);
    }

    public void addListener(Listener<Boolean> listener)
    {
        mSymbolBroadcaster.addListener(listener);
    }
}
