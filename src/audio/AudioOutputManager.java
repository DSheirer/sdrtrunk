package audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioOutputManager
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( AudioOutputManager.class );
	
	public AudioOutputManager()
	{
		
	}
	
	public static void main( String[] args )
	{
		Mixer.Info[] mi = AudioSystem.getMixerInfo();

		for (Mixer.Info info : mi) 
        {
			mLog.debug( "info: " + info );
            
            Mixer m = AudioSystem.getMixer(info);
            
            mLog.debug( "mixer " + m );
            
            Line.Info[] sl = m.getSourceLineInfo();
            
            for (Line.Info info2 : sl) 
            {
            	mLog.debug( "    info: " + info2 );
                
            	try
            	{
                    Line line = AudioSystem.getLine( info2 );
                    
                    if (line instanceof SourceDataLine) 
                    {
                        SourceDataLine source = (SourceDataLine) line;

                        DataLine.Info i = (DataLine.Info) source.getLineInfo();
                        
                        for (AudioFormat format : i.getFormats()) 
                        {
                        	mLog.debug( "    format: " + format );
                        }
                    }
            	}
            	catch( Exception e )
            	{
            		mLog.error( "errors", e );
            	}
            }
        }		
	}
}
