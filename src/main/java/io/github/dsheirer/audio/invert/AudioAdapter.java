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

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.FloatFIRFilter;
import io.github.dsheirer.dsp.oscillator.IRealOscillator;
import io.github.dsheirer.dsp.oscillator.OscillatorFactory;
import io.github.dsheirer.dsp.window.WindowType;

/**
 * Based on the AudioType applied, produces normal, muted, or audio invert
 * audio output.
 */
public class AudioAdapter
{
	private static int mGain = 5;
	private IRealOscillator mSineWaveGenerator;
	private FloatFIRFilter mPostInversionFilter;
	private FloatFIRFilter mAudioHighPassFilter;
	private AudioType mAudioType = AudioType.NORMAL;
	private int mSampleRate;
	
	public AudioAdapter( int sampleRate )
	{
		mSampleRate = sampleRate;
		mSineWaveGenerator = OscillatorFactory.getRealOscillator(3000, mSampleRate );
		mAudioHighPassFilter = new FloatFIRFilter( 
				FilterFactory.getHighPass( mSampleRate, 200, 1600, 48, 
						WindowType.HAMMING, true ), mGain );
		mPostInversionFilter = new FloatFIRFilter( 
				FilterFactory.getLowPass( mSampleRate, 3000, 5000, 48, 
						WindowType.HAMMING, true ), mGain );
	}
	
	public void setAudioType( AudioType type )
	{
		if( type != null && type != mAudioType )
		{
			mAudioType = type;

			int inversionFrequency = type.getAudioInversionFrequency();
			
			if( inversionFrequency != 0 )
			{
				mSineWaveGenerator.setFrequency( inversionFrequency );
			}
		}
	}
	
    public float get( float sample )
    {
    	float retVal;
    	
		switch( mAudioType )
		{
			case NORMAL:
				retVal = mAudioHighPassFilter.get( sample );
				break;
			case MUTE:
				retVal = 0.0f;
				break;
			case INV2632:
			case INV2718:
			case INV2868:
			case INV3023:
			case INV3107:
			case INV3196:
			case INV3333:
			case INV3339:
			case INV3496:
			case INV3729:
			case INV4096:
			default:
				retVal = (float)( mPostInversionFilter.get( 
						mAudioHighPassFilter.get( sample ) * 
						mSineWaveGenerator.generate(1)[0] ) );
				break;
		}
		
		return retVal;
    }
}
