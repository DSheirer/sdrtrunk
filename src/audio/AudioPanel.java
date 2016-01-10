package audio;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.sound.sampled.FloatControl;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import settings.SettingsManager;
import source.SourceManager;
import source.mixer.MixerChannelConfiguration;
import source.mixer.MixerManager;
import audio.output.AudioOutput;

public class AudioPanel extends JPanel implements Listener<AudioEvent>
{
	private static final long serialVersionUID = 1L;
	
	private static final Logger mLog = LoggerFactory.getLogger( AudioPanel.class );
	
	private static ImageIcon MUTED_ICON = SettingsManager.getScaledIcon( 
			new ImageIcon( "images/audio_muted.png" ), 38 );
	private static ImageIcon UNMUTED_ICON = SettingsManager.getScaledIcon( 
			new ImageIcon( "images/audio_unmuted.png" ), 38 );

	private SettingsManager mSettingsManager;
	private SourceManager mSourceManager;
	private IAudioController mController;

	private JButton mMuteButton;
	private AudioChannelsPanel mAudioChannelsPanel;
	
	public AudioPanel( SettingsManager settingsManager, 
					   SourceManager sourceManager,
					   IAudioController controller )
	{
		mSettingsManager = settingsManager;
		mSourceManager = sourceManager;
		mController = controller;
		
		mController.addControllerListener( this );
		
		init();
	}
	
	private void init()
	{
		setLayout( new MigLayout( "insets 0 0 0 0", "[]0[grow,fill]", "[fill]0[]") );
		setBackground( Color.BLACK );

		mMuteButton = new MuteButton();
		mMuteButton.setBackground( getBackground() );
		add( mMuteButton );
		
		mAudioChannelsPanel = new AudioChannelsPanel( mSettingsManager, mController );
		
		add( mAudioChannelsPanel );
		
		addMouseListener( new MouseSelectionListener() );
	}
	
	@Override
	public void receive( AudioEvent event )
	{
		switch( event.getType() )
		{
			case AUDIO_CONFIGURATION_CHANGE_STARTED:
				break;
			case AUDIO_CONFIGURATION_CHANGE_COMPLETE:
				EventQueue.invokeLater( new Runnable()
				{
					@Override
					public void run()
					{
						remove( mAudioChannelsPanel );

						mAudioChannelsPanel = new AudioChannelsPanel( 
								mSettingsManager, mController );
						
						add( mAudioChannelsPanel );

						mAudioChannelsPanel.repaint();

						revalidate();
						repaint();
					}
				} );
				break;
			default:
				break;
		}
	}

	/**
	 * Audio output mute control menu item.
	 */
	public class AudioOutputMuteItem extends JMenuItem
	{
		private static final long serialVersionUID = 1L;
		
		private AudioOutput mAudioOutput;
		
		public AudioOutputMuteItem( AudioOutput audioOutput )
		{
			super( audioOutput.isMuted() ? "Unmute" : "Mute" );
			
			mAudioOutput = audioOutput;
			
			addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( ActionEvent e )
				{
					mAudioOutput.setMuted( !mAudioOutput.isMuted() );
				}
			} );
		}
	}
	
	/**
	 * Mouse listener
	 */
	public class MouseSelectionListener implements MouseListener
	{
		@Override
		public void mouseClicked( MouseEvent event )
		{
			if( SwingUtilities.isRightMouseButton( event ) )
			{
				JPopupMenu popup = new JPopupMenu();

				/* Audio mixer/output selection menus */
				JMenu outputMenu = new JMenu( "Audio Output" );

				MixerChannelConfiguration[] mixerConfigurations = 
						mSourceManager.getMixerManager().getOutputMixers();

				for( MixerChannelConfiguration mixerConfig: mixerConfigurations )
				{
					MixerSelectionItem mixerItem = new MixerSelectionItem( mixerConfig );
					
					try
					{
						MixerChannelConfiguration current = mController.getMixerChannelConfiguration();

						if( current != null && current.equals( mixerConfig ) )
						{
							mixerItem.setSelected( true );
						}
					}
					catch( AudioException e )
					{
						mLog.error( "Error while detecting current mixer "
								+ "channel configuration", e );
					}
					
					outputMenu.add( mixerItem );
				}
				
				popup.add( outputMenu );

				/* Audio output mute and volume control */
				for( AudioOutput output: mController.getAudioOutputs() )
				{
					JMenu menu = new JMenu( "Channel: " + output.getChannelName() );
					
					menu.add( new AudioOutputMuteItem( output ) );
					
					if( output.hasGainControl() )
					{
						JMenu volume = new JMenu( "Volume" );
						volume.add( new VolumeSlider( output.getGainControl() ) );
						menu.add( volume );
					}
					
					popup.add( menu );
				}
				
				popup.show( event.getComponent(), event.getX(), event.getY() );
			}
		}

		@Override
		public void mousePressed( MouseEvent e ) {}
		@Override
		public void mouseReleased( MouseEvent e ) {}
		@Override
		public void mouseEntered( MouseEvent e ) {}
		@Override
		public void mouseExited( MouseEvent e ) {}
	}
	
	/**
	 * Audio volume (gain) adjustment slider control
	 */
	public class VolumeSlider extends JSlider
	{
		private static final long serialVersionUID = 1L;
		
		private FloatControl mFloatControl;
		
		public VolumeSlider( FloatControl control )
		{
			super( 0, 100, 0 );
			
	    	setMajorTickSpacing( 25 );
	    	setMinorTickSpacing( 5 );
	    	setPaintTicks( true );
	    	setPaintLabels( true );

			mFloatControl = control;
			
			setValue( getIntegerValue( mFloatControl.getValue() ) );
			
			addChangeListener( new ChangeListener() 
			{
				@Override
				public void stateChanged( ChangeEvent event )
				{
					mFloatControl.shift( mFloatControl.getValue(), 
						getFloatValue( VolumeSlider.this.getValue() ), 
						1000 );
				}
			} );
			
	    	addMouseListener( new MouseListener()
			{
				@Override
				public void mouseClicked( MouseEvent event )
				{
					if( event.getClickCount() == 2 )
					{
						VolumeSlider.this.setValue( 50 );
					}
				}
				
				public void mouseReleased( MouseEvent arg0 ) {}
				public void mousePressed( MouseEvent arg0 ) {}
				public void mouseExited( MouseEvent arg0 ) {}
				public void mouseEntered( MouseEvent arg0 ) {}
			} );
		}

		/**
		 * Converts the integer value to a floating point value to use in the
		 * float control.  Assumes an integer value of 50 is the 0.0 dB mid
		 * point (ie no gain ) value.
		 */
		private int getIntegerValue( float value )
		{
			if( value == 0.0f )
			{
				return 50;
			}
			else if( value < 0.0f )
			{
				float ratio = value / mFloatControl.getMinimum();
				
				return 50 - (int)( ratio * 50.0f );
			}
			else
			{
				float ratio = value / mFloatControl.getMaximum();
				
				return 50 + (int)( ratio * 50.0f );
			}
		}
		
		private float getFloatValue( int value )
		{
			if( value == 50 )
			{
				return 0.0f;
			}
			else if( value < 50 )
			{
				return (float)( 50 - value ) / 50.0f * mFloatControl.getMinimum();
			}
			else
			{
				return (float)( value - 50 ) / 50.0f * mFloatControl.getMaximum();
			}
		}
	}
	
	/**
	 * Mixer/Channel configuration selection item
	 */
	public class MixerSelectionItem extends JMenuItem
	{
		private static final long serialVersionUID = 1L;

		private MixerChannelConfiguration mConfiguration;
		
		public MixerSelectionItem( MixerChannelConfiguration config )
		{
			super( config.toString() );
			
			mConfiguration = config;
			
			addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( ActionEvent e )
				{
					try
					{
						mController.setMixerChannelConfiguration( mConfiguration );
					} 
					catch ( AudioException e1 )
					{
						mLog.error( "Couldn't set mixer channel configuration "
								+ "to: " + mConfiguration.toString() );
						
						JOptionPane.showMessageDialog( MixerSelectionItem.this, 
							"Couldn't set [" + mConfiguration.toString() + 
							"] as the audio output device" );
					}
				}
			} );
		}
	}

	/**
	 * Mute button to mute all audio output channels exposed by the audio
	 * controller
	 */
	public class MuteButton extends JButton
	{
		private static final long serialVersionUID = 1L;
		
		private boolean mMuted = false;
		
		public MuteButton()
		{
			setIcon( UNMUTED_ICON );
			setBorderPainted( false );
			
			addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( ActionEvent e )
				{
					mMuted = !mMuted;
					
					for( AudioOutput output: mController.getAudioOutputs() )
					{
						output.setMuted( mMuted );
					}
					
					EventQueue.invokeLater( new Runnable()
					{
						@Override
						public void run()
						{
							setIcon( mMuted ? MUTED_ICON : UNMUTED_ICON );
						}
					} );
				}
			} );
		}
	}
}
