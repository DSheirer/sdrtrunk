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
package decode.ltrstandard;

import java.awt.EventQueue;

import javax.swing.JLabel;

import settings.ColorSetting;
import settings.Setting;
import settings.SettingsManager;
import alias.Alias;
import controller.channel.Channel;
import controller.state.ChannelState.ChangedAttribute;
import controller.state.ChannelStatePanel;

public class LTRPanel extends ChannelStatePanel
{
    private static final long serialVersionUID = 1L;

    private JLabel mStateLabel;
    private JLabel mSourceLabel;
    private JLabel mChannelLabel;

    private JLabel mProtocol = new JLabel( "LTR Standard" );
    private JLabel mTalkgroup = new JLabel();
    private JLabel mTalkgroupAlias = new JLabel();

    private JLabel mLCNLabel = new JLabel( "LCN" );
    private JLabel mLCN = new JLabel();
    
    private SettingsManager mSettingsManager;
	
	public LTRPanel( SettingsManager settingsManager, Channel channel )
	{
		super( channel );
		
        channel.getProcessingChain().getChannelState().addListener( this );

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

		mTalkgroup.setFont( mFontDecoder );
		mTalkgroup.setForeground( mColorLabelDecoder );

		mTalkgroupAlias.setFont( mFontDecoder );
		mTalkgroupAlias.setForeground( mColorLabelDecoder );

        mLCNLabel.setFont( mFontDecoder );
        mLCNLabel.setForeground( mColorLabelDetails );

        mLCN.setFont( mFontDecoder );
        mLCN.setForeground( mColorLabelDecoder );

        add( mStateLabel );
		add( mSourceLabel );
		add( mChannelLabel, "wrap" );

		add( mProtocol );
		add( mTalkgroup );
		add( mTalkgroupAlias, "wrap" );
		
		add( mLCNLabel );
		add( mLCN, "wrap" );
	}

    @Override
    public void receive( final ChangedAttribute attribute )
    {
		EventQueue.invokeLater( new Runnable()
		{
			@Override
            public void run()
            {
				LTRChannelState state = (LTRChannelState)mChannel
						.getProcessingChain().getChannelState();
				
	            switch( attribute )
	            {
	                case CHANNEL_NUMBER:
	                    if( state.getChannelNumber() != 0 )
	                    {
	                        mLCN.setText( 
                        		String.valueOf( state.getChannelNumber() ) );
	                    }
	                    else
	                    {
	                        mLCN.setText( "" );
	                    }
	                    break;
	                case FROM_TALKGROUP:
	                    mTalkgroup.setText( state.getTalkgroup() );
	                    /* fall-through */
	                case FROM_TALKGROUP_ALIAS:
	                    Alias alias = state.getTalkgroupAlias();
	                    
	    	    		if( alias != null )
	    	    		{
	    					mTalkgroupAlias.setText( alias.getName() );
	    					mTalkgroupAlias.setIcon( mSettingsManager
    							.getImageIcon( alias.getIconName(), 12 ) );
	    	    		}
	    	    		else
	    	    		{
	    	    			mTalkgroupAlias.setText( null );
	    	    			mTalkgroupAlias.setIcon( null );
	    	    		}
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
					if( mLCNLabel != null )
					{
						mLCNLabel.setForeground( mColorLabelDetails );
					}
					break;
			}
		}
    }
}
