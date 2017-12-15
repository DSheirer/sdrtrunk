/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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
package ua.in.smartjava.controller.channel.map;

import ua.in.smartjava.gui.editor.DocumentListenerEditor;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.text.InternationalFormatter;

import net.miginfocom.swing.MigLayout;
import ua.in.smartjava.controller.channel.map.ChannelMapEvent.Event;
import ua.in.smartjava.controller.channel.map.ChannelRangeModel.ChannelRangeEventListener;

public class ChannelMapEditor extends DocumentListenerEditor<ChannelMap>
		implements ListSelectionListener, ChannelRangeEventListener
{
    private static final long serialVersionUID = 1L;

    private JLabel mNameLabel;
    private JTextField mNameText;
    private ChannelRangeModel mRangeModel;
    private JTable mRangeTable;
    private JButton mNewButton;
    private JButton mDeleteButton;
    private JButton mSaveButton;
    private JLabel mRangeDescription;
    private ChannelMapModel mChannelMapModel;

	public ChannelMapEditor( ChannelMapModel channelMapModel )
	{
		mChannelMapModel = channelMapModel;
		init();
	}
	
	public ChannelMap getChannelMap()
	{
		if( hasItem() )
		{
			return (ChannelMap)getItem();
		}
		
		return null;
	}
	
	@Override
	public void setItem( ChannelMap item )
	{
		super.setItem( item );
		
		if( hasItem() )
		{
			ChannelMap map = getChannelMap();
			
			mNameText.setText( map.getName() );
			
			mRangeModel.clear();
			mRangeModel.addRanges( map.getRanges() );
			
			mNewButton.setEnabled( true );
		}
		else
		{
			mNameText.setText( "" );
			
			mRangeModel.clear();
			
			mNewButton.setEnabled( false );
		}
		
		setModified( false );
	}

	@Override
	public void setModified( boolean modified )
	{
		super.setModified( modified );

		mSaveButton.setEnabled( modified );
	}

	private void init()
	{
		setLayout( new MigLayout( "fill,wrap 3", 
				"[grow,fill][grow,fill][grow,fill]", "[][grow][][]" ) );
		
		mNameLabel = new JLabel( "Channel Map:" );
		add( mNameLabel );
		
		mNameText = new JTextField();
		mNameText.getDocument().addDocumentListener( this );
		add( mNameText, "span" );

		mRangeModel = new ChannelRangeModel();
		mRangeModel.setListener( this );
		
		mRangeTable = new JTable( mRangeModel );
		mRangeTable.setPreferredScrollableViewportSize( new Dimension( 300, 150 ) );
		mRangeTable.setFillsViewportHeight( true );
		mRangeTable.setAutoCreateRowSorter( true );
		for( int x = 0; x < mRangeTable.getColumnCount(); x++ )
		{
			mRangeTable.getColumnModel().getColumn( x )
				.setCellEditor( new RangeTableEditor() );
		}
		
		mRangeTable.getSelectionModel().addListSelectionListener( new ListSelectionListener()
		{
			@Override
			public void valueChanged( ListSelectionEvent event )
			{
				if( !event.getValueIsAdjusting() )
				{
					int index = event.getFirstIndex();
					
					mDeleteButton.setEnabled( index >= 0 );

					if( index >= 0 )
					{
						int modelIndex = mRangeTable.convertRowIndexToModel( index );
						
						if( modelIndex >= 0 )
						{
							ChannelRange selected = mRangeModel.getChannelRange( modelIndex );
							
							if( selected != null )
							{
								mRangeDescription.setText( selected.getDescription() );
							}
							else
							{
								mRangeDescription.setText( "" );
							}
						}
					}
				}
			}
		} );

		JScrollPane scrollPane = new JScrollPane( mRangeTable );
		add( scrollPane, "span,grow,push");
		
		mRangeDescription = new JLabel( " " );
		add( mRangeDescription, "span" );
		
		mSaveButton = new JButton( "Save" );
		mSaveButton.setEnabled( false );
		mSaveButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				save();
			}
		} );
		add( mSaveButton );

		mNewButton = new JButton( "New Range" );
		mNewButton.setEnabled( false );
		mNewButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				mRangeModel.addRange( new ChannelRange() );
			}
		} );
		add( mNewButton );

		mDeleteButton = new JButton( "DeleteRange" );
		mDeleteButton.setEnabled( false );
		mDeleteButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				int selectedRow = mRangeTable.getSelectedRow();
				
				if( selectedRow >= 0 )
				{
					int modelRow = mRangeTable.convertRowIndexToModel( selectedRow );

					ChannelRange selectedRange = mRangeModel.getChannelRange( modelRow );
					
					if( selectedRange != null )
					{
						int choice = JOptionPane.showConfirmDialog( mRangeTable, 
								"Do you want to delete this ua.in.smartjava.channel range?",
								"Delete Channel Range?", JOptionPane.YES_NO_OPTION );
							
						if( choice == JOptionPane.YES_OPTION )
						{
							mRangeModel.removeRange( selectedRange );
							mDeleteButton.setEnabled( false );
						}
					}
				}
			}
		} );
		add( mDeleteButton, "wrap" );
	}
	
	@Override
	public void save()
	{
		if( isModified() && hasItem() )
		{
			ChannelMap channelMap = getChannelMap();
			
			channelMap.setName( mNameText.getText() );
			channelMap.setRanges( mRangeModel.getChannelRanges() );

			mChannelMapModel.broadcast( new ChannelMapEvent( getChannelMap(), Event.CHANGE ) );
			setModified( false );
		}
	}

	@Override
	public void valueChanged( ListSelectionEvent e )
	{
		if( !e.getValueIsAdjusting() )
		{
			if( e.getSource() instanceof JList )
			{
				JList<?> list = (JList<?>)e.getSource();

				if( list.getSelectedValue() instanceof ChannelMap )
				{
					setItem( (ChannelMap)list.getSelectedValue() );
				}
				else
				{
					setItem( null );
				}
			}
		}
	}

	@Override
	public void channelRangesChanged()
	{
		setModified( true );

		//Update description
		int index = mRangeTable.getSelectedRow();
		
		if( index >= 0 )
		{
			ChannelRange selected = mRangeModel.getChannelRange( 
					mRangeTable.convertRowIndexToModel( index ) );
			
			if( selected != null )
			{
				mRangeDescription.setText( selected.getDescription() );
			}
			else
			{
				mRangeDescription.setText( "" );
			}
		}
	}
	
	public class RangeTableEditor extends AbstractCellEditor
	  implements TableCellEditor, ActionListener
	{
		private static final long serialVersionUID = 1L;
		
		JFormattedTextField mEditor;
		
		public RangeTableEditor()
		{
			InternationalFormatter nf = new InternationalFormatter();
			nf.setMinimum( 1 );
			nf.setAllowsInvalid( true );
			
			mEditor = new JFormattedTextField( nf );
		}
		
		@Override
		public Object getCellEditorValue()
		{
			return mEditor.getText();
		}
		
		@Override
		public void actionPerformed( ActionEvent e )
		{
		}
		
		@Override
		public Component getTableCellEditorComponent( JTable table, 
								  Object value,
								  boolean isSelected, 
								  int row, 
								  int column )
		{
			mEditor.setValue( value );
			
			return mEditor;
		}
	}
	
}
