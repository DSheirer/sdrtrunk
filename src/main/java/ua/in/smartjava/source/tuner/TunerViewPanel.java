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

package ua.in.smartjava.source.tuner;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.in.smartjava.source.tuner.TunerEvent.Event;

import com.jidesoft.swing.JideSplitPane;

public class TunerViewPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	private final static Logger mLog = LoggerFactory.getLogger( TunerViewPanel.class );

	private TunerModel mTunerModel;
	private JTable mTunerTable;
	private TableRowSorter<TunerModel> mRowSorter;
	private JideSplitPane mSplitPane;
	private TunerEditor mTunerEditor;
	
	public TunerViewPanel( TunerModel tunerModel )
	{
		mTunerModel = tunerModel;
		mTunerEditor = new TunerEditor( mTunerModel.getTunerConfigurationModel() );
		
		init();
	}
	
	private void init()
	{
		setLayout( new MigLayout( "insets 0 0 0 0", "[fill,grow]", "[fill,grow]" ) );

		mRowSorter = new TableRowSorter<>( mTunerModel );
		List<RowSorter.SortKey> sortKeys = new ArrayList<>();
		sortKeys.add( new RowSorter.SortKey( TunerModel.TUNER_TYPE, SortOrder.ASCENDING ) );
		sortKeys.add( new RowSorter.SortKey( TunerModel.TUNER_ID, SortOrder.ASCENDING ) );
		mRowSorter.setSortKeys( sortKeys );
		
		mTunerTable = new JTable( mTunerModel );
		mTunerTable.setRowSorter( mRowSorter );
		mTunerTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		mTunerTable.getSelectionModel().addListSelectionListener( new ListSelectionListener()
		{
			@Override
			public void valueChanged( ListSelectionEvent event )
			{
				if( !event.getValueIsAdjusting() )
				{
					int row = mTunerTable.getSelectedRow();
					int modelRow = mTunerTable.convertRowIndexToModel( row );

					mTunerEditor.setItem( mTunerModel.getTuner( modelRow ) );
				}
			}
		} );
		mTunerTable.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseClicked( MouseEvent e )
			{
				int column = mTunerTable.columnAtPoint( e.getPoint() );

				if( column == TunerModel.SPECTRAL_DISPLAY_MAIN )
				{
					int tableRow = mTunerTable.rowAtPoint( e.getPoint() );
					int modelRow = mTunerTable.convertRowIndexToModel( tableRow );
					
					Tuner tuner = mTunerModel.getTuner( modelRow );
					
					if( tuner != null )
					{
						mTunerModel.broadcast( new TunerEvent( tuner, 
								Event.REQUEST_MAIN_SPECTRAL_DISPLAY ) );
					}
				}
				else if( column == TunerModel.SPECTRAL_DISPLAY_NEW )
				{
					int tableRow = mTunerTable.rowAtPoint( e.getPoint() );
					int modelRow = mTunerTable.convertRowIndexToModel( tableRow );
					
					Tuner tuner = mTunerModel.getTuner( modelRow );
					
					if( tuner != null )
					{
						mTunerModel.broadcast( new TunerEvent( tuner, 
								Event.REQUEST_NEW_SPECTRAL_DISPLAY ) );
					}
				}
			}
		} );
		
		TableCellRenderer renderer = new LinkCellRenderer();
		
		mTunerTable.getColumnModel().getColumn( 5 ).setCellRenderer( renderer );
		mTunerTable.getColumnModel().getColumn( 6 ).setCellRenderer( renderer );
		
		JScrollPane listScroller = new JScrollPane( mTunerTable );
		listScroller.setPreferredSize( new Dimension( 400, 20 ) );

		JScrollPane editorScroller = new JScrollPane( mTunerEditor );
		editorScroller.setPreferredSize( new Dimension( 400, 80 ) );
		
		mSplitPane = new JideSplitPane();
		mSplitPane.setOrientation( JideSplitPane.VERTICAL_SPLIT );
		mSplitPane.add( listScroller );
		mSplitPane.add( editorScroller );
		
		add( mSplitPane );
	}
	
	public class LinkCellRenderer extends DefaultTableCellRenderer
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent( JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column )
		{
			JLabel label = (JLabel)super.getTableCellRendererComponent( table, 
				value, isSelected, hasFocus, row, column );
			
			label.setForeground( Color.BLUE.brighter() );
			
			if( column == TunerModel.SPECTRAL_DISPLAY_MAIN )
			{
				label.setToolTipText( "Show this tuner in the main spectral display" );
			}
			else if( column == TunerModel.SPECTRAL_DISPLAY_NEW )
			{
				label.setToolTipText( "Show this tuner in a new spectral display" );
			}
			
			return label;
		}
	}
}
