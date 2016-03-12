package source.tuner;

import gui.control.JFrequencyControl;
import gui.editor.Editor;
import gui.editor.EmptyEditor;

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;

import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import source.tuner.configuration.TunerConfiguration;
import source.tuner.configuration.TunerConfigurationModel;

import com.jidesoft.swing.JideSplitPane;

public class TunerEditor extends Editor<Tuner>
{
	private static final long serialVersionUID = 1L;

	private final static Logger mLog = LoggerFactory.getLogger( TunerEditor.class );

	private TunerConfigurationModel mTunerConfigurationModel;
	private JTable mTunerConfigurationTable;
	private TableRowSorter<TunerConfigurationModel> mRowSorter;
	private ConfigurationRowFilter mRowFilter = new ConfigurationRowFilter();
	private JFrequencyControl mFrequencyControl = new JFrequencyControl();
	
	public TunerEditor( TunerConfigurationModel tunerConfigurationModel )
	{
		mTunerConfigurationModel = tunerConfigurationModel;
		init();
	}
	
	public void init()
	{
		setLayout( new MigLayout( "insets 0 0 0 0", "[grow,fill]", "[grow,fill]" ) );

		JPanel listPanel = new JPanel();
		listPanel.setLayout( new MigLayout( "fill,wrap 1", 
				"[grow,fill]", "[][][grow,fill][]" ) );
		listPanel.add( mFrequencyControl );

//        mFrequencyControl.addListener( this );
//        /* Add frequency control as frequency change listener. */
//        mController.addListener( mFrequencyControl );
//        mFrequencyControl.setFrequency( mController.getFrequency(), false );

		mTunerConfigurationTable = new JTable( mTunerConfigurationModel );
		mRowSorter = new TableRowSorter<>( mTunerConfigurationModel );
		mRowSorter.setRowFilter( mRowFilter );
		mTunerConfigurationTable.setRowSorter( mRowSorter );

		listPanel.add( new JLabel( "Tuner Configurations" ) );
		listPanel.add( mTunerConfigurationTable );

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new MigLayout( "insets 0 0 0 0", 
				"[grow,fill][grow,fill]", "[grow,fill]" ) );
		
		JButton newButton = new JButton( "New" );
		buttonPanel.add( newButton );
		
		JButton deleteButton = new JButton( "Delete" );
		buttonPanel.add( deleteButton );
		
		listPanel.add( buttonPanel );

		//Set preferred size on both scrollers to same width, so they split the space
		JScrollPane listScroller = new JScrollPane( listPanel );
		listScroller.setPreferredSize( new Dimension( 200, 80 ) );
		
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
	}

	@Override
	public void setItem( Tuner tuner )
	{
		super.setItem( tuner );
		
		if( hasItem() )
		{
			mRowFilter.setTunerType( tuner.getTunerType() );

			TunerConfiguration assigned = mTunerConfigurationModel
				.getTunerConfiguration( tuner.getTunerType(), tuner.getUniqueID() );

			int modelIndex = mTunerConfigurationModel.getRowIndex( assigned );
			
			if( modelIndex >= 0 )
			{
				int viewIndex = mTunerConfigurationTable.convertRowIndexToView( modelIndex );
				
				if( viewIndex >= 0 )
				{
					mTunerConfigurationTable.setRowSelectionInterval( viewIndex, viewIndex );
				}
			}
		}
		else
		{
			mRowFilter.setTunerType( null );
		}
		
		mRowSorter.setRowFilter( mRowFilter );
	}
	
	public class ConfigurationRowFilter extends RowFilter<TunerConfigurationModel,Integer>
	{
		private TunerType mTunerType;
		
		public ConfigurationRowFilter()
		{
		}
		
		public void setTunerType( TunerType type )
		{
			mTunerType = type;
		}
		
		@Override
		public boolean include( javax.swing.RowFilter.Entry<? extends TunerConfigurationModel, 
								? extends Integer> entry )
		{
			if( mTunerType == null )
			{
				return true;
			}
			else
			{
				TunerConfigurationModel model = entry.getModel();
				TunerConfiguration config = model.getTunerConfiguration( entry.getIdentifier() );
				
				if( config != null )
				{
					return config.getTunerType() == mTunerType;
				}
			}
			
			return false;
		}
	}
	
}
