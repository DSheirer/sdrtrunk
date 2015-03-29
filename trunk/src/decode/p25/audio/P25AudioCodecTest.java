package decode.p25.audio;

import java.util.Iterator;
import java.util.ServiceLoader;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.spi.FormatConversionProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P25AudioCodecTest
{
	private final static Logger mLog = LoggerFactory.getLogger( P25AudioCodecTest.class );

	public static void main( String[] args )
	{
		mLog.debug( "Starting ..." );
		AudioFormat from = IMBEAudioFormat.IMBE_AUDIO_FORMAT;
		AudioFormat to = IMBEAudioFormat.PCM_SIGNED_8KHZ_16BITS;

		ServiceLoader<FormatConversionProvider> loader = ServiceLoader.load( FormatConversionProvider.class );
		
		Iterator<FormatConversionProvider> it = loader.iterator();
		
		while( it.hasNext() )
		{
			mLog.debug( "Provider:" + it.next().getClass() );
		}
		
		
		
//		try
//		{
//			TargetDataLine tdl = AudioSystem.getTargetDataLine( from );
//			
//			mLog.debug( "We got a tdl ..." );
//		} 
//		catch ( Exception e )
//		{
//			e.printStackTrace();
//		}
		
		mLog.debug( "Finished" );
	}
}
