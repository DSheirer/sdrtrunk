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
package controller;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import map.MapPanel;
import map.MapService;
import module.decode.event.CallEventPanel;
import module.decode.event.MessageActivityPanel;
import module.decode.state.ChannelList;
import net.miginfocom.swing.MigLayout;
import playlist.PlaylistManager;
import settings.SettingsManager;
import source.SourceManager;
import spectrum.ChannelSpectrumPanel;
import alias.AliasController;
import alias.AliasModel;
import audio.AudioManager;
import audio.AudioPanel;

import com.jidesoft.swing.JideSplitPane;
import com.jidesoft.swing.JideTabbedPane;

import controller.channel.ChannelController;
import controller.channel.ChannelModel;
import controller.channel.ChannelProcessingManager;

public class ControllerPanel extends JPanel
{
    private static final long serialVersionUID = 1L;

    private ChannelList mChannelStateList;

    private CallEventPanel mCallEventPanel;
    
    private MessageActivityPanel mMessageActivityPanel;
    
    private ChannelSpectrumPanel mChannelSpectrumPanel;

    private AliasController mAliasController;
    
	private ChannelController mChannelController;

	private JideTabbedPane mTabbedPane;

    protected ConfigurationTreePanel mSystemControlViewPanel;
	protected OldConfigurationEditor mConfigurationEditor;
    protected JideSplitPane mSystemControlSplitPane;

	protected JTable mChannelActivityTable = new JTable();
	private AudioPanel mAudioPanel;
	private MapPanel mMapPanel;

	private ChannelModel mChannelModel;
	private ConfigurationControllerModel mController;
	protected SettingsManager mSettingsManager;

	public ControllerPanel( AudioManager audioManager,
							ConfigurationControllerModel controller,
							AliasModel aliasModel,
							ChannelModel channelModel,
							ChannelProcessingManager channelProcessingManager,
							MapService mapService,
							PlaylistManager playlistManager,
							SettingsManager settingsManager,
							SourceManager sourceManager )
	{
		mChannelModel = channelModel;
		mController = controller;
	    mSettingsManager = settingsManager;

    	mAudioPanel = new AudioPanel( mSettingsManager, sourceManager, audioManager );

    	mMapPanel = new MapPanel( mapService, mSettingsManager );
	    
    	mMessageActivityPanel = new MessageActivityPanel( channelProcessingManager );

    	mCallEventPanel = new CallEventPanel( mSettingsManager, 
    			channelProcessingManager );	
	    
    	mChannelSpectrumPanel = new ChannelSpectrumPanel( mSettingsManager, 
    			channelProcessingManager );

    	mChannelStateList = new ChannelList( channelModel, channelProcessingManager, 
    			playlistManager, mSettingsManager );

    	mChannelController = new ChannelController( channelModel, playlistManager, sourceManager );
    	
    	mAliasController = new AliasController( aliasModel, mSettingsManager );

		init();
	}
	
	private void init()
	{
    	setLayout( new MigLayout( "insets 0 0 0 0 ", 
    							  "[grow,fill]", 
    							  "[grow,fill]") );
    	
    	//System Configuration View and Editor
    	mConfigurationEditor = new OldConfigurationEditor();

    	mSystemControlViewPanel = new ConfigurationTreePanel( mController );
    	mSystemControlViewPanel.addTreeSelectionListener( mConfigurationEditor );

    	mSystemControlSplitPane = new JideSplitPane( JideSplitPane.HORIZONTAL_SPLIT );
    	mSystemControlSplitPane.setDividerSize( 5 );
    	mSystemControlSplitPane.add( mSystemControlViewPanel );
    	mSystemControlSplitPane.add( mConfigurationEditor );
    	
    	//Tabbed View - configuration, calls, messages, map
    	mTabbedPane = new JideTabbedPane();
    	mTabbedPane.setFont( this.getFont() );
    	mTabbedPane.setForeground( Color.BLACK );
    	mTabbedPane.addTab( "Channels", mChannelController );
    	mTabbedPane.addTab( "Aliases", mAliasController );
    	mTabbedPane.addTab( "Configuration", mSystemControlSplitPane  );
    	mTabbedPane.addTab( "Channel Spectrum", mChannelSpectrumPanel );
    	mTabbedPane.addTab( "Events", mCallEventPanel );
    	mTabbedPane.addTab( "Messages", mMessageActivityPanel );
    	mTabbedPane.addTab( "Map", mMapPanel );

    	/**
    	 * Change listener to enable/disable the channel spectrum display
    	 * only when the tab is visible, and a channel has been selected
    	 */
    	mTabbedPane.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent event )
			{
				int index = mTabbedPane.getSelectedIndex();
				
				Component component = mTabbedPane.getComponentAt( index );
				
				if( component instanceof ChannelSpectrumPanel )
				{
					mChannelSpectrumPanel.setEnabled( true );
				}
				else
				{
					mChannelSpectrumPanel.setEnabled( false );
				}
			}
		} );
    	
    	/* Register each of the components to receive channel events when the
    	 * channels are selected or change */
    	mChannelModel.addListener( mCallEventPanel );
    	mChannelModel.addListener( mChannelStateList );
    	mChannelModel.addListener( mChannelSpectrumPanel );
    	mChannelModel.addListener( mMessageActivityPanel );
		
		JScrollPane channelStateListScroll = new JScrollPane();
    	channelStateListScroll.getViewport().setView( mChannelStateList );
    	channelStateListScroll.setPreferredSize( new Dimension( 200, 300 ) ); 

    	JideSplitPane audioChannelListSplit = new JideSplitPane( JideSplitPane.VERTICAL_SPLIT );
    	audioChannelListSplit.setDividerSize( 5 );
    	audioChannelListSplit.add( mAudioPanel );
    	audioChannelListSplit.add( channelStateListScroll );
    	
    	JideSplitPane channelSplit = new JideSplitPane( JideSplitPane.HORIZONTAL_SPLIT );
    	channelSplit.setDividerSize( 5 );
    	channelSplit.add( audioChannelListSplit );
    	channelSplit.add( mTabbedPane );
    	
    	add( channelSplit );
	}

	public ConfigurationControllerModel getController()
	{
		return mController;
	}
}
