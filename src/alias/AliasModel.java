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
package alias;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.AliasEvent.Event;
import sample.Broadcaster;
import sample.Listener;

/**
 * Alias Model
 */
public class AliasModel implements TableModel
{
	private final static Logger mLog = LoggerFactory.getLogger( AliasModel.class );
	
	public static final int COLUMN_LIST = 0;
	public static final int COLUMN_GROUP = 1;
	public static final int COLUMN_NAME = 2;
	public static final int COLUMN_ICON = 3;
	public static final int COLUMN_COLOR = 4;

	private List<Alias> mAliases = new ArrayList<>();
	private List<TableModelListener> mTableModelListeners = new CopyOnWriteArrayList<>();
	private Broadcaster<AliasEvent> mAliasEventBroadcaster = new Broadcaster<>();

	public AliasModel()
	{
	}

	/**
	 * Unmodifiable list of all aliases currently in the model
	 */
	public List<Alias> getAliases()
	{
		return Collections.unmodifiableList( mAliases );
	}
	
	public Alias getAliasAtIndex( int row )
	{
		if( mAliases.size() >= row )
		{
			return mAliases.get( row );
		}
		
		return null;
	}
	
	/**
	 * Returns a list of unique alias list names from across the alias set
	 */
	public List<String> getListNames()
	{
		List<String> listNames = new ArrayList<>();
		
		for( Alias alias: mAliases )
		{
			if( alias.hasList() && !listNames.contains( alias.getList() ) )
			{
				listNames.add( alias.getList() );
			}
		}
		
		Collections.sort( listNames );
		
		return listNames;
	}
	
	/**
	 * Returns a list of alias group names for all aliases that have a matching
	 * list name value
	 */
	public List<String> getGroupNames( String listName )
	{
		List<String> groupNames = new ArrayList<>();

		if( listName != null )
		{
			for( Alias alias: mAliases )
			{
				if( alias.hasList() && 
					alias.hasGroup() &&
					listName.equals( alias.getList() ) &&
					!groupNames.contains( alias.getGroup() ) )
				{
					groupNames.add( alias.getGroup() );
				}
			}
		}

		Collections.sort( groupNames );
		
		return groupNames;
	}

	/**
	 * Bulk loading of aliases
	 */
	public void addAliases( List<Alias> aliases )
	{
		for( Alias alias: aliases )
		{
			addAlias( alias );
		}
	}

	/**
	 * Adds the alias to the model
	 */
	public int addAlias( Alias alias )
	{
		if( alias != null )
		{
			mAliases.add( alias );

			int index = mAliases.size() - 1;

			broadcast( new TableModelEvent( this, index, index, 
				TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT ) );
			
			broadcast( new AliasEvent( alias, Event.ADD ) );

			return index;
		}
		
		return -1;
	}
	
	/**
	 * Removes the channel from the model and broadcasts a channel remove event
	 */
	public void removeAlias( Alias alias )
	{
		if( alias != null )
		{
			int index = mAliases.indexOf( alias );
			
			mAliases.remove( alias );
			
			broadcast( new TableModelEvent( this, index, index, 
				TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE ) );
			
			broadcast( new AliasEvent( alias, Event.DELETE ) );
		}
	}

	@Override
	public int getRowCount()
	{
		return mAliases.size();
	}

	@Override
	public int getColumnCount()
	{
		return 5;
	}

	@Override
	public String getColumnName( int columnIndex )
	{
		switch( columnIndex )
		{
			case COLUMN_LIST:
				return "List";
			case COLUMN_GROUP:
				return "Group";
			case COLUMN_NAME:
				return "Name";
			case COLUMN_ICON:
				return "Icon";
			case COLUMN_COLOR:
				return "Color";
		}
		
		return null;
	}

	@Override
	public Class<?> getColumnClass( int columnIndex )
	{
		if( columnIndex == COLUMN_COLOR )
		{
			return Integer.class;
		}
		
		return String.class;
	}

	@Override
	public boolean isCellEditable( int rowIndex, int columnIndex )
	{
		return false;
	}

	@Override
	public Object getValueAt( int rowIndex, int columnIndex )
	{
		Alias alias = mAliases.get( rowIndex );
		
		switch( columnIndex )
		{
			case COLUMN_LIST:
				return alias.getList();
			case COLUMN_GROUP:
				return alias.getGroup();
			case COLUMN_NAME:
				return alias.getName();
			case COLUMN_ICON:
				return alias.getIconName();
			case COLUMN_COLOR:
				return alias.getColor();
		}
		
		return null;
	}

	@Override
	public void setValueAt( Object aValue, int rowIndex, int columnIndex )
	{
		throw new IllegalArgumentException( "Not yet implemented" );
	}
	
	private void broadcast( TableModelEvent event )
	{
		for( TableModelListener listener: mTableModelListeners )
		{
			listener.tableChanged( event );
		}
	}

	@Override
	public void addTableModelListener( TableModelListener listener )
	{
		mTableModelListeners.add( listener );
	}

	@Override
	public void removeTableModelListener( TableModelListener listener )
	{
		mTableModelListeners.remove( listener );
	}
	
	public void broadcast( AliasEvent event )
	{
		mAliasEventBroadcaster.broadcast( event );
		
		if( event.getEvent() == Event.CHANGE )
		{
			int index = mAliases.indexOf( event.getAlias() );
			
			broadcast( new TableModelEvent( this, index ) );
		}
	}
	
	public void addAliasEventListener( Listener<AliasEvent> listener )
	{
		mAliasEventBroadcaster.addListener( listener );
	}
	
	public void removeAliasEventListener( Listener<AliasEvent> listener )
	{
		mAliasEventBroadcaster.removeListener( listener );
	}
}
