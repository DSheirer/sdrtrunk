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
package module.decode.p25;

import java.awt.EventQueue;

import javax.swing.JLabel;

import module.decode.state.ChangedAttribute;
import module.decode.state.DecoderPanel;
import net.miginfocom.swing.MigLayout;
import settings.ColorSetting;
import settings.Setting;
import settings.SettingsManager;
import alias.Alias;

public class P25DecoderPanel extends DecoderPanel
{
    private static final long serialVersionUID = 1L;

    private JLabel mProtocol;
    private JLabel mFrom = new JLabel( " " );
    private JLabel mFromAlias = new JLabel( " " );
    
    private JLabel mNAC = new JLabel( "NAC:" );
    private JLabel mTo = new JLabel( " " );
    private JLabel mToAlias = new JLabel( " " );
    
    private JLabel mSystem = new JLabel( "SYS:" );
    private JLabel mSite = new JLabel( "Site:" );
    private JLabel mSiteAlias = new JLabel( "" );
    
	
	public P25DecoderPanel( SettingsManager settingsManager, P25Decoder decoder )
	{
		super( settingsManager, decoder );

		mProtocol = new JLabel( "P25-1 " + decoder.getModulation().getShortLabel() );
		
		init();
	}
	
	public void init()
	{
		super.init();
		
    	setLayout( new MigLayout( "insets 0 0 0 0", "[grow,fill]", "[]0[]0[]") );

    	mProtocol.setFont( mFontDecoder );
		mProtocol.setForeground( mColorLabelDecoder );

		mFrom.setFont( mFontDecoder );
		mFrom.setForeground( mColorLabelDecoder );
		
		mFromAlias.setFont( mFontDecoder );
		mFromAlias.setForeground( mColorLabelDecoder );

		mNAC.setFont( mFontDetails );
		mNAC.setForeground( mColorLabelDecoder );

		mTo.setFont( mFontDecoder );
		mTo.setForeground( mColorLabelDecoder );
		
		mToAlias.setFont( mFontDecoder );
		mToAlias.setForeground( mColorLabelDecoder );

		mSystem.setFont( mFontDetails );
		mSystem.setForeground( mColorLabelDecoder );

		mSiteAlias.setFont( mFontDecoder );
		mSiteAlias.setForeground( mColorLabelDecoder );

		mSite.setFont( mFontDetails );
		mSite.setForeground( mColorLabelDecoder );

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
	
	private P25DecoderState getDecoderState()
	{
		return (P25DecoderState)getDecoder().getDecoderState();
	}

	@Override
    public void receive( final ChangedAttribute changedAttribute )
    {
		final P25DecoderState state = getDecoderState();

		EventQueue.invokeLater( new Runnable()
		{
			@Override
            public void run()
            {
				
				switch( changedAttribute )
				{
					case NAC:
						mNAC.setText( "NAC:" + state.getNAC() );
						break;
					case SYSTEM:
						mSystem.setText( "SYS:" + state.getSystem() );
						break;
					case SITE:
						mSite.setText( "SITE:" + state.getSite() );
						break;
					case SITE_ALIAS:
						mSiteAlias.setText( state.getSiteAlias() );
						break;
					case FROM_TALKGROUP:
						mFrom.setText( state.getFromTalkgroup() );
						break;
					case FROM_TALKGROUP_ALIAS:
						Alias from = state.getFromAlias();
						
						if( from != null )
						{
							mFromAlias.setText( from.getName() );
							mFromAlias.setIcon( mSettingsManager
				    				.getImageIcon( from.getIconName(), 12 )  );
						}
						else
						{
							mFromAlias.setText( null );
							mFromAlias.setIcon( null );
						}
						break;
					case TO_TALKGROUP:
						mTo.setText( state.getToTalkgroup() );
						break;
					case TO_TALKGROUP_ALIAS:
						Alias to = state.getToAlias();
						
						if( to != null )
						{
							mToAlias.setText( to.getName() );
							mToAlias.setIcon( mSettingsManager
				    				.getImageIcon( to.getIconName(), 12 )  );
						}
						else
						{
							mToAlias.setText( null );
							mToAlias.setIcon( null );
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
					if( mProtocol != null )
					{
						mProtocol.setForeground( mColorLabelDecoder );
					}
					break;
				default:
					break;
			}
		}
    }
}
