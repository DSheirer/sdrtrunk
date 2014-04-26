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

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import settings.ColorSetting;
import settings.Setting;
import alias.Alias;
import alias.AliasList;
import controller.channel.Channel;
import controller.state.ChannelState.ChangedAttribute;
import controller.state.ChannelStatePanel;

public class MPT1327Panel extends ChannelStatePanel
{
    private static final long serialVersionUID = 1L;

    private JLabel mProtocol;
    private JLabel mSourceLabel;
    private JLabel mChannelLabel;

    private JLabel mStateLabel;
    private JLabel mTalkgroup;
    private JLabel mTalkgroupAlias;
    
    private JLabel mSiteLabel;
    private JLabel mSite;
    private JLabel mSiteAliasLabel;
	
    private MPT1327ChannelState mState;
    
    private AliasList mAliasList;

    public MPT1327Panel( Channel channel )
	{
		super( channel );
		mState = (MPT1327ChannelState)channel.getProcessingChain().getChannelState();
		mState.addListener( this );

		mAliasList = mState.getAliasList();
		
		init();
	}
    
    public void dispose()
    {
    	super.dispose();
    	mState = null;
    	mAliasList = null;
    }
	
	public void init()
	{
		mProtocol = new JLabel( "MPT1327" );
		mProtocol.setFont( mFontDecoder );
		mProtocol.setForeground( mColorLabelDecoder );

		mSourceLabel = new JLabel( mChannel.getSourceConfiguration().getDescription() );
		mSourceLabel.setFont( mFontDetails );
		mSourceLabel.setForeground( mColorLabelDetails );
		
		mChannelLabel = new JLabel( mChannel.getChannelDisplayName() );
		mChannelLabel.setFont( mFontDetails );
		mChannelLabel.setForeground( mColorLabelDetails );
		
		mStateLabel = new JLabel( mChannel.getProcessingChain().
				getChannelState().getState().getDisplayValue() );
		mStateLabel.setFont( mFontDecoder );
		mStateLabel.setForeground( mColorLabelDecoder );

		mTalkgroup = new JLabel( mState.getFromTalkgroup() );
		mTalkgroup.setFont( mFontDecoder );
		mTalkgroup.setForeground( mColorLabelDecoder );

		mTalkgroupAlias = new JLabel();
		mTalkgroupAlias.setFont( mFontDecoder );
		mTalkgroupAlias.setForeground( mColorLabelDecoder );

		Alias alias = getTalkgroupAlias( mState.getFromTalkgroup() );
		
		if( alias != null )
		{
			mTalkgroupAlias.setText( alias.getName() );
			mTalkgroupAlias.setIcon( getIcon( alias ) );
		}

		mSiteLabel = new JLabel( "Site:" );
		mSiteLabel.setFont( mFontDetails );
		mSiteLabel.setForeground( mColorLabelDetails );

		mSite = new JLabel( String.valueOf( mState.getSite() ) );
		mSite.setFont( mFontDecoder );
		mSite.setForeground( mColorLabelDecoder );

		mSiteAliasLabel = new JLabel();
		mSiteAliasLabel.setFont( mFontDecoder );
		mSiteAliasLabel.setForeground( mColorLabelDecoder );
		
		Alias siteAlias = getSiteAlias( mState.getSite() );
		
		if( siteAlias != null )
		{
			mSiteAliasLabel.setText( siteAlias.getName() );
		}

		add( mProtocol );
		add( mSourceLabel );
		add( mChannelLabel, "wrap" );

		add( mStateLabel );
		add( mTalkgroup );
		add( mTalkgroupAlias, "wrap" );
		
		add( mSiteLabel );
		add( mSite );
		add( mSiteAliasLabel, "wrap" );
	}

	@Override
    public void receive( final ChangedAttribute changedAttribute )
    {
		EventQueue.invokeLater( new Runnable() 
		{
			@Override
            public void run()
            {
				switch( changedAttribute )
				{
					case CHANNEL_STATE:
						mStateLabel.setText( mState.getState().getDisplayValue() );
						break;
					case CHANNEL_SITE_NUMBER:
						mSite.setText( String.valueOf( mState.getSite() ) );
						
						Alias siteAlias = getSiteAlias( mState.getSite() );
						
						if( siteAlias != null )
						{
							mSiteAliasLabel.setText( siteAlias.getName() );
						}
						repaint();
						break;
					case FROM_TALKGROUP:
						mTalkgroup.setText( mState.getFromTalkgroup() );

						Alias alias = getTalkgroupAlias( mState.getFromTalkgroup() );
						
						if( alias != null )
						{
							mTalkgroupAlias.setText( alias.getName() );
							mTalkgroupAlias.setIcon( getIcon( alias ) );
						}
						repaint();
						break;
				}
            }
		} );
    }
	
	private ImageIcon getIcon( Alias alias )
	{
		if( alias != null )
		{
			String iconName = alias.getIconName();
			
			if( iconName != null )
			{
				return getSettingsManager().getImageIcon( iconName, 12 );
			}
		}
		
		return null;
	}

	private Alias getSiteAlias( int site )
	{
		if( site != 0 && mAliasList != null )
		{
			return mAliasList.getSiteID( site );
		}
		
		return null;
	}
	
	private Alias getTalkgroupAlias( String talkgroup )
	{
		if( talkgroup != null && mAliasList != null )
		{
			return mAliasList.getTalkgroupAlias( talkgroup );
		}
		
		return null;
	}
	
	@Override
	public void trafficChannelAdded( Channel traffic )
	{
		if( !getTrafficPanels().keySet().contains( traffic ) )
		{
			MPT1327TrafficPanel panel = 
				new MPT1327TrafficPanel( traffic );
			
			getTrafficPanels().put( traffic, panel );

			addPanel( panel );
		}
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
					if( mProtocol != null )
					{
						mProtocol.setForeground( mColorLabelDecoder );
					}
					if( mStateLabel != null )
					{
						mStateLabel.setForeground( mColorLabelDecoder );
					}
					if( mTalkgroup != null )
					{
						mTalkgroup.setForeground( mColorLabelDecoder );
					}
					if( mTalkgroupAlias != null )
					{
						mTalkgroupAlias.setForeground( mColorLabelDecoder );
					}
					if( mSite != null )
					{
						mSite.setForeground( mColorLabelDecoder );
					}
					if( mSiteAliasLabel != null )
					{
						mSiteAliasLabel.setForeground( mColorLabelDecoder );
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
					if( mSiteLabel != null )
					{
						mSiteLabel.setForeground( mColorLabelDetails );
					}
					break;
			}
		}
    }
}
