package audio;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.sound.sampled.BooleanControl;
import javax.swing.JButton;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import settings.SettingsManager;

public class AudioPanel extends JPanel implements Listener<AudioEvent>
{
	private static final long serialVersionUID = 1L;
	
	private static final Logger mLog = LoggerFactory.getLogger( AudioPanel.class );

	private SettingsManager mSettingsManager;
	private IAudioController mController;
	private MuteButton mMuteButton;
	
	public AudioPanel( SettingsManager settingsManager, IAudioController controller )
	{
		mSettingsManager = settingsManager;
		mController = controller;
		
		mMuteButton = new MuteButton( mController );
		
		init();
	}
	
	private void init()
	{
		setLayout( new MigLayout( "insets 0 0 0 0", "[grow,fill]", "[]0[]") );
		setBackground( Color.BLACK );

		add( mMuteButton, "span" );
		
		for( String channel: mController.getAudioChannels() )
		{
			add( new AudioChannelPanel( mSettingsManager, mController, channel ), "span" );
		}
	}
	
	public int getPreferredHeight()
	{
		return mMuteButton.getHeight() + 
			( mController.getAudioChannels().size() * 20 );
	}

	@Override
	public void receive( AudioEvent event )
	{
		if( event.getType() == AudioEvent.Type.AUDIO_CONFIGURATION_CHANGED )
		{
			mLog.debug( "Audio configuration change detected" );
		}
	}
	
	public class MuteButton extends JButton
	{
		private IAudioController mController;

		private boolean mMuted = false;
		
		public MuteButton( IAudioController controller )
		{
			super( "Mute Audio" );
			
			setBackground( Color.GREEN );
			
			mController = controller;
			
			addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( ActionEvent e )
				{
					mMuted = !mMuted;
					
					for( String channel: mController.getAudioChannels() )
					{
						try
						{
							BooleanControl control = mController.getMuteControl( channel );
							
							if( control != null )
							{
								control.setValue( mMuted );
							}
						} 
						catch ( AudioException e1 )
						{
							mLog.error( "Couldn't obtain mute control for "
								+ "channel [" + channel + "] from audio manager", e1 );
						}
					}

					EventQueue.invokeLater( new Runnable()
					{
						@Override
						public void run()
						{
							MuteButton.this.setText( ( mMuted ? "Unmute Audio" : "Mute Audio" ) );
							MuteButton.this.setBackground( ( mMuted ? Color.RED : Color.GREEN ) );
						}
					} );
				}
			} );
		}
	}
}
