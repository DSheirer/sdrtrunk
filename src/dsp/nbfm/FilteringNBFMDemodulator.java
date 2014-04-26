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
package dsp.nbfm;

import sample.Broadcaster;
import sample.Listener;
import sample.complex.ComplexSample;
import dsp.filter.ComplexFIRFilter;
import dsp.filter.FilterFactory;
import dsp.filter.FloatFIRFilter;
import dsp.filter.Window.WindowType;

public class FilteringNBFMDemodulator implements Listener<ComplexSample>
{
	private Broadcaster<Float> mBroadcaster = new Broadcaster<Float>();
	private ComplexFIRFilter mIQFilter;
	private FloatFIRFilter mAudioFilter;
	private FMDiscriminator mDiscriminator;

	public FilteringNBFMDemodulator( double[] iqFilter, double iqGain,
							double[] audioFilter, double audioGain )
	{
		mIQFilter = new ComplexFIRFilter( iqFilter, iqGain );

		mAudioFilter = new FloatFIRFilter( audioFilter, audioGain ); 
		
		/**
		 * Add the FM polar discriminator with a gain of 32768, to match
		 * the fact that we're producing demodulated float values
		 */
		mDiscriminator = new FMDiscriminator( 32768 );

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
		this( FilterFactory.getLowPass( 48000, 4000, 6000, 48, WindowType.HAMMING, true ), 
				  1.0002,
			  FilterFactory.getLowPass( 48000, 3000, 6000, 48, WindowType.HAMMING, true ),
			  2 );
	}

	/**
	 * Receive method for complex samples that are fed into this class to be
	 * processed
	 */
	@Override
    public void receive( ComplexSample quadratureSample )
    {
		mIQFilter.receive( quadratureSample );
    }

	/**
	 * Adds a listener to receive demodulated samples
	 */
	public void addListener( Listener<Float> listener )
    {
		mBroadcaster.addListener( listener );
    }

	/**
	 * Removes a listener from receiving demodulated samples
	 */
    public void removeListener( Listener<Float> listener )
    {
		mBroadcaster.removeListener( listener );
    }
}
