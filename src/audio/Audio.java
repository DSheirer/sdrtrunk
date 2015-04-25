package audio;

import javax.sound.sampled.AudioFormat;

public class Audio
{
	public static final AudioFormat MONO_48K_16_LE = 
			new AudioFormat( 48000,  //SampleRate
    						 16,     //Sample Size
    						 1,      //Channels
    						 true,   //Signed
    						 false ); //Little Endian

	public static final AudioFormat STEREO_48K_16_LE = 
			new AudioFormat( 48000,  //SampleRate
    						 16,     //Sample Size
    						 2,      //Channels
    						 true,   //Signed
    						 false ); //Little Endian
}
