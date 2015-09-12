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
package module.decode.passport;

import java.awt.EventQueue;

import javax.swing.JLabel;

import module.decode.state.ChangedAttribute;
import module.decode.state.DecoderPanel;
import settings.ColorSetting;
import settings.Setting;
import settings.SettingsManager;
import alias.Alias;

public class PassportDecoderPanel extends DecoderPanel
{
    private static final long serialVersionUID = 1L;

    private JLabel mProtocol = new JLabel( "Passport" );
    private JLabel mToTalkgroup = new JLabel();
    private JLabel mToTalkgroupAlias = new JLabel();
    
    private JLabel mSiteAndChannel = new JLabel( "Site:" );
    private JLabel mFromTalkgroup = new JLabel();
    private JLabel mFromTalkgroupAlias = new JLabel();
    
	public PassportDecoderPanel( SettingsManager settingsManager, PassportDecoder decoder )
	{
		super( settingsManager, decoder );
		
		init();
	}
	
	public void dispose()
	{
		super.dispose();
	}
	
	public void init()
	{
		mProtocol.setFont( mFontDecoder );
		mProtocol.setForeground( mColorLabelDecoder );

		mToTalkgroup.setFont( mFontDecoder );
		mToTalkgroup.setForeground( mColorLabelDecoder );

		mToTalkgroupAlias.setFont( mFontDecoder );
		mToTalkgroupAlias.setForeground( mColorLabelDecoder );

		mSiteAndChannel.setFont( mFontDecoder );
		mSiteAndChannel.setForeground( mColorLabelDecoder );

		mFromTalkgroup.setFont( mFontDecoder );
		mFromTalkgroup.setForeground( mColorLabelDecoder );

		mFromTalkgroupAlias.setFont( mFontDecoder );
		mFromTalkgroupAlias.setForeground( mColorLabelDecoder );

		add( mProtocol );
		add( mToTalkgroup );
		add( mToTalkgroupAlias, "wrap" );

		add( mSiteAndChannel );
		add( mFromTalkgroup );
		add( mFromTalkgroupAlias, "wrap" );
	}
	
	private PassportDecoderState getDecoderState()
	{
		return (PassportDecoderState)getDecoder().getDecoderState();
	}

	@Override
    public void receive( final ChangedAttribute changedAttribute )
    {
		EventQueue.invokeLater( new Runnable()
		{
			@Override
            public void run()
            {
				PassportDecoderState state = getDecoderState();
				
				switch( changedAttribute )
				{
					case TO_TALKGROUP:
						String tg = state.getTalkgroup();
						
						if( tg != null )
						{
				    		mToTalkgroup.setText( "TG:" + tg );
						}
						else
						{
				    		mToTalkgroup.setText( null );
						}
			    		break;
					case TO_TALKGROUP_ALIAS:
						Alias toAlias = state.getTalkgroupAlias();
						
			    		if( toAlias != null )
			    		{
							mToTalkgroupAlias.setText( toAlias.getName() );
							
							mToTalkgroupAlias.setIcon( mSettingsManager
								.getImageIcon( toAlias.getIconName(), SettingsManager.DEFAULT_ICON_SIZE ) );
			    		}
			    		else
			    		{
			    			mToTalkgroupAlias.setText( null );
			    			mToTalkgroupAlias.setIcon( null );
			    		}
			    		break;
					case FROM_TALKGROUP:
						String mid = state.getMobileID();
						
						if( mid != null )
						{
				    		mFromTalkgroup.setText( "MIN:" + mid );
						}
						else
						{
							mFromTalkgroup.setText( null );
						}
			    		break;
					case FROM_TALKGROUP_ALIAS:
			    		Alias fromAlias = state.getMobileIDAlias();
			    		
			    		if( fromAlias != null )
			    		{
				    		mFromTalkgroupAlias.setText( fromAlias.getName() );
				    		
				    		mFromTalkgroupAlias.setIcon( mSettingsManager
				    			.getImageIcon( fromAlias.getIconName(), SettingsManager.DEFAULT_ICON_SIZE ) );
			    		}
			    		else
			    		{
			    			mFromTalkgroupAlias.setText( null );
			    			mFromTalkgroupAlias.setIcon( null );
			    		}
			    		break;
					case CHANNEL_NUMBER:
						StringBuilder sb = new StringBuilder();
						sb.append( "Site: " );
						sb.append( state.getSiteNumber() );
						sb.append( "/" );
						sb.append( state.getChannelNumber() );
						
						mSiteAndChannel.setText( sb.toString() );
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
						if( mSiteAndChannel != null )
						{
							mSiteAndChannel.setForeground( mColorLabelDecoder );
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
				default:
					break;
			}
		}
    }
}
