package module.decode.p25.audio;

import jmbe.iface.AudioConversionLibrary;
import jmbe.iface.AudioConverter;
import message.IMessageListener;
import message.Message;
import module.Module;
import module.decode.p25.message.ldu.LDUMessage;
import module.decode.p25.message.tdu.TDUMessage;
import module.decode.p25.message.tdu.lc.TDULinkControlMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controller.channel.ChannelEvent;
import controller.channel.IChannelEventListener;
import sample.Listener;
import alias.Metadata;
import audio.AudioFormats;
import audio.AudioPacket;
import audio.IAudioPacketProvider;
import audio.metadata.AudioMetadata;
import audio.metadata.IMetadataListener;

public class P25AudioModule extends Module implements Listener<Message>, 
	IAudioPacketProvider, IMessageListener, IChannelEventListener, IMetadataListener
{
	private final static Logger mLog = LoggerFactory.getLogger( P25AudioModule.class );

	private static final String IMBE_CODEC = "IMBE";

	/* Provides a unique identifier for this audio module instance to use as a
	 * source identifier for all audio packets */
	private static int UNIQUE_ID = 0;
	private int mSourceID;
	private static boolean mLibraryLoadStatusLogged = false;
	private boolean mCanConvertAudio = false;
	private boolean mEncryptedAudio = false;
	private AudioConverter mAudioConverter;
	private Listener<AudioPacket> mAudioPacketListener;
	private AudioMetadata mAudioMetadata;
	private ChannelEventListener mChannelEventListener = new ChannelEventListener();

	public P25AudioModule()
	{
		mSourceID = ++UNIQUE_ID;
		mAudioMetadata = new AudioMetadata( mSourceID );
	}
	
	@Override
	public Listener<Message> getMessageListener()
	{
		return this;
	}

	@Override
	public void dispose()
	{
		mAudioConverter = null;
	}

	@Override
	public void init()
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
		if( mCanConvertAudio && mAudioPacketListener != null )
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
						float[] audio = mAudioConverter.convert( frame );

						mAudioPacketListener.receive( 
								new AudioPacket( audio, mAudioMetadata ) );
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
				
				if( !mLibraryLoadStatusLogged )
				{
					mLog.info( "JMBE audio conversion library successfully loaded"
							+ " - P25 audio will be available" );
					
					mLibraryLoadStatusLogged = true;
				}
			}
			else
			{
				if( !mLibraryLoadStatusLogged )
				{
					mLog.info( "JMBE audio conversion library NOT FOUND" );
					mLibraryLoadStatusLogged = true;
				}
			}
		} 
		catch ( ClassNotFoundException e1 )
		{
			if( !mLibraryLoadStatusLogged )
			{
				mLog.error( "Couldn't find/load JMBE audio conversion library" );
				mLibraryLoadStatusLogged = true;
			}
		}
		catch ( InstantiationException e1 )
		{
			if( !mLibraryLoadStatusLogged )
			{
				mLog.error( "Couldn't instantiate JMBE audio conversion library class" );
				mLibraryLoadStatusLogged = true;
			}
		}
		catch ( IllegalAccessException e1 )
		{
			if( !mLibraryLoadStatusLogged )
			{
				mLog.error( "Couldn't load JMBE audio conversion library due to "
						+ "security restrictions" );
				mLibraryLoadStatusLogged = true;
			}
		}
	}

	@Override
	public void setAudioPacketListener( Listener<AudioPacket> listener )
	{
		mAudioPacketListener = listener;
	}

	@Override
	public void removeAudioPacketListener()
	{
		mAudioPacketListener = null;
	}
	
	@Override
	public Listener<Metadata> getMetadataListener()
	{
		return mAudioMetadata;
	}

	@Override
	public Listener<ChannelEvent> getChannelEventListener()
	{
		return mChannelEventListener;
	}

	/**
	 * Wrapper for channel event listener.  Responds to channel state reset
	 * events to remove/cleanup current audio metadata
	 */
	public class ChannelEventListener implements Listener<ChannelEvent>
	{
		@Override
		public void receive( ChannelEvent event )
		{
			switch( event.getEvent() )
			{
				case CHANNEL_STATE_RESET:
					mAudioMetadata.reset();
					break;
				case CHANGE_SELECTED:
					mAudioMetadata.setSelected( event.getChannel().isSelected() );
					break;
				default:
					break;
			}
		}
	}
}
