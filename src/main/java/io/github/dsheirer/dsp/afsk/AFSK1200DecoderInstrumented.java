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
package io.github.dsheirer.dsp.afsk;

import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;

public class AFSK1200DecoderInstrumented extends AFSK1200Decoder
{
    private Broadcaster<Boolean> mSymbolBroadcaster = new Broadcaster<>();

    public AFSK1200DecoderInstrumented(boolean invertOutput)
    {
        super(new AFSKSampleBufferInstrumented(SAMPLES_PER_SYMBOL, TIMING_ERROR_GAIN),
            new AFSKTimingErrorDetectorInstrumented(SAMPLES_PER_SYMBOL), Output.NORMAL);
    }

    public AFSKSampleBufferInstrumented getSampleBuffer()
    {
        return (AFSKSampleBufferInstrumented)mSampleBuffer;
    }

    public AFSKTimingErrorDetectorInstrumented getErrorDetector()
    {
        return (AFSKTimingErrorDetectorInstrumented)mTimingErrorDetector;
    }

    protected void dispatch(boolean symbol)
    {
        super.dispatch(symbol);
        mSymbolBroadcaster.broadcast(symbol);
    }

    public void addListener(Listener<Boolean> listener)
    {
        mSymbolBroadcaster.addListener(listener);
    }
}
