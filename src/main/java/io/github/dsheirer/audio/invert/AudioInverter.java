/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
 * ****************************************************************************
 */
package io.github.dsheirer.audio.invert;

import io.github.dsheirer.dsp.filter.FloatFIRFilter;
import io.github.dsheirer.dsp.oscillator.IRealOscillator;
import io.github.dsheirer.dsp.oscillator.OscillatorFactory;
import io.github.dsheirer.sample.real.RealSampleListener;

/**
 * Applies audio inversion (or un-inversion) to a broadcast of float audio samples
 * by multiplying each successive sample by either a 1 or a -1 value.
 */
public class AudioInverter implements RealSampleListener
{
	private static final float[] LOW_PASS_FILTER = new float[] { -0.0052419787903715655f,
			-0.0003789909443307852f,  0.0012970137854134386f,  0.004210460544067679f,
			0.008221266221665309f,   0.012951576579356689f,   0.017794312853913318f,
			0.021959871931790928f,   0.024583800118549094f,   0.024878144443784132f,
			0.022296319647647538f,   0.016685495012933472f,   0.008390268267799136f,
			-0.001718264469387386f,  -0.012288389660616603f,  -0.02161633130497537f,
			-0.027882128567957774f, 	-0.02943377233328899f,	-0.025006516193566884f,
			-0.014040074880106284f,   0.0032217508351258103f,  0.025660031586092195f,
			0.05136009984280478f,    0.0778336106888181f,     0.10230926486919244f,
			0.12209961841343948f,    0.13496813439824185f,    0.13943117253534673f,
			0.13496813439824185f,    0.12209961841343948f,    0.10230926486919244f,
			0.0778336106888181f,     0.05136009984280478f,    0.025660031586092195f,
			0.0032217508351258103f, -0.014040074880106284f,  -0.025006516193566884f,
			-0.02943377233328899f,   -0.027882128567957774f,  -0.02161633130497537f,
			-0.012288389660616603f, 	-0.001718264469387386f,   0.008390268267799136f,
			0.016685495012933472f,   0.022296319647647538f,   0.024878144443784132f,
			0.024583800118549094f,   0.021959871931790928f,   0.017794312853913318f,
			0.012951576579356689f,   0.008221266221665309f,   0.004210460544067679f,
			0.0012970137854134386f, -0.0003789909443307852f, -0.0052419787903715655f };
	private IRealOscillator mSineWaveGenerator;
	private FloatFIRFilter mPostInversionLowPassFilter;
	private RealSampleListener mListener;
	
	public AudioInverter( int inversionFrequency, int sampleRate )
	{
		mSineWaveGenerator = OscillatorFactory.getRealOscillator( inversionFrequency, sampleRate );
		mPostInversionLowPassFilter = new FloatFIRFilter(LOW_PASS_FILTER, 1.04f );
		mPostInversionLowPassFilter.setListener( new FilteredSampleProcessor() );
	}
	
	public AudioInverter( InversionFrequency frequency, int sampleRate )
	{
		this( frequency.getFrequency(), sampleRate );
	}
	
    public void setListener( RealSampleListener listener )
    {
	    mListener = listener;
    }

	@Override
    public void receive( float sample )
    {
		//Multiply the sample by the folding frequency and then send it to
		//the low pass filter
		if( mPostInversionLowPassFilter != null )
		{
			mPostInversionLowPassFilter.receive(sample * mSineWaveGenerator.generate(1)[0] );
		}
    }
	
	/**
	 * Simple class to receive the output of the FIR low pass filter and then
	 * send it to the registered listener(s)
	 */
	class FilteredSampleProcessor implements RealSampleListener
	{
		@Override
        public void receive( float sample )
        {
			if( mListener != null )
			{
				mListener.receive( sample );
			}
        }
	}
}
