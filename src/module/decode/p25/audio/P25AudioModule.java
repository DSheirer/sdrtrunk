package module.decode.p25.audio;

import jmbe.iface.AudioConversionLibrary;
import jmbe.iface.AudioConverter;
import message.IMessageListener;
import message.Message;
import module.Module;
import module.decode.p25.message.ldu.LDUMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import audio.AudioFormats;
import audio.AudioPacket;
import audio.IAudioPacketProvider;
import audio.metadata.AudioMetadata;
import audio.metadata.IMetadataListener;
import audio.metadata.Metadata;
import audio.squelch.ISquelchStateListener;
import audio.squelch.SquelchState;
import controller.channel.ChannelEvent;
import controller.channel.IChannelEventListener;
import dsp.filter.iir.DeemphasisFilter;
import dsp.gain.NonClippingGain;

public class P25AudioModule extends Module implements Listener<Message>, 
	IAudioPacketProvider, IMessageListener, IChannelEventListener, 
	IMetadataListener, ISquelchStateListener
{
	private final static Logger mLog = LoggerFactory.getLogger( P25AudioModule.class );

	private static final String IMBE_CODEC = "IMBE";

	/* Provides a unique identifier for this audio module instance to use as a
	 * source identifier for all audio packets */
	private static int UNIQUE_ID = 0;
	private int mSourceID;
	private static boolean mLibraryLoadStatusLogged = false;
	private boolean mCanConvertAudio = false;
	private AudioConverter mAudioConverter;
	private Listener<AudioPacket> mAudioPacketListener;
	private AudioMetadata mAudioMetadata;
	private ChannelEventListener mChannelEventListener = new ChannelEventListener();
	private SquelchStateListener mSquelchStateListener = new SquelchStateListener();
	private NonClippingGain mGain = new NonClippingGain( 5.0f, 0.95f );

	public P25AudioModule( boolean record )
	{
		mSourceID = ++UNIQUE_ID;
		mAudioMetadata = new AudioMetadata( mSourceID, record );
		
		loadConverter();
	}
	
	@Override
	public Listener<Message> getMessageListener()
	{
		return this;
	}

	@Override
	public Listener<SquelchState> getSquelchStateListener()
	{
		return mSquelchStateListener;
	}

	@Override
	public void dispose()
	{
		mAudioConverter = null;
	}

	@Override
	public void reset()
	{
		mAudioMetadata.reset();
	}

	@Override
	public void start()
	{
		
	}

	@Override
	public void stop()
	{
		
	}

	/**
	 * Primary processing method for p25 imbe audio frame messages.  Each LDU 
	 * audio message contains 9 imbe voice frames.  Each frame is converted to
	 * audio when the JMBE library is loaded, processed by auto-gain, and 
	 * broadcast to the audio packet listener.
	 */
	public void receive( Message message )
	{
		if( mCanConvertAudio && mAudioPacketListener != null )
		{
			if( message instanceof LDUMessage )
			{
				LDUMessage ldu = (LDUMessage)message;
				
				/* Only process unencrypted audio frames */
				if( !( ldu.isValid() && ldu.isEncrypted() ) )
				{
					for( byte[] frame: ldu.getIMBEFrames() )
					{
						float[] audio = mAudioConverter.decode( frame );

						audio = mGain.apply( audio );

						mAudioPacketListener.receive( 
								new AudioPacket( audio, mAudioMetadata.copyOf() ) );
					}
				}
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
			
			if( ( library.getMajorVersion() == 0 && 
				  library.getMinorVersion() >= 3 && 
				  library.getBuildVersion() >= 1 ) ||
				library.getMajorVersion() >= 1 )
			{
				mAudioConverter = library.getAudioConverter( IMBE_CODEC, 
						AudioFormats.PCM_SIGNED_48KHZ_16BITS_MONO );
				
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
			else
			{
				mLog.warn( "JMBE library version 0.3.1 or higher is required - found: " + library.getVersion() );
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
				case NOTIFICATION_STATE_RESET:
					mAudioMetadata.reset();
					break;
				case NOTIFICATION_SELECTION_CHANGE:
					mAudioMetadata.setSelected( event.getChannel().isSelected() );
					break;
				default:
					break;
			}
		}
	}
	
	/**
	 * Wrapper for squelch state listener.  Internally, the P25 audio module
	 * doesn't have a squelch state.  If there are IMBE audio frames, we have 
	 * audio.  We use this listener to signal to the recorder manager that the
	 * channel state is resetting and it should end the recording.
	 */
	public class SquelchStateListener implements Listener<SquelchState>
	{
		@Override
		public void receive( SquelchState state )
		{
			if( state == SquelchState.SQUELCH && mAudioPacketListener != null )
			{
				mAudioPacketListener.receive( new AudioPacket( AudioPacket.Type.END, 
						mAudioMetadata.copyOf() ) );

				mAudioMetadata.reset();
			}
		}
	}
}
