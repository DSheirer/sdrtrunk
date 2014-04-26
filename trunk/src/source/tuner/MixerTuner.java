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

import java.util.ArrayList;

import javax.sound.sampled.TargetDataLine;

import sample.Listener;
import sample.complex.ComplexSampleListListener;
import source.mixer.MixerSource;
import source.mixer.SampleAdapter;

public abstract class MixerTuner extends Tuner
{
	private TargetDataLine mTargetDataLine;
	protected MixerTunerType mMixerTunerType;
	protected MixerSource mMixerSource;
	protected ArrayList<ComplexSampleListListener> mListeners = 
					new ArrayList<ComplexSampleListListener>();
	
	/**
	 * Provides a wrapper around a MixerSource to treat it like a Tuner that
	 * can provide ComplexSample<Short> samples.  Subclasses should couple this
	 * functionality with a tuner control class to allow tuning.
	 */
	public MixerTuner( String name,
					   MixerTunerType mixerTunerType,
					   TargetDataLine targetDataLine,
					   SampleAdapter sampleAdapter )
	{
		super( name );
		
		mMixerTunerType = mixerTunerType;

		mTargetDataLine = targetDataLine;
		
        mMixerSource = new MixerSource( targetDataLine, 
				   mMixerTunerType.getAudioFormat(),
				   name,
				   sampleAdapter );
	}
	
	public MixerTuner( String name, 
					   MixerTunerDataLine mixerTuner,
					   SampleAdapter sampleAdapter )
	{
		this( name, 
			  mixerTuner.getMixerTunerType(), 
			  mixerTuner.getTargetDataLine(),
			  sampleAdapter );
	}

	public MixerTunerType getMixerTunerType()
	{
		return mMixerTunerType;
	}
	
	public TargetDataLine getTargetDataLine()
	{
		return mTargetDataLine;
	}
	
	/**
	 * Overrides the Tuner listener management methods and allows the 
	 * complex mixer source to manage sample listeners
	 */
	@Override
    public void addListener( Listener<Float[]> listener )
    {
		mMixerSource.setListener( listener );
    }

	/**
	 * Overrides the Tuner listener management methods and allows the 
	 * complex mixer source to manage sample listeners
	 */
	@Override
    public void removeListener( Listener<Float[]> listener )
    {
	    mMixerSource.removeListener( listener );
    }
}
