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
 * Mono Audio output implementation
 */
public class MonoAudioOutput extends AudioOutput
{
	private final static int BUFFER_SIZE = 48000;
	
	public MonoAudioOutput( ThreadPoolManager threadPoolManager, Mixer mixer )
	{
		super( threadPoolManager, 
			   mixer, 
			   MixerChannel.MONO, 
			   AudioFormats.PCM_SIGNED_48KHZ_16BITS_MONO, 
			   AudioFormats.MONO_SOURCE_DATALINE_INFO, 
			   BUFFER_SIZE  );
	}

	/**
	 * Converts the audio packet data into mono audio frames.
	 */
	protected ByteBuffer convert( AudioPacket packet )
	{
		if( packet.hasAudioBuffer() )
		{
			float[] samples = packet.getAudioBuffer().getSamples();
			
			/* Little-endian byte buffer */
			ByteBuffer buffer = 
				ByteBuffer.allocate( samples.length * 2 )
						.order( ByteOrder.LITTLE_ENDIAN );

			ShortBuffer shortBuffer = buffer.asShortBuffer();

			for( float sample: samples )
			{
				shortBuffer.put( (short)( sample * Short.MAX_VALUE ) );
			}
			
			return buffer;
		}
		
		return null;
	}
}
