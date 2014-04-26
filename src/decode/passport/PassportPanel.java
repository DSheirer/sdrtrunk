/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package decode.passport;

import java.awt.EventQueue;

import javax.swing.JLabel;

import settings.ColorSetting;
import settings.Setting;
import settings.SettingsManager;
import alias.Alias;
import controller.channel.Channel;
import controller.state.ChannelState.ChangedAttribute;
import controller.state.ChannelStatePanel;

public class PassportPanel extends ChannelStatePanel
{
    private static final long serialVersionUID = 1L;

    private JLabel mStateLabel;
    private JLabel mSourceLabel;
    private JLabel mChannelLabel;

    private JLabel mProtocol = new JLabel( "Passport" );
    private JLabel mToTalkgroup = new JLabel();
    private JLabel mToTalkgroupAlias = new JLabel();
    
    private JLabel mFromTalkgroupType = new JLabel( " " );
    private JLabel mFromTalkgroup = new JLabel();
    private JLabel mFromTalkgroupAlias = new JLabel();
    
    private SettingsManager mSettingsManager;
	
	public PassportPanel( SettingsManager settingsManager, Channel channel )
	{
		super( channel );
		
		mSettingsManager = settingsManager;
		
		init();
	}
	
	public void dispose()
	{
		super.dispose();
		mSettingsManager = null;
	}
	
	public void init()
	{
		mStateLabel = new JLabel( mChannel.getProcessingChain().
				getChannelState().getState().getDisplayValue() );
		mStateLabel.setFont( mFontDecoder );
		mStateLabel.setForeground( mColorLabelDecoder );
		
		mSourceLabel = new JLabel( mChannel.getSourceConfiguration().getDescription() );
		mSourceLabel.setFont( mFontDetails );
		mSourceLabel.setForeground( mColorLabelDetails );
		
		mChannelLabel = new JLabel( mChannel.getChannelDisplayName() );
		mChannelLabel.setFont( mFontDetails );
		mChannelLabel.setForeground( mColorLabelDetails );
		
		mProtocol.setFont( mFontDecoder );
		mProtocol.setForeground( mColorLabelDecoder );

		mToTalkgroup.setFont( mFontDecoder );
		mToTalkgroup.setForeground( mColorLabelDecoder );

		mToTalkgroupAlias.setFont( mFontDecoder );
		mToTalkgroupAlias.setForeground( mColorLabelDecoder );

		mFromTalkgroupType.setFont( mFontDecoder );
		mFromTalkgroupType.setForeground( mColorLabelDecoder );

		mFromTalkgroup.setFont( mFontDecoder );
		mFromTalkgroup.setForeground( mColorLabelDecoder );

		mFromTalkgroupAlias.setFont( mFontDecoder );
		mFromTalkgroupAlias.setForeground( mColorLabelDecoder );

		add( mStateLabel );
		add( mSourceLabel );
		add( mChannelLabel, "wrap" );

		add( mProtocol );
		add( mToTalkgroup );
		add( mToTalkgroupAlias, "wrap" );

		add( mFromTalkgroupType );
		add( mFromTalkgroup );
		add( mFromTalkgroupAlias, "wrap" );
	}

	@Override
    public void receive( final ChangedAttribute changedAttribute )
    {
		EventQueue.invokeLater( new Runnable()
		{
			@Override
            public void run()
            {
				PassportChannelState state = (PassportChannelState)getChannel()
						.getProcessingChain().getChannelState();
				
				
				switch( changedAttribute )
				{
					case CHANNEL_STATE:
			    		mStateLabel.setText( state.getState().getDisplayValue() );
						break;
					case SOURCE:
			    		mSourceLabel.setText( mChannel.getSourceConfiguration()
			    				.getDescription() );
						break;
					case CHANNEL_NAME:
					case SITE_NAME:
					case SYSTEM_NAME:
				    	mChannelLabel.setText( mChannel.getChannelDisplayName() );
				    	break;
					case TO_TALKGROUP:
			    		mToTalkgroup.setText( state.getTalkgroup() );
			    		break;
					case TO_TALKGROUP_ALIAS:
						Alias toAlias = state.getTalkgroupAlias();
						
			    		if( toAlias != null )
			    		{
							mToTalkgroupAlias.setText( toAlias.getName() );
							
							mToTalkgroupAlias.setIcon( mSettingsManager
								.getImageIcon( toAlias.getIconName(), 12 ) );
			    		}
			    		else
			    		{
			    			mToTalkgroupAlias.setText( null );
			    			mToTalkgroupAlias.setIcon( null );
			    		}
			    		break;
					case FROM_TALKGROUP:
						String mid = state.getMobileID();
						
			    		mFromTalkgroup.setText( state.getMobileID() );

			    		if( mid != null )
			    		{
				    		mFromTalkgroupType.setText( "Mobile ID" );
			    		}
			    		else
			    		{
			    			mFromTalkgroupType.setText( " " );
			    		}
			    		break;
					case FROM_TALKGROUP_ALIAS:
			    		Alias fromAlias = state.getMobileIDAlias();
			    		
			    		if( fromAlias != null )
			    		{
				    		mFromTalkgroupAlias.setText( fromAlias.getName() );
				    		
				    		mFromTalkgroupAlias.setIcon( mSettingsManager
				    			.getImageIcon( fromAlias.getIconName(), 12 ) );
			    		}
			    		else
			    		{
			    			mFromTalkgroupAlias.setText( null );
			    			mFromTalkgroupAlias.setIcon( null );
			    		}
			    		break;
		    		default:
		    			break;
				}
				
				repaint();
            }
		});
    }
	
	@Override
    public void settingChanged( Setting setting )
    {
		super.settingChanged( setting );
		
		if( setting instanceof ColorSetting )
		{
			ColorSetting colorSetting = (ColorSetting)setting;
			
			switch( colorSetting.getColorSettingName() )
			{
				case CHANNEL_STATE_LABEL_DECODER:
					if( mColorLabelDecoder != null )
					{
						if( mStateLabel != null )
						{
							mStateLabel.setForeground( mColorLabelDecoder );
						}
						if( mProtocol != null )
						{
							mProtocol.setForeground( mColorLabelDecoder );
						}
						if( mToTalkgroup != null )
						{
							mToTalkgroup.setForeground( mColorLabelDecoder );
						}
						if( mToTalkgroupAlias != null )
						{
							mToTalkgroupAlias.setForeground( mColorLabelDecoder );
						}
						if( mFromTalkgroupType != null )
						{
							mFromTalkgroupType.setForeground( mColorLabelDecoder );
						}
						if( mFromTalkgroup != null )
						{
							mFromTalkgroup.setForeground( mColorLabelDecoder );
						}
						if( mFromTalkgroupAlias != null )
						{
							mFromTalkgroupAlias.setForeground( mColorLabelDecoder );
						}
					}
					break;
				case CHANNEL_STATE_LABEL_DETAILS:
					if( mColorLabelDetails != null )
					{
						if( mSourceLabel != null )
						{
							mSourceLabel.setForeground( mColorLabelDetails );
						}
						if( mChannelLabel != null )
						{
							mChannelLabel.setForeground( mColorLabelDetails );
						}
					}
					break;
			}
		}
    }
}
