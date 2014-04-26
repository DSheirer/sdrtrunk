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
package controller.state;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;

import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import settings.ColorSetting.ColorSettingName;
import settings.ColorSettingMenuItem;
import settings.ColorSettingResetMenuItem;
import settings.SettingsManager;
import controller.channel.Channel;
import controller.channel.Channel.ChannelEvent;
import controller.channel.Channel.ChannelType;
import controller.channel.ChannelListener;
import decode.fleetsync2.FleetsyncChannelState;
import decode.fleetsync2.FleetsyncPanel;
import decode.ltrnet.LTRNetChannelState;
import decode.ltrnet.LTRNetPanel;
import decode.ltrstandard.LTRChannelState;
import decode.ltrstandard.LTRPanel;
import decode.mdc1200.MDCChannelState;
import decode.mdc1200.MDCPanel;
import decode.mpt1327.MPT1327ChannelState;
import decode.mpt1327.MPT1327Panel;
import decode.nbfm.NBFMChannelState;
import decode.nbfm.NBFMPanel;
import decode.passport.PassportChannelState;
import decode.passport.PassportPanel;

public class ChannelStateList extends JPanel implements ChannelListener
{
    private static final long serialVersionUID = 1L;
    
    private HashMap<Channel,ChannelStatePanel> mDisplayedPanels = 
    			new HashMap<Channel,ChannelStatePanel>();
    
    private SettingsManager mSettingsManager;
    
    public ChannelStateList( SettingsManager settingsManager )
    {
    	mSettingsManager = settingsManager;

    	init();
    }

    private void init()
    {
		setLayout( new MigLayout( "insets 0 0 0 0", "[grow,fill]", "[]0[]") );
		setBackground( Color.BLACK );
		
		addMouseListener( new ListSelectionListener() );
    }
    
	@Override
    public void occurred( Channel channel, ChannelEvent event )
    {
		ChannelType type = channel.getChannelType();
		
		switch( event )
		{
			case PROCESSING_STARTED:
				if( type == ChannelType.STANDARD )
				{
					channelStarted( channel );
				}
				else if( type == ChannelType.TRAFFIC )
				{
					for( Channel parent: mDisplayedPanels.keySet() )
					{
						if( parent.hasTrafficChannel( channel ) )
						{
							mDisplayedPanels.get( parent ).trafficChannelAdded( channel );
							validate();
						}
					}
				}
				break;
			case PROCESSING_STOPPED:
				if( type == ChannelType.STANDARD )
				{
					channelStopped( channel );
				}
				/* Since we don't have a reference to the parent channel that
				 * owns the traffic channel, iterate all channels to find the 
				 * parent */
				else if( type == ChannelType.TRAFFIC )
				{
					for( Channel control: mDisplayedPanels.keySet() )
					{
						if( control.hasTrafficChannel( channel ) )
						{
							control.removeTrafficChannel( channel );
							
							mDisplayedPanels.get( control ).trafficChannelDeleted( channel );

							validate();
						}
					}
				}
				break;
			default:
				break;
		}
    }

    public void channelStarted( Channel channel )
    {
    	if( !mDisplayedPanels.containsKey( channel ) )
    	{
    		ChannelState state = channel.getProcessingChain().getChannelState();

    		ChannelStatePanel panel = null;
    		
    		if( state instanceof NBFMChannelState )
    		{
    			NBFMChannelState convState = 
    					(NBFMChannelState)state;
    			
    			panel = new NBFMPanel( channel );

    			for( AuxChannelState aux: convState.getAuxChannelStates() )
    			{
    				if( aux instanceof FleetsyncChannelState )
    				{
    					FleetsyncPanel fsPanel = 
    							new FleetsyncPanel( mSettingsManager, 
    									(FleetsyncChannelState)aux );
    					
    					panel.addPanel( fsPanel );
    				}
    				else if( aux instanceof MDCChannelState )
    				{
    					MDCPanel mdsPanel = new MDCPanel( mSettingsManager, 
    									(MDCChannelState)aux );
    					
    					panel.addPanel( mdsPanel );
    				}
    			}
    		}
    		else if( state instanceof LTRChannelState )
    		{
    			LTRChannelState ltrState = (LTRChannelState)state;
    			
    			panel = new LTRPanel( mSettingsManager, channel );

    			for( AuxChannelState aux: ltrState.getAuxChannelStates() )
    			{
    				if( aux instanceof FleetsyncChannelState )
    				{
    					FleetsyncPanel fsPanel = new FleetsyncPanel( mSettingsManager,
    							(FleetsyncChannelState)aux );
    					
    					panel.addPanel( fsPanel );
    				}
    			}
    		}
    		else if( state instanceof LTRNetChannelState )
    		{
    			LTRNetChannelState ltrState = (LTRNetChannelState)state;
    			
    			panel = new LTRNetPanel( channel );

    			for( AuxChannelState aux: ltrState.getAuxChannelStates() )
    			{
    				if( aux instanceof FleetsyncChannelState )
    				{
    					FleetsyncPanel fsPanel = new FleetsyncPanel( mSettingsManager,
    							(FleetsyncChannelState)aux );
    					
    					panel.addPanel( fsPanel );
    				}
    			}
    		}
    		else if( state instanceof MPT1327ChannelState )
    		{
    			MPT1327ChannelState mptState = (MPT1327ChannelState)state;
    			
    			panel = new MPT1327Panel( channel );
    		}
    		else if( state instanceof PassportChannelState )
    		{
    			PassportChannelState passportState = (PassportChannelState)state;
    			
    			panel = new PassportPanel( mSettingsManager, channel );

    			for( AuxChannelState aux: passportState.getAuxChannelStates() )
    			{
    				if( aux instanceof FleetsyncChannelState )
    				{
    					FleetsyncPanel fsPanel = new FleetsyncPanel( mSettingsManager,
    							(FleetsyncChannelState)aux );
    					
    					panel.addPanel( fsPanel );
    				}
    			}
    		}
    		
    		if( panel != null )
    		{
    			add( panel, "wrap" );
    			
    			mDisplayedPanels.put( channel, panel );
    			
    			validate();
    		}
    	}
    }

    public void channelStopped( Channel channel )
    {
		if( mDisplayedPanels.containsKey( channel ) )
		{
			ChannelStatePanel panel = mDisplayedPanels.get( channel );
			
			if( panel != null )
			{
				remove( panel );
				
				validate();
			}

			mDisplayedPanels.remove( channel );
		}
    }
	
	public void setSelectedChannel( ChannelStatePanel selectedPanel )
	{
		boolean current = selectedPanel.getSelected();
		
		for( ChannelStatePanel panel: mDisplayedPanels.values() )
		{
			//Turn selected off
			panel.setSelected( false );
		}

		selectedPanel.setSelected( !current );
	}
	
	public class ListSelectionListener implements MouseListener
	{
		@Override
        public void mouseClicked( MouseEvent e )
        {
			Component component = getComponentAt( e.getPoint() );

			if( component instanceof ChannelStatePanel )
			{
				ChannelStatePanel panel = (ChannelStatePanel)component;

				/* If the panel has child traffic channels, attempt to get
				 * one of them for the mouse event */
				if( panel.getChannel().getTrafficChannels().size() > 0 )
				{
					for( ChannelStatePanel trafficPanel: panel.getTrafficPanels().values() )
					{
						Point translatedPoint = SwingUtilities
								.convertPoint( e.getComponent(), e.getPoint(), 
										trafficPanel );

						if( trafficPanel.contains( translatedPoint ) )
						{
							panel = trafficPanel;
							break;
						}
					}
				}
				
				if( e.getButton() == MouseEvent.BUTTON1 )
				{
					setSelectedChannel( panel );
				}
				else if( e.getButton() == MouseEvent.BUTTON3 )
				{
					JPopupMenu popupMenu = new JPopupMenu();
					JMenu menu = panel.getChannel().getContextMenu();

					popupMenu.add( menu );
					
					JMenu colorMenu = new JMenu( "Color" );
					popupMenu.add( colorMenu );

					JMenu backgroundMenu = new JMenu( "Background" );
					colorMenu.add( backgroundMenu );
					
					backgroundMenu.add( new ColorSettingMenuItem( mSettingsManager, 
							ColorSettingName.CHANNEL_STATE_BACKGROUND ) );
					backgroundMenu.add( new ColorSettingResetMenuItem( mSettingsManager, 
							ColorSettingName.CHANNEL_STATE_BACKGROUND ) );
					
					JMenu channelsMenu = new JMenu( "Channel State" );
					colorMenu.add( channelsMenu );
					
					JMenu callMenu = new JMenu( "Call" );
					channelsMenu.add( callMenu );
					
					callMenu.add( new ColorSettingMenuItem( mSettingsManager, 
							ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_CALL ) );
					callMenu.add( new ColorSettingResetMenuItem( mSettingsManager, 
						ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_CALL ) );
					callMenu.add( new JSeparator() );	

					callMenu.add( new ColorSettingMenuItem( mSettingsManager, 
							ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_CALL ) );
					callMenu.add( new ColorSettingResetMenuItem( mSettingsManager, 
						ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_CALL ) );
					
					JMenu controlMenu = new JMenu( "Control" );
					channelsMenu.add( controlMenu );
					
					controlMenu.add( new ColorSettingMenuItem( mSettingsManager, 
							ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_CONTROL ) );
					controlMenu.add( new ColorSettingResetMenuItem( mSettingsManager, 
						ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_CONTROL ) );
					controlMenu.add( new JSeparator() );	
					
					controlMenu.add( new ColorSettingMenuItem( mSettingsManager, 
							ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_CONTROL ) );
					controlMenu.add( new ColorSettingResetMenuItem( mSettingsManager, 
						ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_CONTROL ) );
					
					JMenu dataMenu = new JMenu( "Data" );
					channelsMenu.add( dataMenu );
					
					dataMenu.add( new ColorSettingMenuItem( mSettingsManager, 
							ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_DATA ) );
					dataMenu.add( new ColorSettingResetMenuItem( mSettingsManager, 
						ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_DATA ) );
					dataMenu.add( new JSeparator() );	
					
					dataMenu.add( new ColorSettingMenuItem( mSettingsManager, 
							ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_DATA ) );
					dataMenu.add( new ColorSettingResetMenuItem( mSettingsManager, 
						ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_DATA ) );
					
					JMenu fadeMenu = new JMenu( "Fade" );
					channelsMenu.add( fadeMenu );
					
					fadeMenu.add( new ColorSettingMenuItem( mSettingsManager, 
							ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_FADE ) );
					fadeMenu.add( new ColorSettingResetMenuItem( mSettingsManager, 
						ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_FADE ) );
					fadeMenu.add( new JSeparator() );	
					
					fadeMenu.add( new ColorSettingMenuItem( mSettingsManager, 
							ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_FADE ) );
					fadeMenu.add( new ColorSettingResetMenuItem( mSettingsManager, 
						ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_FADE ) );
					
					JMenu idleMenu = new JMenu( "Idle" );
					channelsMenu.add( idleMenu );
					
					idleMenu.add( new ColorSettingMenuItem( mSettingsManager, 
							ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_IDLE ) );
					idleMenu.add( new ColorSettingResetMenuItem( mSettingsManager, 
						ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_IDLE ) );
					idleMenu.add( new JSeparator() );	
					
					idleMenu.add( new ColorSettingMenuItem( mSettingsManager, 
							ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_IDLE ) );
					idleMenu.add( new ColorSettingResetMenuItem( mSettingsManager, 
						ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_IDLE ) );
					
					JMenu noTunerMenu = new JMenu( "No Tuner" );
					channelsMenu.add( noTunerMenu );
					
					noTunerMenu.add( new ColorSettingMenuItem( mSettingsManager, 
							ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_NO_TUNER ) );
					noTunerMenu.add( new ColorSettingResetMenuItem( mSettingsManager, 
						ColorSettingName.CHANNEL_STATE_GRADIENT_MIDDLE_NO_TUNER ) );
					noTunerMenu.add( new JSeparator() );	
					
					noTunerMenu.add( new ColorSettingMenuItem( mSettingsManager, 
							ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_NO_TUNER ) );
					noTunerMenu.add( new ColorSettingResetMenuItem( mSettingsManager, 
						ColorSettingName.CHANNEL_STATE_GRADIENT_TOP_NO_TUNER ) );
					
					JMenu labelMenu = new JMenu( "Labels" );
					colorMenu.add( labelMenu );
					
					labelMenu.add( new ColorSettingMenuItem( mSettingsManager, 
						ColorSettingName.CHANNEL_STATE_LABEL_AUX_DECODER ) );
					labelMenu.add( new ColorSettingResetMenuItem( mSettingsManager, 
						ColorSettingName.CHANNEL_STATE_LABEL_AUX_DECODER ) );
					labelMenu.add( new JSeparator() );	
					
					labelMenu.add( new ColorSettingMenuItem( mSettingsManager, 
							ColorSettingName.CHANNEL_STATE_LABEL_DECODER ) );
					labelMenu.add( new ColorSettingResetMenuItem( mSettingsManager, 
							ColorSettingName.CHANNEL_STATE_LABEL_DECODER ) );
					labelMenu.add( new JSeparator() );	
						
					labelMenu.add( new ColorSettingMenuItem( mSettingsManager, 
							ColorSettingName.CHANNEL_STATE_LABEL_DETAILS ) );
					labelMenu.add( new ColorSettingResetMenuItem( mSettingsManager, 
							ColorSettingName.CHANNEL_STATE_LABEL_DETAILS ) );
						
					JMenu selectedMenu = new JMenu( "Selected" );
					colorMenu.add( selectedMenu );
					
					selectedMenu.add( new ColorSettingMenuItem( mSettingsManager, 
							ColorSettingName.CHANNEL_STATE_SELECTED_CHANNEL ) );
					selectedMenu.add( new ColorSettingResetMenuItem( mSettingsManager, 
							ColorSettingName.CHANNEL_STATE_SELECTED_CHANNEL ) );
					
					popupMenu.show( ChannelStateList.this, e.getX(), e.getY() );
				}
			}
        }

        public void mousePressed( MouseEvent e ) {}
        public void mouseReleased( MouseEvent e ) {}
        public void mouseEntered( MouseEvent e ) {}
        public void mouseExited( MouseEvent e ) {}
	}
}
