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
package source.tuner;

import sample.Broadcaster;
import sample.Listener;
import sample.complex.ComplexSample;
import dsp.filter.ComplexFilter;
import dsp.filter.FilterFactory;
import dsp.filter.Window.WindowType;

@Deprecated
public class DigitalDropChannel implements Listener<ComplexSample>,
										   FrequencyChangeListener
{
	/**
	 * Implements a frequency translating, decimating FIR filter channelizer.  
	 * 
	 * Uses multiple cascaded half-band low pass filters that each decimate by
	 * two, to achieve an overall power-of-two decimation factor.
	 * 
	 * @param decimation - power of 2 decimation factor
	 * 
	 * @param frequencyShift  +/- frequency shift in hertz
	 */

	private static int sCHANNEL_RATE = 48000;
	private static int sCHANNEL_PASS_FREQUENCY = 12500;
	
	private NumericallyControlledOscillator mFrequencyTranslator;

	private ComplexFilter[] mFilters;
	
	private Broadcaster<ComplexSample> mComplexSampleBroadcaster = 
										new Broadcaster<ComplexSample>();
	
	private int mSampleRate;
	private int mFrequencyShift;
	
	public DigitalDropChannel( int sampleRate, int frequencyShift )
	{
		if( sampleRate % sCHANNEL_RATE != 0 )
		{
			throw new IllegalArgumentException( "Sample rate must be an "
					+ "integer multiple of 48000" );
		}
		
		mFrequencyShift = frequencyShift;
		
		setSampleRate( sampleRate );
	}
	
	private void setSampleRate( int sampleRate )
	{
		if( sampleRate != mSampleRate )
		{
			/* Setup or adjust the frequency translator */
			if( mFrequencyTranslator == null )
			{
				mFrequencyTranslator = 
					new NumericallyControlledOscillator( mFrequencyShift, 
														 sampleRate );
			}
			else
			{
				mFrequencyTranslator.setSampleRate( sampleRate );
			}
			
			/* Get new decimation filters */
			mFilters = FilterFactory
					.getDecimationFilters( sampleRate, 
												   sCHANNEL_RATE, 
												   sCHANNEL_PASS_FREQUENCY, 
												   48, //dB attenuation
												   WindowType.HAMMING );
			
			/* wire the first filter stage to the frequency translator */
//			mFrequencyTranslator.setListener( mFilters[ 0  ] );
			
			/* wire the remaining filters together */
			if( mFilters.length > 1 )
			{
				for( int x = 1; x < mFilters.length; x++ )
				{
					mFilters[ x - 1 ].setListener( mFilters[ x ] );
				}
			}

			/* Add the sample broadcaster as a listener to the final filter */
//			mFilters[ mFilters.length - 1 ].setListener( mComplexSampleBroadcaster );

			mSampleRate = sampleRate;
		}
	}

	@Override
    public void frequencyChanged( int frequency, int bandwidth )
    {
		mFrequencyTranslator.setFrequency( frequency );
		
		if( bandwidth != mSampleRate )
		{
			setSampleRate( bandwidth );
		}
    }

	public int getFrequencyOffset()
	{
		return mFrequencyTranslator.getFrequency();
	}

    public void addListener( Listener<ComplexSample> listener )
    {
		mComplexSampleBroadcaster.addListener( listener );
    }

    public void removeListener( Listener<ComplexSample> listener )
    {
		mComplexSampleBroadcaster.removeListener( listener );
    }
	
	public int getListenerCount()
	{
		return mComplexSampleBroadcaster.getListenerCount();
	}
	
	public void removeListeners()
	{
		mComplexSampleBroadcaster.clear();
	}

	@Override
    public void receive( ComplexSample sample )
    {
		mFrequencyTranslator.receive( sample );
    }
}
