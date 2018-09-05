/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.controller.channel;

import com.jidesoft.swing.JideSplitPane;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.controller.channel.map.ChannelMapModel;
import io.github.dsheirer.source.SourceManager;
import net.coderazzi.filters.gui.AutoChoices;
import net.coderazzi.filters.gui.TableFilterHeader;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChannelController extends JPanel implements ActionListener, ListSelectionListener
{
	private static final long serialVersionUID = 1L;

	private ChannelModel mChannelModel;
	private JTable mChannelTable;
	private TableFilterHeader mTableFilterHeader;
	private ChannelEditor mEditor;
	
	private static final String NEW_CHANNEL = "New";
	private static final String COPY_CHANNEL = "Copy";
	private static final String DELETE_CHANNEL = "Delete";
	
	private JButton mNewChannelButton = new JButton( NEW_CHANNEL );
	private JButton mCopyChannelButton = new JButton( COPY_CHANNEL );
	private JButton mDeleteChannelButton = new JButton( DELETE_CHANNEL );

	public ChannelController( ChannelModel channelModel,
							  ChannelMapModel channelMapModel,
							  SourceManager sourceManager,
							  AliasModel aliasModel )
	{
		mChannelModel = channelModel;

    	mEditor = new ChannelEditor( channelModel, channelMapModel, sourceManager, aliasModel );
    	mChannelModel.addListener( mEditor );

    	init();
	}
	
	private void init()
	{
    	setLayout( new MigLayout( "", "[grow,fill]", "[grow,fill]") );

		//System Configuration View and Editor
    	mChannelTable = new JTable( mChannelModel );
    	mChannelTable.setDefaultRenderer( String.class, new ChannelTableCellRenderer() );
    	mChannelTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
    	mChannelTable.getSelectionModel().addListSelectionListener( this );
    	mChannelTable.setAutoCreateRowSorter( true );
    	
    	mTableFilterHeader = new TableFilterHeader( mChannelTable, AutoChoices.ENABLED );
    	mTableFilterHeader.setFilterOnUpdates( true );
    	
    	JScrollPane channelScroller = new JScrollPane( mChannelTable );

    	JPanel buttonsPanel = new JPanel();
    	
    	buttonsPanel.setLayout( 
			new MigLayout( "insets 0 0 0 0", "[grow,fill][grow,fill][grow,fill]", "[]") );

    	mNewChannelButton.addActionListener( this );
    	mNewChannelButton.setToolTipText( "Create a new channel configuration" );
    	buttonsPanel.add( mNewChannelButton );
    	
    	mCopyChannelButton.addActionListener( this );
    	mCopyChannelButton.setEnabled( false );
    	mCopyChannelButton.setToolTipText( "Create a copy of the currently selected channel configuration" );
    	buttonsPanel.add( mCopyChannelButton );

    	mDeleteChannelButton.addActionListener( this );
    	mDeleteChannelButton.setEnabled( false );
    	mDeleteChannelButton.setToolTipText( "Delete the currently selected channel configuration" );
    	buttonsPanel.add( mDeleteChannelButton );
    	
    	JPanel listAndButtonsPanel = new JPanel();

    	listAndButtonsPanel.setLayout( 
			new MigLayout( "insets 0 0 0 0", "[grow,fill]", "[grow,fill][]") );

    	listAndButtonsPanel.add( channelScroller, "wrap" );
    	listAndButtonsPanel.add( buttonsPanel );
		
		JideSplitPane splitPane = new JideSplitPane( JideSplitPane.VERTICAL_SPLIT );
		splitPane.setDividerSize( 5 );
		splitPane.setShowGripper( true );

		//Shrink this guy so that he'll eventually be expanded to the scroller preferred size
		listAndButtonsPanel.setPreferredSize( new Dimension( 10, 10 ) );

		//Attempt to get a 60:40 vertical sizing preference
		JScrollPane listScroller = new JScrollPane( listAndButtonsPanel );
		listScroller.setPreferredSize( new Dimension( 800, 58 ) );
		
		JScrollPane editorScroller = new JScrollPane( mEditor );
		editorScroller.setPreferredSize( new Dimension( 800, 42 ) );

		splitPane.add( listScroller );
		splitPane.add( editorScroller );

		add( splitPane );
	}

	@Override
	public void valueChanged( ListSelectionEvent event )
	{
		//This limits event firing to only when selection is complete 
		if( !event.getValueIsAdjusting() )
		{
			int selectedRow = mChannelTable.getSelectedRow();
			
			if( selectedRow != -1 )
			{
				int index = mChannelTable.convertRowIndexToModel( mChannelTable.getSelectedRow() );
				
				Channel channel = mChannelModel.getChannelAtIndex( index );
				
				if( channel != null )
				{
					mEditor.setItem( channel );
					mCopyChannelButton.setEnabled( true );
					mDeleteChannelButton.setEnabled( true );
				}
			}
			else
			{
				mEditor.setItem( null );
				mCopyChannelButton.setEnabled( false );
				mDeleteChannelButton.setEnabled( false );
			}
		}
	}

	/**
	 * Adds the channel to the channel model/table and scrolls to view it
	 */
	private void addChannel( Channel channel )
	{
		//HACK: when inserting a row to the model, the JTable gets
		//notified and attempts to tell the coderazzi table filter
		//adaptive choices filter to refresh before the table filter is
		//notified of the row additions, causing an index out of bounds 
		//exception.  We turn off adaptive choices temporarily, add the
		//channel, and turn on adaptive choices again.
		mTableFilterHeader.setAdaptiveChoices( false );
		
		int index = mChannelModel.addChannel( channel );

		mTableFilterHeader.setAdaptiveChoices( true );

		if( index >= 0 )
		{
			int translatedIndex = mChannelTable.convertRowIndexToView( index );

			if(translatedIndex >= 0)
			{
				mChannelTable.setRowSelectionInterval( translatedIndex, translatedIndex );
				mChannelTable.scrollRectToVisible(
					new Rectangle( mChannelTable.getCellRect( translatedIndex, 0, true ) ) );
			}
		}
	}
	
	private Channel getSelectedChannel()
	{
		int index = mChannelTable.getSelectedRow();
		
		if( index >= 0 )
		{
			return mChannelModel.getChannelAtIndex( 
				mChannelTable.convertRowIndexToModel( index ) );
		}
		
		return null;
	}

	/**
	 * Responds to New, Copy and Delete Channel button invocations
	 */
	@Override
	public void actionPerformed( ActionEvent event )
	{
		switch( event.getActionCommand() )
		{
			case NEW_CHANNEL:
				addChannel( new Channel( "New Channel" ) );
				break;
			case COPY_CHANNEL:
				Channel selected = getSelectedChannel();
				
				if( selected != null )
				{
					addChannel( selected.copyOf() );
				}
				break;
			case DELETE_CHANNEL:
				Channel toDelete = getSelectedChannel();

				if( toDelete != null )
				{
					int choice = JOptionPane.showConfirmDialog( ChannelController.this, 
						"Do you want to delete this channel?", "Delete Channel?",
						JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE );
					
					if( choice == JOptionPane.YES_OPTION )
					{
						mChannelModel.removeChannel( toDelete );
					}
				}
				break;
			default:
				break;
		}
	}
	
	public class ChannelTableCellRenderer extends DefaultTableCellRenderer
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent( JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column )
		{
			Component component = super.getTableCellRendererComponent( 
					table, value, isSelected, hasFocus, row, column );

			ChannelModel model = (ChannelModel)table.getModel();

			int index = table.convertRowIndexToModel( row );
			
			Channel channel = model.getChannelAtIndex( index );
			
			boolean enabled = channel.isProcessing();
			
			if( isSelected )
			{
				if( enabled )
				{
					component.setBackground( Color.YELLOW );
				}
				else
				{
					component.setBackground( table.getSelectionBackground() );
				}
			}
			else
			{
				if( enabled )
				{
					component.setBackground( Color.GREEN );
				}
				else
				{
					component.setBackground( table.getBackground() );
				}
			}
			
			return component;
		}
		
	}
}
