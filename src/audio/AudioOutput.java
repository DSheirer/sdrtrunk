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
package audio;

import java.nio.ByteBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import log.Log;
import sample.Listener;
import buffer.FloatSampleBufferAssembler;

public class AudioOutput implements Listener<Float>, 
									SquelchListener,
									AudioTypeListener
{
	private static final int sAUDIO_BLOCK_SIZE = 24000;
	
    private static final AudioFormat sAUDIO_FORMAT = 
			new AudioFormat( 48000,  //SampleRate
    						 16,     //Sample Size
    						 1,      //Channels
    						 true,   //Signed
    						 false ); //Little Endian
    
    private AudioAdapter mAdapter = new AudioAdapter( 48000 );
    
	private FloatSampleBufferAssembler mBuffer = 
			new FloatSampleBufferAssembler( sAUDIO_BLOCK_SIZE );
	private SourceDataLine mOutput;
	
	private boolean mEnabled = false;
	private SquelchState mSquelchState = SquelchState.SQUELCH;
	
	public AudioOutput( String channelName )
	{
		try
        {
	        mOutput = AudioSystem.getSourceDataLine( sAUDIO_FORMAT );
	        mOutput.open( sAUDIO_FORMAT, (int)( sAUDIO_FORMAT.getSampleRate() ) );
	        mOutput.start();
        }
        catch ( LineUnavailableException e )
        {
        	Log.warning( "AudioOutput - couldn't open audio speakers "
        			+ "for playback:" + e.getLocalizedMessage() );
        }
	}

	@Override
    public void setSquelch( SquelchState state )
    {
		mSquelchState = state;
    }
	
	public void setAudioPlaybackEnabled( boolean enabled )
	{
		mEnabled = enabled;
	}
	
	public void dispose()
	{
		mOutput.stop();
		mOutput.close();
		mOutput = null;
	}

	@Override
    public void receive( Float sample )
    {
		float outputSample = mAdapter.get( sample );

		if( mEnabled && mSquelchState == SquelchState.UNSQUELCH )
		{
			mBuffer.put( outputSample );
		}
		else
		{
			/* Use 0-valued samples when we're disabled or squelched */
			mBuffer.put( 0.0f );
		}
		
		if( !mBuffer.hasRemaining() )
		{
			ByteBuffer buffer = mBuffer.get();
			
			mOutput.write( buffer.array(), 0, buffer.array().length );
			
			mBuffer.reset();
		}
    }
	
	@Override
    public void setAudioType( AudioType type )
    {
		mAdapter.setAudioType( type );
    }
}
