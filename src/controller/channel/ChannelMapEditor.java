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
package controller.channel;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;
import javax.swing.text.InternationalFormatter;

import log.Log;
import net.miginfocom.swing.MigLayout;

public class ChannelMapEditor extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    private ChannelMapNode mChannelMapNode;
    
    private JLabel mNameLabel;
    private JTextField mNameText;
    private ChannelMapModel mRangeModel;
    private JTable mRangeTable;
    private JButton mDelete;
    private ChannelMap mChannelMapCopy;

	public ChannelMapEditor( ChannelMapNode ChannelMap )
	{
		mChannelMapNode = ChannelMap;
		mChannelMapCopy = mChannelMapNode.getChannelMap().copyOf();
		initGUI();
	}
	
	private void initGUI()
	{
		setLayout( new MigLayout( "fill,wrap 2", "[right][grow]", "[][][grow][][]" ) );
		
		add( new JLabel( "Channel Map" ), "span, align center" );
		
		mNameLabel = new JLabel( "Name:" );
		add( mNameLabel );
		
		mNameText = new JTextField( mChannelMapCopy.getName() );
		mNameText.addFocusListener( new FocusListener() 
		{
			@Override public void focusGained( FocusEvent arg0 ) {}

			@Override
            public void focusLost( FocusEvent arg0 )
            {
				Log.info( "text field action event fired, new value:" + mNameText.getText() );
				mChannelMapCopy.setName( mNameText.getText() );
            }
		} );
		add( mNameText, "growx, push" );

		mRangeModel = new ChannelMapModel( mChannelMapCopy );
		
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
		btnNew.addActionListener( ChannelMapEditor.this );
		add( btnNew );

		mDelete = new JButton( "Delete" );
		mDelete.addActionListener( ChannelMapEditor.this );
		add( mDelete, "wrap" );

		JButton btnSave = new JButton( "Save" );
		btnSave.addActionListener( ChannelMapEditor.this );
		add( btnSave, "growx" );

		JButton btnReset = new JButton( "Reset " );
		btnReset.addActionListener( ChannelMapEditor.this );
		add( btnReset, "wrap" );
	}
	
	private boolean checkForValidRanges()
	{
		int invalidRow = mChannelMapCopy.getInvalidRange();

		if( invalidRow != -1 && invalidRow <= mRangeTable.getRowCount() )
		{
			mRangeTable.setRowSelectionInterval( invalidRow, invalidRow );
			
			JOptionPane.showMessageDialog( this,
					"Starting channel number must be less than ending channel number",
				    "Channel number range error",
				    JOptionPane.WARNING_MESSAGE );
			
			return false;
		}
		
		return true;
	}
	
	@Override
    public void actionPerformed( ActionEvent e )
    {
		String command = e.getActionCommand();

		switch( command )
		{
			case "New Range":
				mRangeModel.addRange( new ChannelRange() );
				break;
			case "Save":
				if( checkForValidRanges() )
				{
					mChannelMapNode.getChannelMap()
								.setName( mChannelMapCopy.getName() );
					mChannelMapNode.getChannelMap()
								.setRanges( mChannelMapCopy.getRanges() );
					
					mChannelMapNode.save();
					
					mChannelMapNode.refresh();
					
					mChannelMapNode.show();
				}
			case "Reset":
				mChannelMapCopy = mChannelMapNode.getChannelMap();
				mRangeModel.setChannelMap( mChannelMapCopy );
				mNameText.setText( mChannelMapCopy.getName() );
				break;
			case "Delete":
				int row = mRangeTable.getSelectedRow();
				
				if( row < 0 )
				{
					JOptionPane.showMessageDialog( this,
							"Please select a channel range to delete",
						    "Select a range",
						    JOptionPane.WARNING_MESSAGE );
				}
				else
				{
					mRangeModel.removeRange( 
							mRangeTable.convertRowIndexToModel( row ) );
				}
				break;
		}
    }
	
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
			Log.info( "getCellEditorValue - I've been invoked!" );
	        return mEditor.getText();
        }

		@Override
        public void actionPerformed( ActionEvent e )
        {
			Log.info( "Action Performed:" + e.getActionCommand() );
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
