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
package controller.channel.map;

import gui.editor.DocumentListenerEditor;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.text.InternationalFormatter;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelMapEditor extends DocumentListenerEditor<ChannelMap>
			implements ListSelectionListener
{
    private static final long serialVersionUID = 1L;
	private final static Logger mLog =
			LoggerFactory.getLogger( ChannelMapEditor.class );

    private JLabel mNameLabel;
    private JTextField mNameText;
    private ChannelRangeModel mRangeModel;
    private JTable mRangeTable;
    private JButton mDelete;

	public ChannelMapEditor()
	{
		initGUI();
	}
	
	private ChannelMap getChannelMap()
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
			
		}
	}

	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][grow]", "[][][grow][][]" ) );
		
		add( new JLabel( "Channel Map" ), "span, align center" );
		
		mNameLabel = new JLabel( "Name:" );
		add( mNameLabel );
		
		mNameText = new JTextField();
		mNameText.getDocument().addDocumentListener( this );
		add( mNameText, "growx, push" );

		mRangeModel = new ChannelRangeModel();
		
		mRangeTable = new JTable( mRangeModel );
		mRangeTable.setPreferredScrollableViewportSize( new Dimension( 300, 150 ) );
		mRangeTable.setFillsViewportHeight( true );
		mRangeTable.setAutoCreateRowSorter( true );
		for( int x = 0; x < mRangeTable.getColumnCount(); x++ )
		{
			mRangeTable.getColumnModel().getColumn( x )
				.setCellEditor( new RangeTableEditor() );
		}

		JScrollPane scrollPane = new JScrollPane( mRangeTable );
		add( scrollPane, "span,grow,push");
		
		JButton btnNew = new JButton( "New Range" );
//		btnNew.addActionListener( ChannelMapEditor.this );
		add( btnNew );

		mDelete = new JButton( "Delete" );
//		mDelete.addActionListener( ChannelMapEditor.this );
		add( mDelete, "wrap" );

		JButton btnSave = new JButton( "Save" );
//		btnSave.addActionListener( ChannelMapEditor.this );
		add( btnSave, "growx" );

		JButton btnReset = new JButton( "Reset " );
//		btnReset.addActionListener( ChannelMapEditor.this );
		add( btnReset, "wrap" );
	}
	
	private boolean checkForValidRanges()
	{
		mLog.error( "Fix Me!" );
//		int invalidRow = mChannelMapCopy.getInvalidRange();
//
//		if( invalidRow != -1 && invalidRow <= mRangeTable.getRowCount() )
//		{
//			mRangeTable.setRowSelectionInterval( invalidRow, invalidRow );
//			
//			JOptionPane.showMessageDialog( this,
//					"Starting channel number must be less than ending channel number",
//				    "Channel number range error",
//				    JOptionPane.WARNING_MESSAGE );
//			
//			return false;
//		}
		
		return true;
	}
	
//	@Override
//    public void actionPerformed( ActionEvent e )
//    {
//		String command = e.getActionCommand();
//
//		switch( command )
//		{
//			case "New Range":
//				mRangeModel.addRange( new ChannelRange() );
//				break;
//			case "Save":
//				if( checkForValidRanges() )
//				{
//					mChannelMapNode.getChannelMap()
//								.setName( mChannelMapCopy.getName() );
//					mChannelMapNode.getChannelMap()
//								.setRanges( mChannelMapCopy.getRanges() );
//					
//					mChannelMapNode.save();
//					
//					mChannelMapNode.refresh();
//					
//					mChannelMapNode.show();
//				}
//			case "Reset":
//				mChannelMapCopy = mChannelMapNode.getChannelMap();
//				mRangeModel.setChannelMap( mChannelMapCopy );
//				mNameText.setText( mChannelMapCopy.getName() );
//				break;
//			case "Delete":
//				int row = mRangeTable.getSelectedRow();
//				
//				if( row < 0 )
//				{
//					JOptionPane.showMessageDialog( this,
//							"Please select a channel range to delete",
//						    "Select a range",
//						    JOptionPane.WARNING_MESSAGE );
//				}
//				else
//				{
//					mRangeModel.removeRange( 
//							mRangeTable.convertRowIndexToModel( row ) );
//				}
//				break;
//		}
//    }
	
	public class RangeTableEditor extends AbstractCellEditor
								  implements TableCellEditor, ActionListener
	{
		JFormattedTextField mEditor;

		public RangeTableEditor()
		{
			InternationalFormatter nf = new InternationalFormatter();
			nf.setMinimum( 1 );
			nf.setAllowsInvalid( false );
			
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

	@Override
	public void save()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void valueChanged( ListSelectionEvent e )
	{
		mLog.debug( "List Select Event from: " + e.getSource().getClass() );
	}
}
