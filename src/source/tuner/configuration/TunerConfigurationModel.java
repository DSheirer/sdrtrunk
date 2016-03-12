package source.tuner.configuration;

import java.util.ArrayList;
import java.util.List;

import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import source.tuner.TunerType;
import source.tuner.TunerViewPanel;
import source.tuner.configuration.TunerConfigurationEvent.Event;

public class TunerConfigurationModel extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;
	private final static Logger mLog = 
			LoggerFactory.getLogger( TunerConfigurationModel.class );
	
	private static final int TUNER_TYPE = 0;
	private static final int CONFIGURATION_NAME = 1;
	private static final String[] COLUMN_HEADERS = { "Tuner Type", "Name" };
	
	private List<Listener<TunerConfigurationEvent>> mConfigurationListeners = new ArrayList<>();
	private List<TunerConfiguration> mTunerConfigurations = new ArrayList<>();
	private List<TunerConfigurationAssignment> mTunerConfigurationAssignments = new ArrayList<>();

	/**
	 * Table model for managing tuner configurations and tuner configuration
	 * assignments.  Provides tuner configuration events to registered listeners
	 */
	public TunerConfigurationModel()
	{
	}

	/**
	 * Adds the list of tuner configurations to the model
	 */
	public void addTunerConfigurations( List<TunerConfiguration> configs )
	{
		for( TunerConfiguration config: configs )
		{
			addTunerConfiguration( config );
		}
	}

	/**
	 * Returns of list of tuner configurations from the model
	 */
	public List<TunerConfiguration> getTunerConfigurations()
	{
		return mTunerConfigurations;
	}

	/**
	 * Adds the tuner configuration to the model firing a table model row add
	 * event and broadcasting a tuner configuration event.
	 */
	public void addTunerConfiguration( TunerConfiguration config )
	{
		if( !mTunerConfigurations.contains( config ) )
		{
			mTunerConfigurations.add( config );

			int index = mTunerConfigurations.indexOf( config );
			
			fireTableRowsInserted( index, index );

			broadcast( new TunerConfigurationEvent( config, Event.CONFIGURATION_ADD ) );
		}
	}

	/**
	 * Removes the tuner configuration from the model firing a table model row
	 * delete event and broadcasting a tuner configuration event.
	 */
	public void removeTunerConfiguration( TunerConfiguration config )
	{
		if( mTunerConfigurations.contains( config ) )
		{
			int index = mTunerConfigurations.indexOf( config );
			
			mTunerConfigurations.remove( config );
			
			fireTableRowsDeleted( index, index );
			
			broadcast( new TunerConfigurationEvent( config, Event.CONFIGURATION_REMOVE ) );
		}
	}

	/**
	 * Removes all tuner configurations and assignments from the model and 
	 * fires a table clear event and broadcasts a tuner configuration event for 
	 * each row removed.
	 */
	public void clear()
	{
		for( TunerConfiguration config: mTunerConfigurations )
		{
			broadcast( new TunerConfigurationEvent( config, Event.CONFIGURATION_REMOVE ) );
		}
		
		int size = mTunerConfigurations.size();

		mTunerConfigurations.clear();
		
		fireTableRowsDeleted( 0, size - 1 );
		
		for( TunerConfigurationAssignment assignment: mTunerConfigurationAssignments )
		{
			broadcast( new TunerConfigurationEvent( assignment, Event.ASSIGNMENT_REMOVE ) );
		}
		
		mTunerConfigurationAssignments.clear();
	}

	/**
	 * Registers a tuner configuration event listener
	 */
	public void addListener( Listener<TunerConfigurationEvent> listener )
	{
		mConfigurationListeners.add( listener );
	}

	/**
	 * Removes the tuner configuration event listener
	 */
	public void removeListener( Listener<TunerConfigurationEvent> listener )
	{
		mConfigurationListeners.remove( listener );
	}

	/**
	 * Broadcasts a tuner configuration event
	 */
	public void broadcast( TunerConfigurationEvent event )
	{
		for( Listener<TunerConfigurationEvent> listener: mConfigurationListeners )
		{
			listener.receive( event );
		}
	}

	/**
	 * Adds a list of tuner configuration assignments to the model
	 */
	public void addTunerConfigurationAssignments( List<TunerConfigurationAssignment> assignments )
	{
		for( TunerConfigurationAssignment assigment: assignments )
		{
			addTunerConfigurationAssignment( assigment );
		}
	}

	/**
	 * Returns of list of tuner configurations from the model
	 */
	public List<TunerConfigurationAssignment> getTunerConfigurationAssignments()
	{
		return mTunerConfigurationAssignments;
	}

	/**
	 * Adds a tuner configuration assigment to the model
	 */
	public void addTunerConfigurationAssignment( TunerConfigurationAssignment assignment )
	{
		mTunerConfigurationAssignments.add( assignment );
		
		broadcast( new TunerConfigurationEvent( assignment, Event.ASSIGNMENT_ADD ) );
	}

	/**
	 * Removes a tuner configuration assignment from the model
	 */
	public void removeTunerConfigurationAssignment( TunerConfigurationAssignment assignment )
	{
		mTunerConfigurationAssignments.remove( assignment );

		broadcast( new TunerConfigurationEvent( assignment, Event.ASSIGNMENT_REMOVE ) );
	}

	/**
	 * Returns the tuner configuration that is assigned to the tuner with the 
	 * tuner type and uniqueID, or creates a new tuner configuration and tuner
	 * configuration assignment for the tuner.
	 */
	public TunerConfiguration getTunerConfiguration( TunerType type, String uniqueID )
	{
		String configurationName = null;
		
		TunerConfigurationAssignment assignment = getTunerConfigurationAssignment( type, uniqueID );

		if( assignment != null )
		{
			configurationName = assignment.getTunerConfigurationName();
		}

		if( configurationName == null )
		{
			configurationName = "Default-" + uniqueID;
		}

		List<TunerConfiguration> configs = getTunerConfigurations( type );
		
		for( TunerConfiguration config: configs )
		{
			if( config.getName().contentEquals( configurationName ) )
			{
				if( assignment == null )
				{
					assignTunerConfiguration( config, configurationName );
				}
				
				return config;
			}
		}

		//We didn't find the config so create a new one and assign it
		TunerConfiguration config = TunerConfigurationFactory
				.getTunerConfiguration( type, configurationName );
		
		addTunerConfiguration( config );
		assignTunerConfiguration( config, configurationName );
		
		return config;
	}

	/**
	 * Assigns the tuner configuration to the tuner identified by the uniqueID
	 */
	public void assignTunerConfiguration( TunerConfiguration config, String uniqueID )
	{
		TunerConfigurationAssignment assignment = 
			getTunerConfigurationAssignment( config.getTunerType(), uniqueID );

		Event event = Event.ASSIGNMENT_CHANGE;
		
		if( assignment == null )
		{
			assignment = new TunerConfigurationAssignment();
			assignment.setUniqueID( uniqueID );
			
			addTunerConfigurationAssignment( assignment );
			
			event = Event.ASSIGNMENT_ADD;
		}
		
		assignment.setTunerConfigurationName( config.getName() );
		
		broadcast( new TunerConfigurationEvent( assignment, event ) );
	}

	/**
	 * Returns the tuner configuration assiged to the tuner type and unique id
	 */
	private TunerConfigurationAssignment getTunerConfigurationAssignment( TunerType type, String uniqueID )
	{
		for( TunerConfigurationAssignment assignment: mTunerConfigurationAssignments )
		{
			if( assignment.getTunerType() == type &&
				assignment.getUniqueID().contentEquals( uniqueID ) )
			{
				return assignment;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns all tuner configurations for the specified tuner type
	 */
	public List<TunerConfiguration> getTunerConfigurations( TunerType type )
	{
		List<TunerConfiguration> configs = new ArrayList<>();
		
		for( TunerConfiguration config: mTunerConfigurations )
		{
			if( config.getTunerType() == type )
			{
				configs.add( config );
			}
		}
		
		return configs;
	}

	/**
	 * Column names for the table model
	 */
	@Override
	public String getColumnName( int column )
	{
		return COLUMN_HEADERS[ column ];
	}

	/**
	 * Tuner configuration model row count
	 */
	@Override
	public int getRowCount()
	{
		return mTunerConfigurations.size();
	}

	/**
	 * Tuner configuration model column count
	 */
	@Override
	public int getColumnCount()
	{
		return COLUMN_HEADERS.length;
	}

	/**
	 * Returns the row index for the specified configuration or -1
	 */
	public int getRowIndex( TunerConfiguration configuration )
	{
		if( mTunerConfigurations.contains( configuration ) )
		{
			return mTunerConfigurations.indexOf( configuration );
		}
		
		return -1;
	}

	/**
	 * Returns the configuration at the specified index or null
	 */
	public TunerConfiguration getTunerConfiguration( int index )
	{
		if( index >= 0 && index < mTunerConfigurations.size() )
		{
			return mTunerConfigurations.get( index );
		}
		
		return null;
	}

	/**
	 * Returns the value at the indicated table cell
	 */
	@Override
	public Object getValueAt( int row, int column )
	{
		if( row >= 0 && row < mTunerConfigurations.size() )
		{
			TunerConfiguration config = mTunerConfigurations.get( row );
			
			switch( column )
			{
				case TUNER_TYPE:
					return config.getTunerType().getLabel();
				case CONFIGURATION_NAME:
					return config.getName();
				default:
					break;
			}
		}
		
		return null;
	}
	
}
