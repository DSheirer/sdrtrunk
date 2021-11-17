/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.dsp.fm;

import io.github.dsheirer.dsp.squelch.PowerSquelch;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableBufferQueue;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.buffer.ReusableFloatBuffer;
import io.github.dsheirer.source.SourceEvent;

/**
 * FM Demodulator for demodulating complex samples and producing demodulated floating point samples.
 *
 * Implements listener of source events to process runtime squelch threshold change request events
 * which are forwarded to the power squelch control.
 */
public class SquelchingFMDemodulator extends FMDemodulator implements Listener<SourceEvent>
{
    private ReusableBufferQueue mReusableBufferQueue = new ReusableBufferQueue("SquelchingFMDemodulator");
    private PowerSquelch mPowerSquelch;
    private boolean mSquelchChanged = false;

    /**
     * Creates an FM demodulator instance with a default gain of 1.0.
     */
    public SquelchingFMDemodulator(double alpha, double threshold, int ramp)
    {
        mPowerSquelch = new PowerSquelch(alpha, threshold, ramp);
    }

    /**
     * Registers the listener to receive notifications of squelch change events from the power squelch.
     */
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        mPowerSquelch.setSourceEventListener(listener);
    }

    /**
     * Demodulates the complex baseband sample buffer and returns a demodulated reusable buffer with the user count
     * set to 1.  The complex baseband buffer's user count is decremented after demodulation.
     *
     * @param basebandSampleBuffer containing samples to demodulate
     * @return demodulated sample buffer.
     */
    @Override
    public ReusableFloatBuffer demodulate(ReusableComplexBuffer basebandSampleBuffer)
    {
        setSquelchChanged(false);

        ReusableFloatBuffer demodulatedBuffer = mReusableBufferQueue.getBuffer(basebandSampleBuffer.getSampleCount());

        float[] basebandSamples = basebandSampleBuffer.getSamples();
        float[] demodulatedSamples = demodulatedBuffer.getSamples();
        float i,q;

        for(int x = 0; x < basebandSamples.length; x += 2)
        {
            i = basebandSamples[x];
            q = basebandSamples[x + 1];

            mPowerSquelch.process(i, q);

            if(mPowerSquelch.isUnmuted() || mPowerSquelch.isDecay())
            {
                demodulatedSamples[x / 2] = demodulate(i, q);
            }
            else
            {
                demodulatedSamples[x / 2] = 0.0f;
            }

            if(mPowerSquelch.isSquelchChanged())
            {
                setSquelchChanged(true);
            }
        }

        basebandSampleBuffer.decrementUserCount();

        return demodulatedBuffer;
    }

    /**
     * Sets the threshold for squelch control
     * @param threshold (dB)
     */
    public void setSquelchThreshold(double threshold)
    {
        mPowerSquelch.setSquelchThreshold(threshold);
    }

    /**
     * Indicates if the squelch state has changed during the processing of buffer(s)
     */
    public boolean isSquelchChanged()
    {
        return mSquelchChanged;
    }

    /**
     * Sets or resets the squelch changed flag.
     */
    private void setSquelchChanged(boolean changed)
    {
        mSquelchChanged = changed;
    }

    /**
     * Indicates if the squelch state is currently muted
     */
    public boolean isMuted()
    {
        return mPowerSquelch.isMuted();
    }

    @Override
    public void receive(SourceEvent sourceEvent)
    {
        //Only forward squelch threshold change request events
        if(sourceEvent.getEvent() == SourceEvent.Event.REQUEST_CHANGE_SQUELCH_THRESHOLD)
        {
            mPowerSquelch.receive(sourceEvent);
        }
        else if(sourceEvent.getEvent() == SourceEvent.Event.REQUEST_CURRENT_SQUELCH_THRESHOLD)
        {
            mPowerSquelch.receive(sourceEvent);
        }
    }
}