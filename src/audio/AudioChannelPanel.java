package audio;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import sample.Listener;
import settings.ColorSetting;
import settings.ColorSetting.ColorSettingName;
import settings.Setting;
import settings.SettingChangeListener;
import settings.SettingsManager;
import alias.Alias;
import audio.metadata.AudioMetadata;
import audio.metadata.Metadata;
import audio.metadata.MetadataType;
import audio.output.AudioOutput;

public class AudioChannelPanel extends JPanel 
				implements Listener<AudioEvent>, SettingChangeListener
{
	private static final long serialVersionUID = 1L;
	
    private Font mFont = new Font( Font.MONOSPACED, Font.PLAIN, 12 );
    private Color mLabelColor;
    private Color mDetailsColor;

    private SettingsManager mSettingsManager;
	private AudioOutput mAudioOutput;

	private JLabel mChannelName = new JLabel( " " );
	private JLabel mToLabel = new JLabel( "TO:" );
	private JLabel mTo = new JLabel( "" );
	private JLabel mToAlias = new JLabel( "" );
	
	private JLabel mMutedLabel = new JLabel( " " );
	private JLabel mFromLabel = new JLabel( "FROM:" );
	private JLabel mFrom = new JLabel( "" );
	private JLabel mFromAlias = new JLabel( "" );
	
	private boolean mConfigured = false;
	
	public AudioChannelPanel( SettingsManager settingsManager, AudioOutput audioOutput )
	{
		mSettingsManager = settingsManager;
		mSettingsManager.addListener( this );

		mAudioOutput = audioOutput;
		
		if( mAudioOutput != null )
		{
			mAudioOutput.addAudioEventListener( this );
			mAudioOutput.setAudioMetadataListener( new AudioMetadataProcessor() );
		}

		mLabelColor = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_LABEL_DECODER ).getColor();
		mDetailsColor = mSettingsManager.getColorSetting( 
    			ColorSettingName.CHANNEL_STATE_LABEL_DETAILS ).getColor();
		
		init();
	}
	
	private void init()
	{
		setLayout( new MigLayout( "insets 0 0 0 0", "[][right][][grow,fill]", "[]0[]0[grow,fill]" ) );
		setBackground( Color.BLACK );
		
		mChannelName = new JLabel( mAudioOutput != null ? mAudioOutput.getChannelName() : " " );
		mChannelName.setFont( mFont );
		mChannelName.setForeground( mDetailsColor );
		add( mChannelName );
		
		mToLabel.setFont( mFont );
		if( mAudioOutput != null )
		{
			mToLabel.setForeground( mLabelColor );
		}
		else
		{
			mToLabel.setForeground( getBackground() );
		}
		add( mToLabel );

		mTo.setFont( mFont );
		mTo.setForeground( mLabelColor );
		add( mTo );
		
		mToAlias.setFont( mFont );
		mToAlias.setForeground( mLabelColor );
		add( mToAlias,"wrap" );

		mMutedLabel.setFont( mFont );
		mMutedLabel.setForeground( Color.RED );
		add( mMutedLabel );

		mFromLabel.setFont( mFont );
		if( mAudioOutput != null )
		{
			mFromLabel.setForeground( mLabelColor );
		}
		else
		{
			mFromLabel.setForeground( getBackground() );
		}
		add( mFromLabel );
		
		mFrom.setFont( mFont );
		mFrom.setForeground( mLabelColor );
		add( mFrom );

		mFromAlias.setFont( mFont );
		mFromAlias.setForeground( mLabelColor );
		add( mFromAlias,"wrap" );
		
	}
	
	@Override
	public void receive( final AudioEvent audioEvent )
	{
		switch( audioEvent.getType() )
		{
			case AUDIO_STOPPED:
				EventQueue.invokeLater( new Runnable()
				{
					@Override
					public void run()
					{
						resetLabels();
					}
				} );
				break;
			case AUDIO_MUTED:
			case AUDIO_UNMUTED:
				EventQueue.invokeLater( new Runnable()
				{
					@Override
					public void run()
					{
						mMutedLabel.setText( mAudioOutput.isMuted() ? "M" : " " );
					}
				} );
				break;
			default:
				break;
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
						else
						{
							mFrom.setText( "-----" );
						}
						
						if( to != null )
						{
							mTo.setText( to.getValue() );
							updateAlias( mToAlias, to.getAlias() );
						}
						else
						{
							mTo.setText( "-----" );
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
