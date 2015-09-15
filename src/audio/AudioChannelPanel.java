package audio;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.FloatControl;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import settings.ColorSetting;
import settings.Setting;
import settings.SettingChangeListener;
import settings.SettingsManager;
import settings.ColorSetting.ColorSettingName;
import alias.Alias;
import audio.AudioEvent.Type;
import audio.metadata.AudioMetadata;
import audio.metadata.Metadata;
import audio.metadata.MetadataType;

public class AudioChannelPanel extends JPanel 
				implements Listener<AudioEvent>, SettingChangeListener
{
	private static final long serialVersionUID = 1L;
	
	private static final Logger mLog = LoggerFactory.getLogger( AudioChannelPanel.class );

    private Font mFont = new Font( Font.MONOSPACED, Font.PLAIN, 12 );
    private Color mLabelColor;
    private Color mDetailsColor;

    private SettingsManager mSettingsManager;
	private IAudioController mController;
	private String mChannel;

	private JLabel mToLabel = new JLabel( "TO:" );
	private JLabel mTo = new JLabel( "" );
	private JLabel mToAlias = new JLabel( "" );
	
	private JLabel mFromLabel = new JLabel( "FROM:" );
	private JLabel mFrom = new JLabel( "" );
	private JLabel mFromAlias = new JLabel( "" );
	
	private JLabel mChannelLabel;
	private JLabel mChannelName;
	private JLabel mMutedLabel;
	
	private boolean mConfigured = false;
	
	private BooleanControl mMuteControl;
	private FloatControl mGainControl;
	
	public AudioChannelPanel( SettingsManager settingsManager,
			  				  IAudioController controller, 
							  String channel )
	{
		mSettingsManager = settingsManager;
		mSettingsManager.addListener( this );

		mLabelColor = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_LABEL_DECODER ).getColor();
		mDetailsColor = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_LABEL_DETAILS ).getColor();
		
		mController = controller;
		mChannel = channel;
		
		try
		{
			mMuteControl = mController.getMuteControl( mChannel );
		} 
		catch ( AudioException e )
		{
			mLog.error( "Couldn't obtain mute control from audio channel [" + 
				mChannel + "]", e );
		}

		try
		{
			mGainControl = mController.getGainControl( mChannel );
		} 
		catch ( AudioException e )
		{
			mLog.error( "Couldn't obtain gain control from audio channel [" + 
				mChannel + "]", e );
		}
		
		try
		{
			mController.addAudioEventListener( mChannel, this );
		}
		catch( AudioException e )
		{
			mLog.error( "Couldn't register as audio event listener on channel [" + 
				mChannel + "]", e );
		}
		
		mController.setAudioMetadataListener( mChannel, new AudioMetadataProcessor() );
		
		init();
	}
	
	private void init()
	{
		setLayout( new MigLayout( "insets 0 0 0 0", "[right][][grow,fill]", "[grow,fill]0[]0[]" ) );
		setBackground( Color.BLACK );
		
		addMouseListener( new MouseSelectionListener() );
		
		mToLabel.setFont( mFont );
		mToLabel.setForeground( mLabelColor );
		add( mToLabel );

		mTo.setFont( mFont );
		mTo.setForeground( mLabelColor );
		add( mTo );
		
		mToAlias.setFont( mFont );
		mToAlias.setForeground( mLabelColor );
		add( mToAlias,"wrap" );

		mFromLabel.setFont( mFont );
		mFromLabel.setForeground( mLabelColor );
		add( mFromLabel );
		
		mFrom.setFont( mFont );
		mFrom.setForeground( mLabelColor );
		add( mFrom );

		mFromAlias.setFont( mFont );
		mFromAlias.setForeground( mLabelColor );
		add( mFromAlias,"wrap" );

		mChannelLabel = new JLabel( "Channel:" );
		mChannelLabel.setFont( mFont );
		mChannelLabel.setForeground( mDetailsColor );
		
		add( mChannelLabel );

		mChannelName = new JLabel( mChannel );
		mChannelName.setFont( mFont );
		mChannelName.setForeground( mDetailsColor );
		
		add( mChannelName );

		mMutedLabel = new JLabel( "" );
		mMutedLabel.setFont( mFont );
		mMutedLabel.setForeground( Color.RED );
		
		add( mMutedLabel, "wrap" );
	}
	
	private void updateMuteState()
	{
		if( mMuteControl != null )
		{
			EventQueue.invokeLater( new Runnable()
			{
				@Override
				public void run()
				{
					mMutedLabel.setText( mMuteControl.getValue() ? "Muted" : "" );
				}
			} );
		}
	}

	@Override
	public void receive( final AudioEvent audioEvent )
	{
		if( audioEvent.getType() == Type.AUDIO_STOPPED )
		{
			EventQueue.invokeLater( new Runnable()
			{
				@Override
				public void run()
				{
					resetLabels();
				}
			} );
		}
	}

	/**
	 * Resets the from and to labels.  Note: this does not happen on the swing
	 * event thread.  Only invoke from the swing thread.
	 */
	private void resetLabels()
	{
		mFrom.setText( "" );
		updateAlias( mFromAlias, null );
		
		mTo.setText( "" );
		updateAlias( mToAlias, null );
		
		mConfigured = false;
	}

	/**
	 * Updates the alias label with text and icon from the alias.  Note: this 
	 * does not occur on the Swing event thread -- wrap any calls to this
	 * method with an event thread call.
	 */
	private void updateAlias( JLabel label, Alias alias )
	{
		if( alias != null )
		{
			label.setText( alias.getName() );
			
			String icon = alias.getIconName();
			
			if( icon != null )
			{
				label.setIcon( mSettingsManager.getImageIcon( icon, 
						SettingsManager.DEFAULT_ICON_SIZE ) );
			}
			else
			{
				label.setIcon( null );
			}
		}
		else
		{
			label.setText( "" );
			label.setIcon( null );
		}
	}
	
	public class MouseSelectionListener implements MouseListener
	{
		@Override
		public void mouseClicked( MouseEvent event )
		{
			JPopupMenu popup = new JPopupMenu();

			if( mMuteControl != null )
			{
				JMenuItem mute = new JMenuItem( mMuteControl.getValue() ? 
					"Unmute Channel: " + mChannel : "Mute Channel: " + mChannel );
				
				mute.addActionListener( new ActionListener()
				{
					@Override
					public void actionPerformed( ActionEvent e )
					{
						mMuteControl.setValue( !mMuteControl.getValue() );
						updateMuteState();
					}
				} );
				popup.add( mute );
			}
			
			popup.show( event.getComponent(), event.getX(), event.getY() );
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
	 * Processes audio metadata to update this panel's display values
	 */
	public class AudioMetadataProcessor implements Listener<AudioMetadata>
	{
		@Override
		public void receive( AudioMetadata audioMetadata )
		{
			if( !mConfigured || audioMetadata.isUpdated() )
			{
				final Metadata from = audioMetadata.getMetadata( MetadataType.FROM );

				final Metadata to = audioMetadata.getMetadata( MetadataType.TO );
				
				EventQueue.invokeLater( new Runnable() 
				{
					@Override
					public void run()
					{
						if( from != null )
						{
							mFrom.setText( from.getValue() );
							updateAlias( mFromAlias, from.getAlias() );
						}
						
						if( to != null )
						{
							mTo.setText( to.getValue() );
							updateAlias( mToAlias, to.getAlias() );
						}
					}
				} );
				
				mConfigured = true;
			}
		}
	}


	@Override
    public void settingChanged( Setting setting )
    {
		if( setting instanceof ColorSetting )
		{
			ColorSetting colorSetting = (ColorSetting)setting;
			
			switch( colorSetting.getColorSettingName() )
			{
				case CHANNEL_STATE_LABEL_DECODER:
					if( mFrom != null )
					{
						mFrom.setForeground( mLabelColor );
					}
					if( mFromAlias != null )
					{
						mFromAlias.setForeground( mLabelColor );
					}
					if( mTo != null )
					{
						mTo.setForeground( mLabelColor );
					}
					if( mToAlias != null )
					{
						mToAlias.setForeground( mLabelColor );
					}
					break;
				case CHANNEL_STATE_LABEL_DETAILS:
					if( mFromLabel != null )
					{
						mFromLabel.setForeground( mDetailsColor );
					}
					if( mToLabel != null )
					{
						mToLabel.setForeground( mDetailsColor );
					}
					break;
				default:
					break;
			}
		}
    }

	@Override
	public void settingDeleted( Setting setting ) {}
}
