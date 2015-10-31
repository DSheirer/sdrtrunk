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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.adapter.ISampleAdapter;
import sample.complex.ComplexBuffer;
import sample.complex.ComplexSampleListListener;
import source.mixer.ComplexMixer;

public abstract class MixerTuner extends Tuner implements Listener<ComplexBuffer>
{
	private final static Logger mLog = 
							LoggerFactory.getLogger( MixerTuner.class );

	protected MixerTunerType mMixerTunerType;
	protected ComplexMixer mComplexMixer;
	protected ArrayList<ComplexSampleListListener> mListeners = 
					new ArrayList<ComplexSampleListListener>();
	
	/**
	 * Wrapper class to add basic Tuner functionality to the ComplexMixer.  
	 * Subclasses should couple this functionality with a tuner controller class 
	 * to support tuning.
	 */
	public MixerTuner( String name,
					   MixerTunerType mixerTunerType,
					   TargetDataLine targetDataLine,
					   ISampleAdapter sampleAdapter )
	{
		super( name );
		
		mMixerTunerType = mixerTunerType;

        mComplexMixer = new ComplexMixer( targetDataLine, 
        								  mMixerTunerType.getAudioFormat(),
        								  name,
        								  sampleAdapter,
        								  (Listener<ComplexBuffer>)this );
	}
	
	public MixerTuner( String name, 
					   MixerTunerDataLine mixerTuner,
					   ISampleAdapter sampleAdapter )
	{
		this( name, 
			  mixerTuner.getMixerTunerType(), 
			  mixerTuner.getTargetDataLine(),
			  sampleAdapter );
	}

	public void dispose()
	{
		mComplexMixer.stop();
		
		super.dispose();
	}
	
	public MixerTunerType getMixerTunerType()
	{
		return mMixerTunerType;
	}
	
	public TargetDataLine getTargetDataLine()
	{
		return mComplexMixer.getTargetDataLine();
	}
	
	/**
	 * Dispatches the received complex buffers from the mComplexMixer to all
	 * listeners registered on the parent Tuner class
	 */
	@Override
	public void receive( ComplexBuffer buffer )
	{
		super.broadcast( buffer );
	}
	
	/**
	 * Overrides the parent Tuner class listener management method to allow
	 * starting the buffer processing thread
	 */
	@Override
    public void addListener( Listener<ComplexBuffer> listener )
    {
		super.addListener( listener );

		/* We can call start multiple times -- it ignores additional requests */
		mComplexMixer.start();
    }

	/**
	 * Overrides the parent Tuner class listener management method to allow
	 * stopping the buffer processing thread when there are no more listeners
	 */
	@Override
    public void removeListener( Listener<ComplexBuffer> listener )
    {
		super.removeListener( listener );
		
		if( mSampleListeners.isEmpty() )
		{
			mComplexMixer.stop();
		}
    }
}
