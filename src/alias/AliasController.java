package alias;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

import net.coderazzi.filters.gui.AutoChoices;
import net.coderazzi.filters.gui.TableFilterHeader;
import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import settings.SettingsManager;

import com.jidesoft.swing.JideSplitPane;

public class AliasController extends JPanel 
				implements ActionListener, ListSelectionListener
{
	private static final long serialVersionUID = 1L;
	private final static Logger mLog = LoggerFactory.getLogger( AliasController.class );

	private AliasModel mAliasModel;
	private JTable mAliasTable;
	private TableFilterHeader mTableFilterHeader;
	private AliasEditor mEditor;
	
	private static final String NEW_ALIAS = "New";
	private static final String COPY_ALIAS = "Copy";
	private static final String DELETE_ALIAS = "Delete";
	
	private JButton mNewButton = new JButton( NEW_ALIAS );
	private JButton mCopyButton = new JButton( COPY_ALIAS );
	private JButton mDeleteButton = new JButton( DELETE_ALIAS );
	
	private IconCellRenderer mIconCellRenderer;

	public AliasController( AliasModel aliasModel, SettingsManager settingsManager )
	{
		mAliasModel = aliasModel;

    	mEditor = new AliasEditor( mAliasModel, settingsManager );
    	mAliasModel.addAliasEventListener( mEditor );

    	mIconCellRenderer = new IconCellRenderer( settingsManager );
    	
    	init();
	}
	
	private void init()
	{
    	setLayout( new MigLayout( "insets 0 0 0 0", 
								  "[grow,fill]", 
								  "[grow,fill]") );

		//System Configuration View and Editor
    	mAliasTable = new JTable( mAliasModel );
    	mAliasTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
    	mAliasTable.getSelectionModel().addListSelectionListener( this );
    	mAliasTable.setAutoCreateRowSorter( true );
    	
    	mAliasTable.getColumnModel().getColumn( AliasModel.COLUMN_COLOR )
			.setCellRenderer( new ColorCellRenderer() );
    	
    	mAliasTable.getColumnModel().getColumn( AliasModel.COLUMN_ICON )
			.setCellRenderer( mIconCellRenderer );

    	mTableFilterHeader = new TableFilterHeader( mAliasTable, AutoChoices.ENABLED );
    	mTableFilterHeader.setFilterOnUpdates( true );
    	
    	JScrollPane tableScroller = new JScrollPane( mAliasTable );

    	JPanel buttonsPanel = new JPanel();
    	
    	buttonsPanel.setLayout( 
			new MigLayout( "insets 0 0 0 0", "[grow,fill][grow,fill][grow,fill]", "[]") );

    	mNewButton.addActionListener( this );
    	mNewButton.setToolTipText( "Adds a new alias" );
    	buttonsPanel.add( mNewButton );
    	
    	mCopyButton.addActionListener( this );
    	mCopyButton.setEnabled( false );
    	mCopyButton.setToolTipText( "Creates a copy of the currently selected alias and adds it" );
    	buttonsPanel.add( mCopyButton );

    	mDeleteButton.addActionListener( this );
    	mDeleteButton.setEnabled( false );
    	mDeleteButton.setToolTipText( "Deletes the currently selected alias" );
    	buttonsPanel.add( mDeleteButton );
    	
    	JPanel listAndButtonsPanel = new JPanel();

    	listAndButtonsPanel.setLayout( 
			new MigLayout( "insets 0 0 0 0", "[grow,fill]", "[grow,fill][]") );

    	listAndButtonsPanel.add( tableScroller, "wrap" );
    	listAndButtonsPanel.add( buttonsPanel );
		
		JideSplitPane splitPane = new JideSplitPane( JideSplitPane.HORIZONTAL_SPLIT );
		splitPane.setDividerSize( 5 );
		splitPane.setShowGripper( true );
		splitPane.add( listAndButtonsPanel );
		splitPane.add( mEditor );

		add( splitPane );
	}

	@Override
	public void valueChanged( ListSelectionEvent event )
	{
		//This limits event firing to only when selection is complete 
		if( !event.getValueIsAdjusting() )
		{
			int selectedRow = mAliasTable.getSelectedRow();
			
			if( selectedRow != -1 )
			{
				int index = mAliasTable.convertRowIndexToModel( mAliasTable.getSelectedRow() );
				
				Alias alias = mAliasModel.getAliasAtIndex( index );
				
				if( alias != null )
				{
					mEditor.setAlias( alias );
					mCopyButton.setEnabled( true );
					mDeleteButton.setEnabled( true );
				}
			}
			else
			{
				mEditor.setAlias( null );
				mCopyButton.setEnabled( false );
				mDeleteButton.setEnabled( false );
			}
		}
	}

	/**
	 * Adds the alias to the alias table/model and scrolls the view it
	 */
	private void addAlias( Alias alias )
	{
		//HACK: when inserting a row to the model, the JTable gets
		//notified and attempts to tell the coderazzi table filter 
		//adaptive choices filter to refresh before the table filter is 
		//notified of the row additions, causing an index out of bounds 
		//exception.  We turn off adaptive choices temporarily, add the
		//channel, and turn on adaptive choices again.
		mTableFilterHeader.setAdaptiveChoices( false );
		
		int index = mAliasModel.addAlias( alias );

		mTableFilterHeader.setAdaptiveChoices( true );

		if( index >= 0 )
		{
			int translatedIndex = mAliasTable.convertRowIndexToView( index );
			mAliasTable.setRowSelectionInterval( translatedIndex, translatedIndex );
			mAliasTable.scrollRectToVisible( 
				new Rectangle( mAliasTable.getCellRect( translatedIndex, 0, true ) ) );
		}
	}
	
	private Alias getSelectedAlias()
	{
		int index = mAliasTable.getSelectedRow();
		
		if( index >= 0 )
		{
			return mAliasModel.getAliasAtIndex( 
				mAliasTable.convertRowIndexToModel( index ) );
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
			case NEW_ALIAS:
				addAlias( new Alias( "New Alias" ) );
				break;
			case COPY_ALIAS:
				Alias selected = getSelectedAlias();
				
				if( selected != null )
				{
					addAlias( AliasFactory.copyOf( selected ) );
				}
				break;
			case DELETE_ALIAS:
				Alias toDelete = getSelectedAlias();

				if( toDelete != null )
				{
					int choice = JOptionPane.showConfirmDialog( AliasController.this, 
						"Do you want to delete this alias?", "Delete Alias?", 
						JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE );
					
					if( choice == JOptionPane.YES_OPTION )
					{
						mAliasModel.removeAlias( toDelete );
					}
				}
				break;
			default:
				break;
		}
	}

	/**
	 * Colorizes the cell based on the cell's integer value
	 */
	public class ColorCellRenderer extends DefaultTableCellRenderer
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent( JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column )
		{
			JLabel label = (JLabel)super.getTableCellRendererComponent( table, 
					value, isSelected, hasFocus, row, column );

			label.setBackground( new Color( (int)value ) );
			
			label.setText( "" );
			
			return label;
		}
	}
	
	/**
	 * Displays cell's icon and name
	 */
	public class IconCellRenderer extends DefaultTableCellRenderer
	{
		private static final long serialVersionUID = 1L;
		
		private SettingsManager mSettingsManager;
		
		public IconCellRenderer( SettingsManager settingsManager )
		{
			mSettingsManager = settingsManager;
		}

		@Override
		public Component getTableCellRendererComponent( JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column )
		{
			JLabel label = (JLabel)super.getTableCellRendererComponent( table, 
					value, isSelected, hasFocus, row, column );

			ImageIcon icon = mSettingsManager.getImageIcon( label.getText(), 12 );

			if( icon != null )
			{
				label.setIcon( icon );
			}
			
			return label;
		}
	}
}
