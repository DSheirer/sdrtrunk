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
package io.github.dsheirer.module.demodulate.am;

import io.github.dsheirer.dsp.am.AMDemodulator_CB;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.Window.WindowType;
import io.github.dsheirer.dsp.filter.fir.complex.ComplexFIRFilter_CB_CB;
import io.github.dsheirer.dsp.filter.fir.real.RealFIRFilter_RB_RB;
import io.github.dsheirer.dsp.gain.AutomaticGainControl_RB;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexBuffer;
import io.github.dsheirer.sample.complex.IComplexBufferListener;
import io.github.dsheirer.sample.real.IFilteredRealBufferProvider;
import io.github.dsheirer.sample.real.RealBuffer;

public class AMDemodulatorModule extends Module
    implements IComplexBufferListener, IFilteredRealBufferProvider
{
    private static final int SAMPLE_RATE = 48000;

    private ComplexFIRFilter_CB_CB mIQFilter;
    private AMDemodulator_CB mDemodulator;
    private RealFIRFilter_RB_RB mLowPassFilter;
    private AutomaticGainControl_RB mAGC;

    /**
     * AM Demodulator Module
     */
    public AMDemodulatorModule()
    {
        mIQFilter = new ComplexFIRFilter_CB_CB(FilterFactory.getLowPass(
            SAMPLE_RATE, 5000, 73, WindowType.HAMMING), 1.0f);

        mDemodulator = new AMDemodulator_CB(500.0f);
        mIQFilter.setListener(mDemodulator);

        mLowPassFilter = new RealFIRFilter_RB_RB(
            FilterFactory.getLowPass(48000, 3000, 31, WindowType.COSINE), 1.0f);
        mDemodulator.setListener(mLowPassFilter);

        mAGC = new AutomaticGainControl_RB();
        mLowPassFilter.setListener(mAGC);
    }

    @Override
    public Listener<ComplexBuffer> getComplexBufferListener()
    {
        return mIQFilter;
    }

    @Override
    public void dispose()
    {
        mIQFilter.dispose();
        mIQFilter = null;

        mDemodulator.dispose();
        mDemodulator = null;

        mLowPassFilter.dispose();
        mLowPassFilter = null;

        mAGC.dispose();
        mAGC = null;
    }

    @Override
    public void reset()
    {
    }

    @Override
    public void setFilteredRealBufferListener(Listener<RealBuffer> listener)
    {
        mAGC.setListener(listener);
    }

    @Override
    public void removeFilteredRealBufferListener()
    {
        mAGC.removeListener();
    }

    @Override
    public void start()
    {
    }

    @Override
    public void stop()
    {
    }
}
