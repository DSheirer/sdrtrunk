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
import javax.swing.JTextArea;
import javax.swing.RowFilter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import source.SourceException;
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
	
	public TunerEditor( TunerConfigurationModel tunerConfigurationModel )
	{
		mTunerConfigurationModel = tunerConfigurationModel;
		init();
	}
	
	public void init()
	{
		setLayout( new MigLayout( "insets 0 0 0 0", "[grow,fill]", "[grow,fill]" ) );

		JPanel listPanel = new JPanel();
		listPanel.setLayout( new MigLayout( "fill,wrap 2", "[grow,fill]", "[][][][grow,fill][]" ) );

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
							setConfigurationEditor( mTunerConfigurationModel.getTunerConfiguration( modelRow ) );
						}
					}
				}
			}
		} );

		listPanel.add( new JLabel( "Tuner Configurations" ), "span" );
		listPanel.add( mTunerConfigurationTable, "span" );

		mNewConfigurationButton.setEnabled( false );
		mNewConfigurationButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				Tuner tuner = getItem();
				
				int counter = 0;
				
				String name = tuner.getUniqueID() + "-New";

				while( counter < 10 && mTunerConfigurationModel
					.hasTunerConfiguration( tuner.getTunerType(), name ) )
				{
					counter++;
					
					StringBuilder sb = new StringBuilder();
					sb.append( tuner.getUniqueID() );
					sb.append( "-New(" );
					sb.append( String.valueOf( counter ) );
					sb.append( ")" );
					
					name = sb.toString();
				}

				if( counter < 10 )
				{
					TunerConfiguration config = TunerConfigurationFactory
						.getTunerConfiguration( tuner.getTunerType(), name );
					
					mTunerConfigurationModel.addTunerConfiguration( config );
				}
				else
				{
					JOptionPane.showMessageDialog( TunerEditor.this, 
						"Can't create configuration - maximum limit reached", 
						"Error", 
						JOptionPane.OK_OPTION );
				}
			}
		} );
		listPanel.add( mNewConfigurationButton );

		mDeleteConfigurationButton.setEnabled( false );
		listPanel.add( mDeleteConfigurationButton );
		
		//Set preferred size on both scrollers to same width, so they split the space
		JScrollPane listScroller = new JScrollPane( listPanel );
		listScroller.setPreferredSize( new Dimension( 200, 80 ) );
		
//TODO: this has to be changed
		JPanel editorPanel = new EmptyEditor<TunerConfiguration>();
		JScrollPane editorScroller = new JScrollPane( editorPanel );
		editorScroller.setPreferredSize( new Dimension( 200, 80 ) );

		JideSplitPane splitPane = 
				new JideSplitPane( JideSplitPane.HORIZONTAL_SPLIT );
		splitPane.add( listScroller );
		splitPane.add( editorScroller );

		add( splitPane );
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

			mRowSorter.setRowFilter( new ConfigurationRowFilter( tuner.getTunerType() ) );

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
		}
	}
	
	private void setConfigurationEditor( TunerConfiguration config )
	{
		if( config != null )
		{
			mDeleteConfigurationButton.setEnabled( true );
		}
		else
		{
			mDeleteConfigurationButton.setEnabled( false );
		}
	}
	
	public class ConfigurationRowFilter extends RowFilter<TunerConfigurationModel,Integer>
	{
		private TunerType mTunerType;
		
		public ConfigurationRowFilter( TunerType type )
		{
			mTunerType = type;
		}
		
		@Override
		public boolean include( javax.swing.RowFilter.Entry<? extends TunerConfigurationModel, 
								? extends Integer> entry )
		{
			TunerConfiguration config = entry.getModel()
					.getTunerConfiguration( entry.getIdentifier() );

			return config != null && config.getTunerType() == mTunerType;
		}
	}
	
}
