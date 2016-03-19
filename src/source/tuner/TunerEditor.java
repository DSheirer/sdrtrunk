package source.tuner;

import gui.control.JFrequencyControl;
import gui.editor.Editor;
import gui.editor.EmptyEditor;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import source.tuner.configuration.TunerConfiguration;
import source.tuner.configuration.TunerConfigurationFactory;
import source.tuner.configuration.TunerConfigurationModel;

import com.jidesoft.swing.JideSplitPane;

public class TunerEditor extends Editor<Tuner>
{
	private static final long serialVersionUID = 1L;

	private final static Logger mLog = LoggerFactory.getLogger( TunerEditor.class );

	private TunerConfigurationModel mTunerConfigurationModel;
	private JTable mTunerConfigurationTable;
	private TableRowSorter<TunerConfigurationModel> mRowSorter;
	private JFrequencyControl mFrequencyControl = new JFrequencyControl();
	private JLabel mSelectedTunerType = new JLabel( "No Tuner Selected" );
	private JButton mNewConfigurationButton = new JButton( "New" );
	private JButton mDeleteConfigurationButton = new JButton( "Delete" );
	private JButton mAssignConfigurationButton = new JButton( "Assign" );
	private JScrollPane mEditorScroller;
	private Editor<TunerConfiguration> mEditor = new EmptyEditor<>( "a tuner" );
	private JideSplitPane mEditorSplitPane = new JideSplitPane();
	
	public TunerEditor( TunerConfigurationModel tunerConfigurationModel )
	{
		mTunerConfigurationModel = tunerConfigurationModel;
		init();
	}
	
	public void init()
	{
		setLayout( new MigLayout( "insets 0 0 0 0", "[grow,fill]", "[grow,fill]" ) );

		JPanel listPanel = new JPanel();
		listPanel.setLayout( new MigLayout( "fill,wrap 3", "[grow,fill]", "[][][][grow,fill][]" ) );

		listPanel.add( mSelectedTunerType, "span" );
		
		mFrequencyControl.setEnabled( false );
		listPanel.add( mFrequencyControl, "span" );

		mTunerConfigurationTable = new JTable( mTunerConfigurationModel );
		mRowSorter = new TableRowSorter<>( mTunerConfigurationModel );
		mTunerConfigurationTable.setRowSorter( mRowSorter );
		mTunerConfigurationTable.setEnabled( false );
		mTunerConfigurationTable.getSelectionModel().addListSelectionListener( new ListSelectionListener()
		{
			@Override
			public void valueChanged( ListSelectionEvent event )
			{
				if( !event.getValueIsAdjusting() )
				{
					int row = mTunerConfigurationTable.getSelectedRow();
					
					if( row >= 0 )
					{
						int modelRow = mTunerConfigurationTable.convertRowIndexToModel( row );

						if( modelRow >= 0 )
						{
							setTunerConfiguration( mTunerConfigurationModel.getTunerConfiguration( modelRow ) );
						}
					}
				}
			}
		} );

		JScrollPane tableScroller = new JScrollPane( mTunerConfigurationTable );
		tableScroller.setHorizontalScrollBarPolicy( 
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		listPanel.add( new JLabel( "Tuner Configurations" ), "span" );
		listPanel.add( tableScroller, "span" );

		mNewConfigurationButton.setEnabled( false );
		mNewConfigurationButton.setToolTipText( "Create a new configuration for the currently selected tuner" );
		mNewConfigurationButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				Tuner tuner = getItem();
				
				String name = mTunerConfigurationModel
					.getDistinctName( tuner.getTunerType(), tuner.getUniqueID() );
				
				TunerConfiguration config = TunerConfigurationFactory
					.getTunerConfiguration( tuner.getTunerType(), 
							tuner.getUniqueID(), name );
					
				mTunerConfigurationModel.addTunerConfiguration( config );
			}
		} );
		listPanel.add( mNewConfigurationButton );

		mDeleteConfigurationButton.setEnabled( false );
		mDeleteConfigurationButton.setToolTipText( "Deletes the currently selected tuner" );
		mDeleteConfigurationButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				int choice = JOptionPane.showConfirmDialog( TunerEditor.this, 
						"Do you want to delete this tuner configuration?", 
						"Delete Tuner Configuration?", JOptionPane.YES_NO_OPTION );
				
				if( choice == JOptionPane.YES_OPTION )
				{
					TunerConfiguration selected = getSelectedTunerConfiguration();
					
					mTunerConfigurationModel.removeTunerConfiguration( selected );
				}
			}
		} );
		listPanel.add( mDeleteConfigurationButton );
		
		mAssignConfigurationButton.setEnabled( false );
		mAssignConfigurationButton.setToolTipText( "Assigns to the currently selected tuner configuration to the selected tuner" );
		mAssignConfigurationButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				TunerConfiguration selected = getSelectedTunerConfiguration();
				
				mTunerConfigurationModel.assignTunerConfiguration( selected );
				setTunerConfiguration( getSelectedTunerConfiguration() );
			}
		} );
		listPanel.add( mAssignConfigurationButton );
		
		//Set preferred size on both scrollers to same width, so they split the space
		listPanel.setPreferredSize( new Dimension( 100, 80 ) );
		JScrollPane listScroller = new JScrollPane( listPanel );
		listScroller.setPreferredSize( new Dimension( 100, 80 ) );
		
		mEditorScroller = new JScrollPane( mEditor );
		mEditorScroller.setPreferredSize( new Dimension( 100, 80 ) );

		mEditorSplitPane = new JideSplitPane( JideSplitPane.HORIZONTAL_SPLIT );
		mEditorSplitPane.add( listScroller );
		mEditorSplitPane.add( mEditorScroller );

		add( mEditorSplitPane );
	}
	
	private TunerConfiguration getSelectedTunerConfiguration()
	{
		int tableRow = mTunerConfigurationTable.getSelectedRow();
		
		if( tableRow >= 0 )
		{
			int modelRow = mTunerConfigurationTable.convertRowIndexToModel( tableRow );
			
			if( modelRow >= 0 )
			{
				return mTunerConfigurationModel.getTunerConfiguration( modelRow );
			}
		}

		return null;
	}
	
	@Override
	public void save()
	{
		//Unused
	}

	@Override
	public void setItem( Tuner tuner )
	{
		//Cleanup previously selected tuner
		if( hasItem() )
		{
			Tuner previous = getItem();
			previous.getTunerController().removeListener( mFrequencyControl );
			mFrequencyControl.removeListener( previous.getTunerController() );
		}
		
		super.setItem( tuner );
		
		if( hasItem() )
		{
			mSelectedTunerType.setText( tuner.getName() );
			mFrequencyControl.setEnabled( true );
			mTunerConfigurationTable.setEnabled( true );
			mNewConfigurationButton.setEnabled( true );

			//Change to an editor for this config's tuner type
	        mEditor = TunerConfigurationFactory.getEditor( getItem(), mTunerConfigurationModel );
		        
			mRowSorter.setRowFilter( new ConfigurationRowFilter( tuner.getTunerType(), 
					tuner.getUniqueID() ) );

			TunerConfiguration assigned = mTunerConfigurationModel
				.getTunerConfiguration( tuner.getTunerType(), tuner.getUniqueID() );

			int modelIndex = mTunerConfigurationModel.getRowIndex( assigned );
			
			if( modelIndex >= 0 )
			{
				int viewIndex = mTunerConfigurationTable
						.convertRowIndexToView( modelIndex );
				
				if( viewIndex >= 0 )
				{
					mTunerConfigurationTable
						.setRowSelectionInterval( viewIndex, viewIndex );
				}
			}

			//Link frequency control to the tuner
	        mFrequencyControl.addListener( tuner.getTunerController() );
	        
	        //Link tuner to frequency control
	        tuner.getTunerController().addListener( mFrequencyControl );

	        //Set the displayed frequency without adjusting the tuner's frequency
	        mFrequencyControl.setFrequency( 
	        		tuner.getTunerController().getFrequency(), false );
		}
		else
		{
			mSelectedTunerType.setText( "No Tuner Selected" );
			mFrequencyControl.setEnabled( false );
			mTunerConfigurationTable.setEnabled( false );
			mNewConfigurationButton.setEnabled( false );
			mRowSorter.setRowFilter( null );
			mEditor = new EmptyEditor<TunerConfiguration>();
		}
		
		//Swap out the editor 
		int split = mEditorSplitPane.getDividerLocation( 0 );
		mEditorSplitPane.remove( mEditorScroller );
		mEditorScroller = new JScrollPane( mEditor );
		mEditorSplitPane.add( mEditorScroller );
		mEditorSplitPane.setDividerLocation( 0, split );
	}
	
	private void setTunerConfiguration( TunerConfiguration config )
	{
		if( config != null )
		{
			if( !config.isAssigned() )
			{
				if( !mAssignConfigurationButton.isEnabled() )
				{
					mAssignConfigurationButton.setEnabled( true );
				}
				if( !mDeleteConfigurationButton.isEnabled() )
				{
					mDeleteConfigurationButton.setEnabled( true );
				}
			}
			else
			{
				if( mAssignConfigurationButton.isEnabled() )
				{
					mAssignConfigurationButton.setEnabled( false );
				}
				
				if( mDeleteConfigurationButton.isEnabled() )
				{
					mDeleteConfigurationButton.setEnabled( false );
				}
			}
		}
		else
		{
			if( mDeleteConfigurationButton.isEnabled() )
			{
				mDeleteConfigurationButton.setEnabled( false );
			}
			
			if( mAssignConfigurationButton.isEnabled() )
			{
				mAssignConfigurationButton.setEnabled( false );
			}
		}
		
		mEditor.setItem( config );
	}
	
	public class ConfigurationRowFilter extends RowFilter<TunerConfigurationModel,Integer>
	{
		private TunerType mTunerType;
		private String mUniqueID;
		
		public ConfigurationRowFilter( TunerType type, String uniqueID )
		{
			mTunerType = type;
			mUniqueID = uniqueID;
		}
		
		@Override
		public boolean include( javax.swing.RowFilter.Entry<? extends TunerConfigurationModel, 
								? extends Integer> entry )
		{
			TunerConfiguration config = entry.getModel()
					.getTunerConfiguration( entry.getIdentifier() );

			return config != null && 
				   config.getTunerType() == mTunerType &&
				   config.getUniqueID() != null &&
				   config.getUniqueID().contentEquals( mUniqueID );
		}
	}
	
}
