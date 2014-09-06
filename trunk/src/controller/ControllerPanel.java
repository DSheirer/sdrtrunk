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
import message.Message;
import net.miginfocom.swing.MigLayout;
import sample.Listener;
import spectrum.ChannelSpectrumPanel;

import com.jidesoft.swing.JideSplitPane;
import com.jidesoft.swing.JideTabbedPane;

import controller.activity.CallEventPanel;
import controller.activity.MessageActivityPanel;
import controller.channel.ChannelManager;
import controller.state.ChannelStateList;

public class ControllerPanel extends JPanel
{
    private static final long serialVersionUID = 1L;

    private ChannelStateList mChannelStateList;

    private CallEventPanel mCallEventPanel = new CallEventPanel();
    
    private MessageActivityPanel mMessageActivityPanel = 
    		new MessageActivityPanel();
    
    private ChannelSpectrumPanel mChannelSpectrumPanel;
    
    private JideTabbedPane mTabbedPane;

    protected ConfigurationTreePanel mSystemControlViewPanel;
	protected ConfigurationEditor mConfigurationEditor;
    protected JideSplitPane mSystemControlSplitPane;

	protected JTable mChannelActivityTable = new JTable();
	protected ResourceManager mResourceManager;

	public ControllerPanel( ResourceManager resourceManager )
	{
	    mResourceManager = resourceManager;
		init();
	}
	
	private void init()
	{
    	setLayout( new MigLayout( "insets 0 0 0 0 ", 
    							  "[grow,fill]", 
    							  "[grow,fill]") );
    	
    	//System Configuration View and Editor
    	mConfigurationEditor = new ConfigurationEditor();
    	mSystemControlViewPanel = 
    			new ConfigurationTreePanel( mResourceManager.getController() );
    	mSystemControlViewPanel.addTreeSelectionListener( mConfigurationEditor );

    	mSystemControlSplitPane = new JideSplitPane( JideSplitPane.HORIZONTAL_SPLIT );
    	mSystemControlSplitPane.setDividerSize( 5 );
    	mSystemControlSplitPane.add( mSystemControlViewPanel );
    	mSystemControlSplitPane.add( mConfigurationEditor );
    	
    	mChannelSpectrumPanel = new ChannelSpectrumPanel( mResourceManager );
    	
    	//Tabbed View - configuration, calls, messages, map
    	mTabbedPane = new JideTabbedPane();
    	mTabbedPane.setFont( this.getFont() );
    	mTabbedPane.setForeground( Color.BLACK );
    	mTabbedPane.addTab( "Configuration", mSystemControlSplitPane  );
    	mTabbedPane.addTab( "Channel Spectrum", mChannelSpectrumPanel );
    	mTabbedPane.addTab( "Events", mCallEventPanel );
    	mTabbedPane.addTab( "Messages", mMessageActivityPanel );

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

    	/**
    	 * Add mapping services and map panel to a new tab
    	 */

    	/* Add Map Service as message listener to receive all messages */
    	mResourceManager.getChannelManager().addListener( 
    			(Listener<Message>)mResourceManager.getMapService() );
    	
    	MapPanel mapPanel = new MapPanel( mResourceManager );
    	mTabbedPane.addTab( "Map", mapPanel );

    	
		/* Channel state list */
    	mChannelStateList = new ChannelStateList( mResourceManager.getSettingsManager() );

    	/* Register each of the components to receive channel events when the
    	 * channels are selected or change */
    	ChannelManager channelManager = mResourceManager.getChannelManager();
    	channelManager.addListener( mCallEventPanel );
    	channelManager.addListener( mChannelStateList );
    	channelManager.addListener( mChannelSpectrumPanel );
    	channelManager.addListener( mMessageActivityPanel );
		
		JScrollPane channelStateListScroll = new JScrollPane();
    	channelStateListScroll.getViewport().setView( mChannelStateList );
    	channelStateListScroll.setPreferredSize( new Dimension( 150, 
    			(int)channelStateListScroll.getPreferredSize().getHeight() ) );

    	JideSplitPane channelSplit = new JideSplitPane( JideSplitPane.HORIZONTAL_SPLIT );
    	channelSplit.setDividerSize( 5 );
    	channelSplit.add( channelStateListScroll );
    	channelSplit.add( mTabbedPane );
    	
    	add( channelSplit );
	}

	public ConfigurationControllerModel getController()
	{
		return mResourceManager.getController();
	}
}
