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

import log.Log;
import sample.Listener;
import sample.complex.ComplexSample;
import sample.complex.ComplexSampleUtils;
import util.Oscillator;

public class NumericallyControlledOscillator implements Listener<ComplexSample>,
														FrequencyChangeListener
{
	private long mFrequency;
	private int mSampleRate;
	private Oscillator mSineWaveGenerator;
	private Listener<ComplexSample> mListener;
	/**
	 * Translates (shifts) frequency spectrum up or down, depending on the sign 
	 * of the frequency argument, by mixing received complex samples with a 
	 * generated sine wave of the frequency argument.  Positive frequency shifts 
	 * the received spectrum higher in frequency.  Negative frequency shifts the 
	 * received spectrum lower in frequency.  In both cases, spectrum that is
	 * shifted outside of the sampling bandwidth will wrap around, due to the 
	 * nature of quadrature sampling.
	 */
	public NumericallyControlledOscillator( long frequencyShift, int sampleRate )
	{
		mFrequency = frequencyShift;
		mSampleRate = sampleRate;
		
		int defaultMixingSignalGain = 10; 
		
		mSineWaveGenerator = new Oscillator( mFrequency, mSampleRate );
	}

	/**
	 * Listener method for this class to receive samples from a Complex Sample
	 * Provider.  This method multiplies the incoming sample by the generated
	 * mixing frequency to translate (ie shift +/-) the incoming signal, and 
	 * then send the translated signal on to all registered listeners
	 */
	@Override
    public void receive( ComplexSample sample )
    {
		if( mListener != null )
		{
			mListener.receive( ComplexSampleUtils.multiply( sample, 
					mSineWaveGenerator.nextComplex() ) );
		}
    }
	
	@Override
    public void frequencyChanged( long frequency, int bandwidth )
    {
		if( frequency != mFrequency )
		{
			setFrequency( frequency );
		}
		
		if( bandwidth != mSampleRate )
		{
			setSampleRate( bandwidth );
		}
    }

	/**
	 * Changes the frequency value and resets the sine wave generator, so that
	 * all subsequent samples will be output at the new translated frequency
	 * @param frequency - hertz
	 */
	public void setFrequency( long frequency )
	{
		Log.info( "NumericallyControlledOscillator - setting sine wave gen to freq:" + frequency );

		mFrequency = frequency;
		mSineWaveGenerator.setFrequency( frequency );
	}

	/**
	 * Current frequency shift setting
	 * @return - frequency - hertz
	 */
	public long getFrequency()
	{
		return mFrequency;
	}

	/**
	 * Changes the sample rate, so that all new input/output values will be 
	 * translated at the new sample rate
	 * @param sampleRate - hertz
	 */
	public void setSampleRate( int sampleRate )
	{
		mSampleRate = sampleRate;
		mSineWaveGenerator.setSampleRate( sampleRate );
	}

	/**
	 * Add listener to receive frequency translated complex samples
	 */
    public void setListener( Listener<ComplexSample> listener )
    {
		mListener = listener;
    }

	/**
	 * Remove listener from receiving frequency translated complex samples.
	 */
    public void clearListener()
    {
		mListener = null;
    }
}
