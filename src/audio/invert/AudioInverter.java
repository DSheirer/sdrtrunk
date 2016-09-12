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
package audio.invert;

import audio.InversionFrequency;
import sample.real.RealSampleListener;
import dsp.filter.Filters;
import dsp.filter.FloatFIRFilter;
import dsp.mixer.Oscillator;

/**
 * Applies audio inversion (or un-inversion) to a broadcast of float audio samples
 * by multiplying each successive sample by either a 1 or a -1 value.
 */
public class AudioInverter implements RealSampleListener
{
	private Oscillator mSineWaveGenerator;
	private FloatFIRFilter mPostInversionLowPassFilter;
	private RealSampleListener mListener;
	
	public AudioInverter( int inversionFrequency, int sampleRate )
	{
		mSineWaveGenerator = new Oscillator( inversionFrequency, sampleRate );
		
		mPostInversionLowPassFilter = new FloatFIRFilter( 
				Filters.FIRLP_55TAP_48000FS_3000FC.getCoefficients(), 1.04f );

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
			mPostInversionLowPassFilter.receive( 
					sample * mSineWaveGenerator.getFloat() );
			
			mSineWaveGenerator.rotate();
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
