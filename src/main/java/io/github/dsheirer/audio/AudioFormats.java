package io.github.dsheirer.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.SourceDataLine;

public class AudioFormats
{
	public static final AudioFormat.Encoding IMBE_ENCODING = 
			new AudioFormat.Encoding( "IMBE" );

	public static final boolean LITTLE_ENDIAN = false;
	public static final boolean BIG_ENDIAN = true;
	
	public static final float IMBE_FRAME_RATE = 50;
	public static final float IMBE_SAMPLE_RATE = 50;
	public static final float PCM_8KHZ_RATE = 8000;
	public static final float PCM_48KHZ_RATE = 48000;
	
	public static final int IMBE_FRAME_SIZE_BYTES = 18;
	public static final int IMBE_SAMPLE_SIZE_BITS = 144;
	public static final int ONE_CHANNEL = 1;
	public static final int TWO_CHANNELS = 2;
	public static final int PCM_SAMPLE_SIZE_BITS = 16;
	public static final int PCM_FRAME_SIZE_BYTES_MONO = 2;
	public static final int PCM_FRAME_SIZE_BYTES_STEREO = 4;
	
	public static AudioFormat IMBE_AUDIO_FORMAT = 
				new AudioFormat( IMBE_ENCODING, 
								 IMBE_SAMPLE_RATE,
								 IMBE_SAMPLE_SIZE_BITS,
								 ONE_CHANNEL, 
								 IMBE_FRAME_SIZE_BYTES, 
								 IMBE_FRAME_RATE,
								 LITTLE_ENDIAN );
	
	public static AudioFormat PCM_SIGNED_8KHZ_16BITS_MONO =
						new AudioFormat( AudioFormat.Encoding.PCM_SIGNED, 
								 PCM_8KHZ_RATE,
								 PCM_SAMPLE_SIZE_BITS,
								 ONE_CHANNEL, 
								 PCM_FRAME_SIZE_BYTES_MONO, 
								 PCM_8KHZ_RATE,
								 LITTLE_ENDIAN );

	public static AudioFormat PCM_SIGNED_8KHZ_16BITS_STEREO =
			new AudioFormat( AudioFormat.Encoding.PCM_SIGNED,
					PCM_8KHZ_RATE,
					PCM_SAMPLE_SIZE_BITS,
					TWO_CHANNELS,
					PCM_FRAME_SIZE_BYTES_STEREO,
					PCM_8KHZ_RATE,
					LITTLE_ENDIAN );

	public static AudioFormat PCM_SIGNED_48KHZ_16BITS_MONO =
						new AudioFormat( AudioFormat.Encoding.PCM_SIGNED, 
								 PCM_48KHZ_RATE,
								 PCM_SAMPLE_SIZE_BITS,
								 ONE_CHANNEL, 
								 PCM_FRAME_SIZE_BYTES_MONO, 
								 PCM_48KHZ_RATE,
								 LITTLE_ENDIAN );
	
	public static AudioFormat PCM_SIGNED_48KHZ_16BITS_STEREO =
						new AudioFormat( AudioFormat.Encoding.PCM_SIGNED, 
								 PCM_48KHZ_RATE,
								 PCM_SAMPLE_SIZE_BITS,
								 TWO_CHANNELS, 
								 PCM_FRAME_SIZE_BYTES_STEREO, 
								 PCM_48KHZ_RATE,
								 LITTLE_ENDIAN );
	
	/**
	 * Source Data Line Info for a 48 kHz, 16-bits signed PCM one channel
	 */
	public static final Line.Info MONO_SOURCE_DATALINE_INFO = 
		new DataLine.Info( SourceDataLine.class, PCM_SIGNED_8KHZ_16BITS_MONO);
	
	/**
	 * Source Data Line Info for a 48 kHz, 16-bits signed PCM two channels
	 */
	public static final Line.Info STEREO_SOURCE_DATALINE_INFO = 
		new DataLine.Info( SourceDataLine.class, PCM_SIGNED_8KHZ_16BITS_STEREO);
}
