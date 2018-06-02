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
package io.github.dsheirer.module.demodulate.audio;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.Window.WindowType;
import io.github.dsheirer.dsp.filter.dc.DCRemovalFilter_RealBuffer;
import io.github.dsheirer.dsp.filter.fir.real.RealFIRFilter_RB_RB;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.real.IFilteredRealBufferProvider;
import io.github.dsheirer.sample.real.IUnFilteredRealBufferListener;
import io.github.dsheirer.sample.real.RealBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated //Remove once polyphase channelizer enhancement is complete
public class DemodulatedAudioFilterModule extends Module implements IUnFilteredRealBufferListener, IFilteredRealBufferProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(DemodulatedAudioFilterModule.class);

    private DCRemovalFilter_RealBuffer mDCFilter;
    private RealFIRFilter_RB_RB mBandPassFilter;

    /**
     * Filters demodulated audio and removes any DC component from the buffer
     *
     * @param pass - audio pass band pass frequency
     * @param stop - audio pass band stop frequency
     */
    public DemodulatedAudioFilterModule(int pass, int stop)
    {
        assert (stop > pass);

        mDCFilter = new DCRemovalFilter_RealBuffer();

        //TODO: change this to a band pass filter
        mBandPassFilter = new RealFIRFilter_RB_RB(FilterFactory.getLowPass(48000, pass, stop, 60,
            WindowType.HANN, true), 1.0f);

        mDCFilter.setListener(mBandPassFilter);
    }

    /**
     * Constructs a demodulated audio filter using the filter taps and gain provided
     */
    public DemodulatedAudioFilterModule(float[] taps, float gain)
    {
        mBandPassFilter = new RealFIRFilter_RB_RB(taps, gain);
    }

    @Override
    public void dispose()
    {
        mBandPassFilter.dispose();
        mBandPassFilter = null;

        if(mDCFilter != null)
        {
            mDCFilter.dispose();
            mDCFilter = null;
        }
    }

    @Override
    public void reset()
    {
    }

    @Override
    public void setFilteredRealBufferListener(Listener<RealBuffer> listener)
    {
        mBandPassFilter.setListener(listener);
    }

    @Override
    public void removeFilteredRealBufferListener()
    {
        mBandPassFilter.removeListener();
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
    public Listener<RealBuffer> getUnFilteredRealBufferListener()
    {
        if(mDCFilter != null)
        {
            return mDCFilter;
        }
        else
        {
            return mBandPassFilter;
        }
    }
}
