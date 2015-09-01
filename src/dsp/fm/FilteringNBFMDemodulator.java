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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.complex.Complex;
import sample.real.RealSampleBroadcaster;
import sample.real.RealSampleListener;
import dsp.filter.ComplexFIRFilterOld;
import dsp.filter.FilterFactory;
import dsp.filter.FloatFIRFilter;
import dsp.filter.Window.WindowType;

@Deprecated
public class FilteringNBFMDemodulator implements Listener<Complex>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( FilteringNBFMDemodulator.class );

	private RealSampleBroadcaster mBroadcaster = new RealSampleBroadcaster();
	private ComplexFIRFilterOld mIQFilter;
	private FloatFIRFilter mAudioFilter;
	private FMDiscriminator mDiscriminator;

	public FilteringNBFMDemodulator( float[] iqFilter, float iqGain,
							float[] audioFilter, float audioGain )
	{
		mIQFilter = new ComplexFIRFilterOld( iqFilter, iqGain );

		mAudioFilter = new FloatFIRFilter( audioFilter, audioGain ); 
		
		mDiscriminator = new FMDiscriminator( 1 );

		mIQFilter.setListener( mDiscriminator );

		mDiscriminator.setListener( mAudioFilter );

		mAudioFilter.setListener( mBroadcaster );
	}
	
	/**
	 * Implements a quadrature narrow-band demodulator that produces float
	 * valued demodulated, audio filtered output samples.
	 */
	public FilteringNBFMDemodulator()
	{
		this( FilterFactory.getLowPass( 48000, 5000, 7000, 48, WindowType.HAMMING, true ), 
				  1.0002f,
			  FilterFactory.getLowPass( 48000, 3000, 6000, 48, WindowType.HAMMING, true ),
			  2 );
	}

	/**
	 * Receive method for complex samples that are fed into this class to be
	 * processed
	 */
	@Override
    public void receive( Complex quadratureSample )
    {
		mIQFilter.receive( quadratureSample );
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
