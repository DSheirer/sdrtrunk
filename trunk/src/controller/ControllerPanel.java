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
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import map.MapPanel;
import message.Message;
import net.miginfocom.swing.MigLayout;
import sample.Listener;

import com.jidesoft.swing.JideSplitPane;
import com.jidesoft.swing.JideTabbedPane;

import controller.activity.CallEventPanel;
import controller.activity.MessageActivityPanel;
import controller.state.ChannelStateList;

public class ControllerPanel extends JPanel
{
    private static final long serialVersionUID = 1L;

    private ChannelStateList mChannelStateList;

    private CallEventPanel mCallEventPanel = new CallEventPanel();
    
    private MessageActivityPanel mMessageActivityPanel = 
    		new MessageActivityPanel();

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
    	
    	//Tabbed View - configuration, calls, messages, map
    	mTabbedPane = new JideTabbedPane();
    	mTabbedPane.setFont( this.getFont() );
    	mTabbedPane.setForeground( Color.BLACK );
    	mTabbedPane.addTab( "Configuration", mSystemControlSplitPane  );
    	mTabbedPane.addTab( "Calls", mCallEventPanel );
    	mTabbedPane.addTab( "Messages", mMessageActivityPanel );

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

    	/* Add message activity panel to receive channel selection events so
    	 * that we can display the correct message listings */
    	mResourceManager.getChannelManager().addListener( mMessageActivityPanel );

    	/* Add call event panel to receive channel selection events so
    	 * that we can display the correct call event listings */
    	mResourceManager.getChannelManager().addListener( mCallEventPanel );

    	/* Register ChannelStateList as a listener on the channel manager to
		 * receive channel starts and stops and deletes */
		mResourceManager.getChannelManager().addListener( mChannelStateList );
		
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
