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
package decode.mpt1327;

import java.awt.EventQueue;

import javax.swing.JLabel;

import settings.ColorSetting;
import settings.Setting;
import alias.Alias;
import alias.AliasList;
import controller.channel.Channel;
import controller.channel.ProcessingChain;
import controller.state.ChannelState.ChangedAttribute;
import controller.state.ChannelStatePanel;

public class MPT1327TrafficPanel extends ChannelStatePanel
{
    private static final long serialVersionUID = 1L;

    private JLabel mProtocolLabel;
    private JLabel mFrequencyLabel;
    private JLabel mChannelLabel;

    private JLabel mStateLabel;
    private JLabel mFromTalkgroupLabel;
    private JLabel mFromTalkgroupAliasLabel;

    private JLabel mToLabel;
    private JLabel mToTalkgroupLabel;
    private JLabel mToTalkgroupAliasLabel;
    
    private AliasList mAliasList;

    public MPT1327TrafficPanel( Channel channel )
	{
		super( channel );
		
		mAliasList = channel.getProcessingChain().getChannelState().getAliasList();
		
		init();
	}
    
    public void dispose()
    {
    	super.dispose();
    	mAliasList = null;
    }
	
	public void init()
	{
		mProtocolLabel = new JLabel( "MPT1327" );
		mProtocolLabel.setFont( mFontDecoder );
		mProtocolLabel.setForeground( mColorLabelDecoder );

		mFrequencyLabel = new JLabel( mChannel.getSourceConfiguration().getDescription() );
		mFrequencyLabel.setFont( mFontDetails );
		mFrequencyLabel.setForeground( mColorLabelDetails );
		
		MPT1327ChannelState state = 
			(MPT1327ChannelState)mChannel.getProcessingChain().getChannelState();
		int channelNumber = state.getChannelNumber();
		
		mChannelLabel = new JLabel( "Channel " + channelNumber );
		mChannelLabel.setFont( mFontDetails );
		mChannelLabel.setForeground( mColorLabelDetails );
		
		mStateLabel = new JLabel( mChannel.getProcessingChain().
				getChannelState().getState().getDisplayValue() );
		mStateLabel.setFont( mFontDecoder );
		mStateLabel.setForeground( mColorLabelDecoder );

		mFromTalkgroupLabel = new JLabel( state.getFromTalkgroup() );
		mFromTalkgroupLabel.setFont( mFontDecoder );
		mFromTalkgroupLabel.setForeground( mColorLabelDecoder );

		mFromTalkgroupAliasLabel = new JLabel();
		mFromTalkgroupAliasLabel.setFont( mFontDecoder );
		mFromTalkgroupAliasLabel.setForeground( mColorLabelDecoder );

		if( mAliasList != null )
		{
			Alias alias = mAliasList.getMPT1327Alias( state.getFromTalkgroup() );
			
			if( alias != null )
			{
				mFromTalkgroupAliasLabel.setText( alias.getName() );

				mFromTalkgroupAliasLabel.setIcon( getChannel()
						.getResourceManager().getSettingsManager()
						.getImageIcon( alias.getIconName(), 12 ) );
			}
		}

		mToLabel = new JLabel( "TO:" );
		mToLabel.setFont( mFontDetails );
		mToLabel.setForeground( mColorLabelDetails );

		mToTalkgroupLabel = new JLabel( state.getFromTalkgroup() );
		mToTalkgroupLabel.setFont( mFontDecoder );
		mToTalkgroupLabel.setForeground( mColorLabelDecoder );

		mToTalkgroupAliasLabel = new JLabel();
		mToTalkgroupAliasLabel.setFont( mFontDecoder );
		mToTalkgroupAliasLabel.setForeground( mColorLabelDecoder );

		if( mAliasList != null )
		{
			Alias alias = mAliasList.getMPT1327Alias( state.getFromTalkgroup() );
			
			if( alias != null )
			{
				mToTalkgroupAliasLabel.setText( alias.getName() );

				mToTalkgroupAliasLabel.setIcon( getChannel()
						.getResourceManager().getSettingsManager()
						.getImageIcon( alias.getIconName(), 12 ) );
			}
		}

		add( mProtocolLabel );
		add( mFrequencyLabel );
		add( mChannelLabel, "wrap" );

		add( mStateLabel );
		add( mFromTalkgroupLabel );
		add( mFromTalkgroupAliasLabel, "wrap" );
		
		add( mToLabel );
		add( mToTalkgroupLabel );
		add( mToTalkgroupAliasLabel, "wrap" );
	}

	@Override
    public void receive( final ChangedAttribute changedAttribute )
    {
		EventQueue.invokeLater( new Runnable() 
		{
			@Override
            public void run()
            {
				ProcessingChain chain = getChannel().getProcessingChain();
				
				if( chain != null )
				{
					final MPT1327ChannelState state = 
							(MPT1327ChannelState)chain.getChannelState();
							
					if( state != null )
					{
						switch( changedAttribute )
						{
							case CHANNEL_STATE:
								mStateLabel.setText( state.getState().getDisplayValue() );
								break;
							case CHANNEL_NUMBER:
								mChannelLabel.setText( "Channel " + state.getChannelNumber() );
								break;
							case FROM_TALKGROUP:
								String talkgroup = state.getFromTalkgroup();
								
								mFromTalkgroupLabel.setText( talkgroup );
								
								if( mAliasList != null )
								{
									Alias alias = mAliasList.getMPT1327Alias( talkgroup );
									
									if( alias != null )
									{
										mFromTalkgroupAliasLabel.setText( alias.getName() );
										
										String icon = alias.getIconName();
										
										mFromTalkgroupAliasLabel.setIcon( getChannel()
											.getResourceManager().getSettingsManager()
											.getImageIcon( icon, 12 ) );
									}
								}
								break;
							case TO_TALKGROUP:
								String toTalkgroup = state.getToTalkgroup();
								
								mToTalkgroupLabel.setText( toTalkgroup );
								
								if( mAliasList != null )
								{
									Alias alias = mAliasList.getMPT1327Alias( toTalkgroup );
									
									if( alias != null )
									{
										mToTalkgroupAliasLabel.setText( alias.getName() );
										
										String icon = alias.getIconName();
										
										if( icon != null )
										{
											mToTalkgroupAliasLabel.setIcon( getChannel()
											.getResourceManager().getSettingsManager()
													.getImageIcon( icon, 12 ) );
										}
									}
								}
								break;
							default:
								break;
						}

						repaint();
					}
				}
            }
		} );
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
					mProtocolLabel.setForeground( mColorLabelDecoder );
					mStateLabel.setForeground( mColorLabelDecoder );
					mFromTalkgroupLabel.setForeground( mColorLabelDecoder );
					mFromTalkgroupAliasLabel.setForeground( mColorLabelDecoder );
					mToTalkgroupLabel.setForeground( mColorLabelDecoder );
					mToTalkgroupAliasLabel.setForeground( mColorLabelDecoder );
					break;
				case CHANNEL_STATE_LABEL_DETAILS:
					mFrequencyLabel.setForeground( mColorLabelDetails );
					mChannelLabel.setForeground( mColorLabelDetails );
					mToLabel.setForeground( mColorLabelDetails );
					break;
			}
		}
    }
}
