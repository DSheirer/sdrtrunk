package source.tuner.configuration;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import source.tuner.TunerType;
import source.tuner.configuration.TunerConfigurationEvent.Event;

public class TunerConfigurationModel extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;
	private final static Logger mLog = 
			LoggerFactory.getLogger( TunerConfigurationModel.class );
	
	private static final int TUNER_TYPE = 0;
	private static final int CONFIGURATION_NAME = 1;
	private static final int ASSIGNED = 2;
	private static final String[] COLUMN_HEADERS = { "Tuner Type", "Name", "Assigned" };
	
	private List<Listener<TunerConfigurationEvent>> mConfigurationListeners = new ArrayList<>();
	private List<TunerConfiguration> mTunerConfigurations = new ArrayList<>();

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
		if( !mTunerConfigurations.contains( config ) &&
			config.getUniqueID() != null &&
			config.getName() != null )
		{
			mTunerConfigurations.add( config );

			int index = mTunerConfigurations.indexOf( config );
			
			fireTableRowsInserted( index, index );

			broadcast( new TunerConfigurationEvent( config, Event.ADD ) );
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
			
			broadcast( new TunerConfigurationEvent( config, Event.REMOVE ) );
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
			broadcast( new TunerConfigurationEvent( config, Event.REMOVE ) );
		}
		
		int size = mTunerConfigurations.size();

		mTunerConfigurations.clear();
		
		fireTableRowsDeleted( 0, size - 1 );
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
	 * Returns the tuner configuration that is assigned to the tuner with the 
	 * tuner type and uniqueID, or selects the first existing configuration that
	 * matches the tuner type and unique ID and assigns it, or creates a new 
	 * tuner configuration and assigns it to the tuner.
	 * 
	 * This method will always return a tuner configuration that is assigned to
	 * the tuner type and unique ID.
	 */
	public TunerConfiguration getTunerConfiguration( TunerType type, String uniqueID )
	{
		List<TunerConfiguration> configs = getTunerConfigurations( type, uniqueID );
		
		for( TunerConfiguration config: configs )
		{
			if( config.isAssigned() && 
				config.getUniqueID() != null &&
				config.getUniqueID().contentEquals( uniqueID ) )
			{
				return config;
			}
		}

		//Reuse an existing configuration to assign
		if( !configs.isEmpty() )
		{
			configs.get( 0 ).setAssigned( true );
			return configs.get( 0 );
		}

		String name = getDistinctName( type, uniqueID );
				
		//We didn't find the config so create a new one and assign it
		TunerConfiguration config = TunerConfigurationFactory
				.getTunerConfiguration( type, uniqueID, name );
		
		config.setAssigned( true );
		
		addTunerConfiguration( config );
		
		return config;
	}
	
	/**
	 * Indicates if a configuration exists in this model matching the tuner type
	 * and unique ID and name.
	 */
	public boolean hasTunerConfiguration( TunerType type, String uniqueID, String name )
	{
		for( TunerConfiguration config: mTunerConfigurations )
		{
			if( config.getTunerType() == type && 
				config.getUniqueID() != null &&
				config.getUniqueID().contentEquals( uniqueID ) &&
				config.getName() != null &&
				config.getName().contentEquals( name ) )
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Creates a configuration name for the tuner configuration that is distinct
	 * among the existing tuner configurations of the same type and uniqueID
	 */
	public String getDistinctName( TunerType type, String uniqueID )
	{
		int counter = 0;
		
		String name = "New";

		while( hasTunerConfiguration( type, uniqueID, name ) )
		{
			counter++;
			
			name = "New (" + counter + ")";
		}

		return name;
	}

	/**
	 * Returns all tuner configurations for the specified tuner type
	 */
	public List<TunerConfiguration> getTunerConfigurations( TunerType type, String uniqueID )
	{
		List<TunerConfiguration> configs = new ArrayList<>();
		
		for( TunerConfiguration config: mTunerConfigurations )
		{
			if( config.getTunerType() == type && 
				config.getUniqueID() != null &&
				config.getUniqueID().contentEquals( uniqueID ) )
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
				case ASSIGNED:
					return config.isAssigned() ? "*" : null;
				default:
					break;
			}
		}
		
		return null;
	}
}
