/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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
package module.demodulate.fm;

import module.Module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.complex.ComplexBuffer;
import sample.complex.IComplexBufferListener;
import sample.real.IUnFilteredRealBufferProvider;
import sample.real.RealBuffer;
import dsp.filter.FilterFactory;
import dsp.filter.Window.WindowType;
import dsp.filter.fir.complex.ComplexFIRFilter_CB_CB;
import dsp.fm.FMDemodulator_CB;

public class FMDemodulatorModule extends Module 
		implements IComplexBufferListener, IUnFilteredRealBufferProvider
{
	private static final int SAMPLE_RATE = 48000;
	
	private ComplexFIRFilter_CB_CB mIQFilter;
	private FMDemodulator_CB mDemodulator;
	
	/**
	 * FM Demodulator with I/Q filter.  Demodulated output is unfiltered and
	 * may contain a DC component.
	 * 
	 * @param pass - pass frequency for IQ filtering prior to demodulation.  This
	 * frequency should be half of the signal bandwidth since the filter will
	 * be applied against each of the inphase and quadrature signals and the 
	 * combined pass bandwidth will be twice this value.
	 * 
	 * @param stop - stop frequency for IQ filtering prior to demodulation.
	 */
	public FMDemodulatorModule( int pass, int stop )
	{
		assert( stop > pass );

		mIQFilter = new ComplexFIRFilter_CB_CB( FilterFactory.getLowPass( 
				SAMPLE_RATE, pass, stop, 60, WindowType.HAMMING, true ), 1.0f );
		
		mDemodulator = new FMDemodulator_CB( 1.0f );
		mIQFilter.setListener( mDemodulator );
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
	}

	@Override
	public void reset()
	{
		mDemodulator.reset();
	}

	@Override
	public void setUnFilteredRealBufferListener( Listener<RealBuffer> listener )
	{
		mDemodulator.setListener( listener );
	}

	@Override
	public void removeUnFilteredRealBufferListener()
	{
		mDemodulator.removeListener();
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
