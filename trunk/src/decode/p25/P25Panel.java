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
package decode.p25;

import java.awt.EventQueue;

import javax.swing.JLabel;

import settings.ColorSetting;
import settings.Setting;
import controller.channel.Channel;
import controller.state.ChannelState.ChangedAttribute;
import controller.state.ChannelStatePanel;
import decode.config.DecodeConfigP25Phase1;

public class P25Panel extends ChannelStatePanel
{
    private static final long serialVersionUID = 1L;

    private JLabel mStateLabel;
    private JLabel mSourceLabel;
    private JLabel mChannelLabel;

    private JLabel mProtocol;
    private JLabel mFrom = new JLabel( " " );
    private JLabel mFromAlias = new JLabel( " " );
    
    private JLabel mNAC = new JLabel( "NAC:" );
    private JLabel mTo = new JLabel( " " );
    private JLabel mToAlias = new JLabel( " " );
    
    private JLabel mSystem = new JLabel( "SYS:" );
    private JLabel mSite = new JLabel( "Site:" );
    private JLabel mSiteAlias = new JLabel( "" );
    
	
	public P25Panel( Channel channel )
	{
		super( channel );

		DecodeConfigP25Phase1 p25Config = 
				(DecodeConfigP25Phase1)channel.getDecodeConfiguration();
		
		mProtocol = new JLabel( "P25-1 " + 
				p25Config.getModulation().getShortLabel() );
		
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

		mFrom.setFont( mFontDecoder );
		mFrom.setForeground( mColorLabelDecoder );
		
		mFromAlias.setFont( mFontDecoder );
		mFromAlias.setForeground( mColorLabelDecoder );

		mNAC.setFont( mFontDecoder );
		mNAC.setForeground( mColorLabelDecoder );

		mTo.setFont( mFontDecoder );
		mTo.setForeground( mColorLabelDecoder );
		
		mToAlias.setFont( mFontDecoder );
		mToAlias.setForeground( mColorLabelDecoder );

		mSystem.setFont( mFontDecoder );
		mSystem.setForeground( mColorLabelDecoder );

		mSiteAlias.setFont( mFontDecoder );
		mSiteAlias.setForeground( mColorLabelDecoder );

		mSite.setFont( mFontDecoder );
		mSite.setForeground( mColorLabelDecoder );

		add( mStateLabel );
		add( mSourceLabel );
		add( mChannelLabel, "wrap" );

		add( mProtocol );
		add( mFrom );
		add( mFromAlias, "wrap" );
		
		add( mNAC );
		add( mTo );
		add( mToAlias, "wrap" );
		
		add( mSystem );
		add( mSite );
		add( mSiteAlias, "wrap" );
	}

	@Override
    public void receive( final ChangedAttribute changedAttribute )
    {
		EventQueue.invokeLater( new Runnable()
		{
			@Override
            public void run()
            {
				final P25ChannelState channelState = (P25ChannelState)mChannel.
						getProcessingChain().getChannelState();
				
				switch( changedAttribute )
				{
					case CHANNEL_STATE:
			    		mStateLabel.setText( channelState.getState().getDisplayValue() );
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
					case NAC:
						mNAC.setText( "NAC:" + channelState.getNAC() );
						break;
					case SYSTEM:
						mSystem.setText( "SYS:" + channelState.getSystem() );
						break;
					case SITE:
						mSite.setText( "SITE:" + channelState.getSite() );
						break;
					case SITE_ALIAS:
						mSiteAlias.setText( channelState.getSiteAlias() );
						break;
					case FROM_TALKGROUP:
						mFrom.setText( channelState.getFromTalkgroup() );
						break;
					case FROM_TALKGROUP_ALIAS:
						mFromAlias.setText( channelState.getFromAlias() );
						break;
					case TO_TALKGROUP:
						mTo.setText( channelState.getToTalkgroup() );
						break;
					case TO_TALKGROUP_ALIAS:
						mToAlias.setText( channelState.getToAlias() );
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
