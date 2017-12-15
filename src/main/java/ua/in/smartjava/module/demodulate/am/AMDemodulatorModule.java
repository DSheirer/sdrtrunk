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
package ua.in.smartjava.module.demodulate.am;

import java.util.concurrent.ScheduledExecutorService;

import ua.in.smartjava.module.Module;
import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.sample.complex.ComplexBuffer;
import ua.in.smartjava.sample.complex.IComplexBufferListener;
import ua.in.smartjava.sample.real.IFilteredRealBufferProvider;
import ua.in.smartjava.sample.real.RealBuffer;
import ua.in.smartjava.dsp.am.AMDemodulator_CB;
import ua.in.smartjava.dsp.filter.FilterFactory;
import ua.in.smartjava.dsp.filter.Window.WindowType;
import ua.in.smartjava.dsp.filter.fir.complex.ComplexFIRFilter_CB_CB;
import ua.in.smartjava.dsp.filter.fir.real.RealFIRFilter_RB_RB;
import ua.in.smartjava.dsp.gain.AutomaticGainControl_RB;

public class AMDemodulatorModule extends Module 
					implements IComplexBufferListener, IFilteredRealBufferProvider
{
	private static final int SAMPLE_RATE = 48000;
	
	private ComplexFIRFilter_CB_CB mIQFilter;
	private AMDemodulator_CB mDemodulator;
	private RealFIRFilter_RB_RB mLowPassFilter;	
    private AutomaticGainControl_RB mAGC;

	/**
	 * FM Demodulator with integrated DC removal and automatic frequency
	 * correction control.
	 * 
	 * @param pass - pass frequency for IQ filtering prior to demodulation.  This
	 * frequency should be half of the signal bandwidth since the ua.in.smartjava.filter will
	 * be applied against each of the inphase and quadrature signals and the 
	 * combined pass bandwidth will be twice this value.
	 * 
	 * @param stop - stop frequency for IQ filtering prior to demodulation.
	 * 
	 * @param maxFrequencyCorrection - defines the maximum frequency +/- that 
	 * the correction ua.in.smartjava.controller can adjust from the center tuned frequency.
	 * Set this value to 0 for no frequency correction control.
	 */
	public AMDemodulatorModule()
	{
		mIQFilter = new ComplexFIRFilter_CB_CB( FilterFactory.getLowPass( 
			SAMPLE_RATE, 5000, 73, WindowType.HAMMING ), 1.0f );
		
		mDemodulator = new AMDemodulator_CB( 500.0f );
		mIQFilter.setListener( mDemodulator );

		mLowPassFilter = new RealFIRFilter_RB_RB( 
    		FilterFactory.getLowPass( 48000, 3000, 31, WindowType.COSINE ), 1.0f );
		mDemodulator.setListener( mLowPassFilter );
		
		mAGC = new AutomaticGainControl_RB();
		mLowPassFilter.setListener( mAGC );
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
	public void setFilteredRealBufferListener( Listener<RealBuffer> listener )
	{
		mAGC.setListener( listener );
	}

	@Override
	public void removeFilteredRealBufferListener()
	{
		mAGC.removeListener();
	}

	@Override
	public void start( ScheduledExecutorService executor )
	{
	}

	@Override
	public void stop()
	{
	}
}
