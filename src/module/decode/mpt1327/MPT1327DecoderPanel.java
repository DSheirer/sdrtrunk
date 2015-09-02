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
package module.decode.mpt1327;

import java.awt.EventQueue;

import javax.swing.JLabel;

import module.decode.state.ChangedAttribute;
import module.decode.state.DecoderPanel;
import net.miginfocom.swing.MigLayout;
import settings.ColorSetting;
import settings.Setting;
import settings.SettingsManager;
import alias.Alias;
import controller.channel.Channel.ChannelType;

public class MPT1327DecoderPanel extends DecoderPanel
{
    private static final long serialVersionUID = 1L;

    private JLabel mProtocol = new JLabel( "" );
    private JLabel mSiteOrFromTalkgroup = new JLabel( "" );
    private JLabel mSiteOrFromTalkgroupAlias = new JLabel( "" );
    
    private JLabel mToTalkgroupLabel = new JLabel( "" );
    private JLabel mToTalkgroup = new JLabel( "" );
    private JLabel mToTalkgroupAliasLabel = new JLabel( "" );
	
    public MPT1327DecoderPanel( SettingsManager settingsManager, MPT1327Decoder decoder )
	{
		super( settingsManager, decoder );
		
		init();
	}
    
    private MPT1327DecoderState getDecoderState()
    {
    	return (MPT1327DecoderState)getDecoder().getDecoderState();
    }
    
    public void dispose()
    {
    	super.dispose();
    }
	
	public void init()
	{
		super.init();
		
    	setLayout( new MigLayout( "insets 0 0 0 0", "[grow,fill]", "[]0[]0[]") );

    	mProtocol.setText( "MPT1327" );
		mProtocol.setFont( mFontDecoder );
		mProtocol.setForeground( mColorLabelDecoder );

		MPT1327DecoderState mptState = getDecoderState();

		mSiteOrFromTalkgroup.setFont( mFontDecoder );
		mSiteOrFromTalkgroup.setForeground( mColorLabelDecoder );

		mSiteOrFromTalkgroupAlias.setFont( mFontDecoder );
		mSiteOrFromTalkgroupAlias.setForeground( mColorLabelDecoder );

		mToTalkgroupLabel.setFont( mFontDetails );
		mToTalkgroupLabel.setForeground( mColorLabelDetails );

		mToTalkgroup.setFont( mFontDecoder );
		mToTalkgroup.setForeground( mColorLabelDecoder );

		mToTalkgroupAliasLabel.setFont( mFontDecoder );
		mToTalkgroupAliasLabel.setForeground( mColorLabelDecoder );

		
		switch( mptState.getChannelType() )
		{
			case STANDARD:
				mSiteOrFromTalkgroup.setText( mptState.getSite() );
				setAliasLabel( mSiteOrFromTalkgroupAlias, mptState.getSiteAlias() );
				break;
			case TRAFFIC:
				mSiteOrFromTalkgroup.setText( mptState.getFromTalkgroup() );
				setAliasLabel( mSiteOrFromTalkgroupAlias, mptState.getFromTalkgroupAlias() );

				mToTalkgroupLabel.setText( "To:" );
				mToTalkgroup.setText( getDecoderState().getToTalkgroup() );
				setAliasLabel( mToTalkgroupAliasLabel, mptState.getToTalkgroupAlias() );
				break;
			default:
				throw new IllegalArgumentException( "Unrecognized channel "
					+ "type in MPT1327 Decoder [" + getDecoderState()
						.getChannelType().name() + "]" );
		}
		
		add( mProtocol );
		add( mSiteOrFromTalkgroup );
		add( mSiteOrFromTalkgroupAlias, "wrap" );

		if( mptState.getChannelType() == ChannelType.TRAFFIC )
		{
			add( mToTalkgroupLabel );
			add( mToTalkgroup );
			add( mToTalkgroupAliasLabel, "wrap" );
		}
	}
	
	private void setAliasLabel( final JLabel label, final Alias alias )
	{
		if( alias != null )
		{
			label.setText( alias.getName() );
			
			String iconName = alias.getIconName();
			
			if( iconName != null )
			{
				label.setIcon( mSettingsManager.getImageIcon( iconName, 12 ) );
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

	@Override
    public void receive( final ChangedAttribute changedAttribute )
    {
		final MPT1327DecoderState state = getDecoderState();
		
		EventQueue.invokeLater( new Runnable() 
		{
			@Override
            public void run()
            {
				switch( changedAttribute )
				{
					case CHANNEL_SITE_NUMBER:
						mSiteOrFromTalkgroup.setText( String.valueOf( state.getSite() ) );
						setAliasLabel( mSiteOrFromTalkgroupAlias, state.getSiteAlias() );
						repaint();
						break;
					case FROM_TALKGROUP:
						mSiteOrFromTalkgroup.setText( state.getFromTalkgroup() );
						setAliasLabel( mSiteOrFromTalkgroupAlias, state.getFromTalkgroupAlias() );
						repaint();
						break;
					case TO_TALKGROUP:
						mToTalkgroup.setText( state.getToTalkgroup() );
						setAliasLabel( mToTalkgroupAliasLabel, state.getToTalkgroupAlias() );
						repaint();
						break;
					default:
						break;
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
					if( mProtocol != null )
					{
						mProtocol.setForeground( mColorLabelDecoder );
					}
					if( mSiteOrFromTalkgroup != null )
					{
						mSiteOrFromTalkgroup.setForeground( mColorLabelDecoder );
					}
					if( mSiteOrFromTalkgroupAlias != null )
					{
						mSiteOrFromTalkgroupAlias.setForeground( mColorLabelDecoder );
					}
					if( mToTalkgroup != null )
					{
						mToTalkgroup.setForeground( mColorLabelDecoder );
					}
					if( mToTalkgroupAliasLabel != null )
					{
						mToTalkgroupAliasLabel.setForeground( mColorLabelDecoder );
					}
					break;
				case CHANNEL_STATE_LABEL_DETAILS:
					if( mToTalkgroupLabel != null )
					{
						mToTalkgroupLabel.setForeground( mColorLabelDetails );
					}
					break;
				default:
					break;
			}
		}
    }
}
