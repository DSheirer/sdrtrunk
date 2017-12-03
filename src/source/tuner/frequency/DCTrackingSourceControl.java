/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package source.tuner.frequency;

import module.Module;
import sample.Listener;
import sample.real.RealBuffer;
import source.ISourceEventProvider;
import source.SourceEvent;

import java.util.concurrent.ScheduledExecutorService;

public class DCTrackingSourceControl extends Module
    implements Listener<RealBuffer>, ISourceEventProvider
{
    private static float COARSE_THRESHOLD = 0.1f;
    private static float LOOP_GAIN = 0.1f;
    private static float COARSE_GAIN = 1000.0f;
    private static float FINE_GAIN = 100.0f;
    private int mErrorCorrection = 0;
    private int mMaximumFrequencyCorrection = 0;
    private Listener<SourceEvent> mListener;

    /**
     * DC offset Automatic Frequency Control.  Monitors DC bias present in an FM
     * demodulated audio stream and issues automatic frequency corrections to
     * maintain the DC bias close to zero.
     *
     * @param maximum - maximum allowable (+/-) frequency correction value.
     */
    public DCTrackingSourceControl(int maximum)
    {
        mMaximumFrequencyCorrection = maximum;
    }

    public void dispose()
    {
        mListener = null;
    }

    @Override
    public void receive(RealBuffer buffer)
    {
        double sum = 0.0d;

        for(float sample : buffer.getSamples())
        {
            sum += sample;
        }

        float mean = (float)(sum / (double)buffer.getSamples().length);

        float adjustment = 0.0f;

        if(mean > COARSE_THRESHOLD || mean < -COARSE_THRESHOLD)
        {
            adjustment = mean * COARSE_GAIN;
        }
        else
        {
            adjustment = mean * FINE_GAIN;
        }

        setErrorCorrection(mErrorCorrection + (int)((float)adjustment * LOOP_GAIN));
    }

    private void setErrorCorrection(int correction)
    {
        if(mListener != null)
        {
            if(correction > mMaximumFrequencyCorrection)
            {
                correction = mMaximumFrequencyCorrection;
            }
            else if(correction < -mMaximumFrequencyCorrection)
            {
                correction = -mMaximumFrequencyCorrection;
            }

            if(correction != mErrorCorrection)
            {
                mListener.receive(SourceEvent.channelFrequencyCorrectionRequest(correction));
            }
        }
    }

    @Override
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        mListener = listener;
    }

    @Override
    public void removeSourceEventListener()
    {
        mListener = null;
    }

    @Override
    public void start()
    {
    }

    @Override
    public void stop()
    {
    }

    @Override
    public void reset()
    {
        setErrorCorrection(0);
    }
}
