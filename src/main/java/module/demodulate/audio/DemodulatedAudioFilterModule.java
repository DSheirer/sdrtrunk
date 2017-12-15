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
package module.demodulate.audio;

import dsp.filter.FilterFactory;
import dsp.filter.Window.WindowType;
import dsp.filter.dc.DCRemovalFilter_RealBuffer;
import dsp.filter.fir.real.RealFIRFilter_RB_RB;
import module.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;
import sample.real.IFilteredRealBufferProvider;
import sample.real.IUnFilteredRealBufferListener;
import sample.real.RealBuffer;

import java.util.concurrent.ScheduledExecutorService;

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
            WindowType.HANNING, true), 1.0f);

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
    public void start(ScheduledExecutorService executor)
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
