/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
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
package dsp.fm;

import sample.Listener;
import sample.complex.Complex;
import sample.real.RealSampleBroadcaster;
import sample.real.RealSampleListener;
import dsp.filter.ComplexFIRFilterOld;
import dsp.filter.FilterFactory;
import dsp.filter.Window.WindowType;

@Deprecated
public class NBFMDemodulator implements Listener<Complex>
{
	private RealSampleBroadcaster mBroadcaster = new RealSampleBroadcaster();
	private ComplexFIRFilterOld mIQFilter;
	private FMDiscriminator mDiscriminator;

	/**
	 * Implements a quadrature narrow-band demodulator that produces float
	 * valued demodulated, audio filtered output samples.
	 */
	public NBFMDemodulator()
	{
		this( FilterFactory.getLowPass( 48000, 6250, 73, WindowType.HAMMING ), 
			  1.0002f, true );
	}

	public NBFMDemodulator( float[] iqFilter, float iqGain, boolean afc )
	{
		mIQFilter = new ComplexFIRFilterOld( iqFilter, iqGain );

		mDiscriminator = new FMDiscriminator( 1 );

		mIQFilter.setListener( mDiscriminator );

		mDiscriminator.setListener( mBroadcaster );
	}
	
	public void dispose()
	{
		mDiscriminator.dispose();
		
		mBroadcaster.dispose();
		
		mIQFilter.dispose();
	}
	
	/**
	 * Receive method for complex samples that are fed into this class to be
	 * processed
	 */
	@Override
    public void receive( Complex sample )
    {
		mIQFilter.receive( sample );
    }

	/**
	 * Adds a listener to receive demodulated samples
	 */
	public void addListener( RealSampleListener listener )
    {
		mBroadcaster.addListener( listener );
    }

	/**
	 * Removes a listener from receiving demodulated samples
	 */
    public void removeListener( RealSampleListener listener )
    {
		mBroadcaster.removeListener( listener );
    }
}
