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
package decode.ltrnet;

import java.awt.EventQueue;

import javax.swing.JLabel;

import settings.ColorSetting;
import settings.Setting;
import alias.Alias;
import controller.channel.Channel;
import controller.state.ChannelState.ChangedAttribute;
import controller.state.ChannelStatePanel;

public class LTRNetPanel extends ChannelStatePanel
{
    private static final long serialVersionUID = 1L;
    
    private JLabel mStateLabel;
    private JLabel mSourceLabel;
    private JLabel mChannelLabel;

    private JLabel mProtocol = new JLabel( "LTR-Net" );
    private JLabel mTalkgroup = new JLabel();
    private JLabel mTalkgroupAlias = new JLabel();

    private JLabel mLCN = new JLabel( "LCN:" );
    private JLabel mDescription = new JLabel();
    
	public LTRNetPanel( Channel channel )
	{
		super( channel );
		
		init();
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

		mTalkgroup.setFont( mFontDecoder );
		mTalkgroup.setForeground( mColorLabelDecoder );

		mTalkgroupAlias.setFont( mFontDecoder );
		mTalkgroupAlias.setForeground( mColorLabelDecoder );

		mLCN.setFont( mFontDecoder );
		mLCN.setForeground( mColorLabelDecoder );

		mDescription.setFont( mFontDecoder );
		mDescription.setForeground( mColorLabelDecoder );

		add( mStateLabel );
		add( mSourceLabel );
		add( mChannelLabel, "wrap" );

		add( mProtocol );
		add( mTalkgroup );
		add( mTalkgroupAlias, "wrap" );
		
		add( mLCN );
		add( mDescription, "span,wrap" );
	}

	@Override
    public void receive( final ChangedAttribute changedAttribute )
    {
		EventQueue.invokeLater( new Runnable()
		{
			@Override
            public void run()
            {
				LTRNetChannelState state = (LTRNetChannelState)mChannel
						.getProcessingChain().getChannelState();

				switch( changedAttribute )
				{
					case CHANNEL_STATE:
			    		mStateLabel.setText( state.getState().getDisplayValue() );
			    		break;
					case SOURCE:
			    		mSourceLabel.setText( mChannel
			    				.getSourceConfiguration().getDescription() );
						break;
					case CHANNEL_NAME:
					case SITE_NAME:
					case SYSTEM_NAME:
				    	mChannelLabel.setText( mChannel.getChannelDisplayName() );
				    	break;
					case TO_TALKGROUP:
			    		mTalkgroup.setText( state.getToTalkgroup() );
			    		break;
					case TO_TALKGROUP_ALIAS:
						Alias tgAlias = state.getToTalkgroupAlias();
						
						if( tgAlias != null )
						{
							mTalkgroupAlias.setText( tgAlias.getName() );
							String iconNameString = tgAlias.getIconName();
				    		mTalkgroupAlias.setIcon( getSettingsManager()
				    				.getImageIcon( iconNameString, 12 ) );
						}
						else
						{
							mTalkgroupAlias.setText( null );
							mTalkgroupAlias.setIcon( null );
						}
						break;
					case DESCRIPTION:
						mDescription.setText( state.getDescription() );
						break;
					case CHANNEL_NUMBER:
						mLCN.setText( "LCN: " + state.getChannelNumber() );
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
					if( mStateLabel != null )
					{
						mStateLabel.setForeground( mColorLabelDecoder );
					}
					if( mProtocol != null )
					{
						mProtocol.setForeground( mColorLabelDecoder );
					}
					if( mTalkgroup != null )
					{
						mTalkgroup.setForeground( mColorLabelDecoder );
					}
					if( mTalkgroupAlias != null )
					{
						mTalkgroupAlias.setForeground( mColorLabelDecoder );
					}
					if( mLCN != null )
					{
						mLCN.setForeground( mColorLabelDecoder );
					}
					if( mDescription != null )
					{
						mDescription.setForeground( mColorLabelDecoder );
					}
					break;
				case CHANNEL_STATE_LABEL_DETAILS:
					if( mSourceLabel != null )
					{
						mSourceLabel.setForeground( mColorLabelDetails );
					}
					if( mChannelLabel != null )
					{
						mChannelLabel.setForeground( mColorLabelDetails );
					}
					break;
			}
		}
    }
}
