package audio.output;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.sound.sampled.Mixer;

import source.mixer.MixerChannel;
import audio.AudioFormats;
import audio.AudioPacket;
import controller.ThreadPoolManager;

/**
 * Stereo audio output implementation.  
 */
public class StereoAudioOutput extends AudioOutput
{
	private final static int BUFFER_SIZE = 96000;
	
	public StereoAudioOutput( ThreadPoolManager threadPoolManager, 
			Mixer mixer, MixerChannel channel )
	{
		super( threadPoolManager, 
			   mixer, 
			   channel, 
			   AudioFormats.PCM_SIGNED_48KHZ_16BITS_STEREO, 
			   AudioFormats.STEREO_SOURCE_DATALINE_INFO, 
			   BUFFER_SIZE  );
	}

	/**
	 * Converts the audio packet data into stereo audio frames with the mixer
	 * channel containing the audio and the other channel containing zero 
	 * valued (silent) samples.
	 */
	protected ByteBuffer convert( AudioPacket packet )
	{
		if( packet.hasAudioBuffer() )
		{
			float[] samples = packet.getAudioBuffer().getSamples();
			
			/* Little-endian byte buffer */
			ByteBuffer buffer = ByteBuffer.allocate( samples.length * 4 )
					.order( ByteOrder.LITTLE_ENDIAN );
			
			ShortBuffer shortBuffer = buffer.asShortBuffer();

			if( getMixerChannel() == MixerChannel.LEFT )
			{
				for( float sample: samples )
				{
					shortBuffer.put( (short)( sample * Short.MAX_VALUE ) );
					shortBuffer.put( (short)0 );
				}
			}
			else
			{
				for( float sample: samples )
				{
					shortBuffer.put( (short)0 );
					shortBuffer.put( (short)( sample * Short.MAX_VALUE ) );
				}
			}
			
			return buffer;
		}
		
		return null;
	}
}
