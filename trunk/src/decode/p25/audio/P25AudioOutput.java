package decode.p25.audio;

import jmbe.iface.AudioConversionLibrary;
import jmbe.iface.AudioConverter;
import message.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import audio.AudioFormats;
import audio.AudioPacket;
import audio.IAudioOutput;
import audio.MonoAudioOutput;
import audio.SquelchListener;
import audio.inverted.AudioType;
import audio.inverted.IAudioTypeListener;
import controller.ResourceManager;
import decode.p25.message.ldu.LDUMessage;
import decode.p25.message.tdu.TDUMessage;
import decode.p25.message.tdu.lc.TDULinkControlMessage;

/**
 * P25 Phase 1 IMBE Audio Output converter.  Processes 18-byte IMBE audio frames 
 * and produces PCM 48k MONO 16-bit sample output.  
 * 
 * Note: this class depends on the external JMBE library.
 */
public class P25AudioOutput implements IAudioOutput, Listener<Message>, 
		SquelchListener, IAudioTypeListener
{
	private final static Logger mLog = LoggerFactory.getLogger( P25AudioOutput.class );
	
	private static final String IMBE_CODEC = "IMBE";

	private boolean mCanConvertAudio = false;
	private boolean mEncryptedAudio = false;
	
	private AudioConverter mAudioConverter;
	
	private MonoAudioOutput mAudioOutput = new MonoAudioOutput();
	
	public P25AudioOutput( ResourceManager resourceManager )
	{
		loadConverter();
	}

	/**
	 * Primary inject point for p25 imbe audio frame messages.  Each LDU audio 
	 * message contains 9 imbe voice frames.  If the audio is not encrypted,
	 * we insert each audio frame asynchronously into the audio converter.
	 */
	public void receive( Message message )
	{
		if( mCanConvertAudio )
		{
			if( message instanceof LDUMessage )
			{
				LDUMessage ldu = (LDUMessage)message;
				
				/* If we detect a valid LDU message with the encryption flag
				 * set to true, we toggle the encrypted audio state until we 
				 * receive the first TDU or TDULC message, then reset it. */
				if( ldu.isValid() && ldu.isEncrypted() )
				{
					mEncryptedAudio = true;
				}

				if( !mEncryptedAudio && mAudioConverter != null )
				{
					for( byte[] frame: ((LDUMessage)message).getIMBEFrames() )
					{
						byte[] audio = mAudioConverter.convert( frame );
						
						mAudioOutput.receive( new AudioPacket( "P25 Audio", audio, 1 ) );
					}
				}
			}
			else if( message instanceof TDUMessage || message instanceof TDULinkControlMessage )
			{
				mEncryptedAudio = false;
			}
		}
	}

	/**
	 * Loads audio frame processing chain.  Constructs an imbe targetdataline
	 * to receive the raw imbe frames.  Adds an IMBE to 8k PCM format conversion
	 * stream wrapper.  Finally, adds an upsampling (8k to 48k) stream wrapper.
	 */
	private void loadConverter()
	{
		AudioConversionLibrary library = null;
		
		try
		{
			@SuppressWarnings( "rawtypes" )
			Class temp = Class.forName( "jmbe.JMBEAudioLibrary" );
			
			library = (AudioConversionLibrary)temp.newInstance();

			mAudioConverter = library.getAudioConverter( IMBE_CODEC, 
					AudioFormats.PCM_SIGNED_48KHZ_16BITS );
			
			if( mAudioConverter != null )
			{
				mCanConvertAudio = true;
				mLog.info( "JMBE audio conversion library successfully loaded"
						+ " - P25 audio will be available" );
			}
			else
			{
				mLog.info( "JMBE audio conversion library NOT FOUND" );
			}
		} 
		catch ( ClassNotFoundException e1 )
		{
			mLog.error( "Couldn't find/load JMBE audio conversion library" );
		}
		catch ( InstantiationException e1 )
		{
			mLog.error( "Couldn't instantiate JMBE audio conversion library class" );
		}
		catch ( IllegalAccessException e1 )
		{
			mLog.error( "Couldn't load JMBE audio conversion library due to "
					+ "security restrictions" );
		}
	}
	
	@Override
	public void setAudioPlaybackEnabled( boolean enabled )
	{
		mAudioOutput.setMuted( !enabled );
	}

	@Override
	public void dispose()
	{
		mAudioOutput.dispose();
	}

	@Override
	public void setSquelch( SquelchState state )
	{
		/* not implemented */
	}

	@Override
	public void setAudioType( AudioType type )
	{
		//Not implemented
	}
}