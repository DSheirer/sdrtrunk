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
package map;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import message.Message;
import sample.Listener;
import settings.SettingsManager;
import alias.Alias;
import controller.NamingThreadFactory;

public class MapService implements Listener<Message>
{
	private int mMaxHistory = 2;
	private int mCullThresholdMinutes = 60;
	
	private static final Color sDEFAULT_COLOR = Color.BLACK;
	
	private final ScheduledExecutorService scheduler =
		     Executors.newScheduledThreadPool(1, 
		    		 new NamingThreadFactory( "map tile fetcher" ) );
	
	private ArrayList<PlottableUpdateListener> mListeners = 
				new ArrayList<PlottableUpdateListener>();

	private HashMap<String,PlottableEntity> mEntities = 
						new HashMap<String,PlottableEntity>();

	private SettingsManager mSettingsManager;
	
	public MapService( SettingsManager resourceManager )
	{
		mSettingsManager = resourceManager;
		
//		scheduler.scheduleAtFixedRate( new CullThread(), 5, 5, TimeUnit.MINUTES );		
	}
	
	/**
	 * Returns the max plottables (locations) history setting that is applied 
	 * to all plottable entities
	 */
	public int getMaxHistory()
	{
		return mMaxHistory;
	}

	/**
	 * Sets the max history trail for entities.  Value is applied to all newly
	 * created PlottableEntity and all existing entities.
	 */
	public void setMaxHistory( int maxHistory )
	{
		mMaxHistory = maxHistory;

		/**
		 * Update any existing entities with the new max history value
		 */
		for( PlottableEntity entity: mEntities.values() )
		{
			entity.setMaxHistory( mMaxHistory );
		}
	}
	
	@Override
    public void receive( Message message )
    {
		Plottable plottable = message.getPlottable();
		
		if( plottable != null )
		{
			PlottableEntity entity = mEntities.get( plottable.getID() );
			
			if( entity == null )
			{
				Alias alias = plottable.getAlias();

				Color color = null;
				MapIcon icon = null;
				
				if( alias != null )
				{
					color = alias.getMapColor();
					
					String iconName = alias.getIconName();
					
					if( iconName == null )
					{
						iconName = SettingsManager.DEFAULT_ICON;
					}

					icon = mSettingsManager.getMapIcon( iconName );
				}
				
				if( color == null )
				{
					color = sDEFAULT_COLOR;
				}
				
				entity = new PlottableEntity( plottable, color );
				
				entity.setMaxHistory( mMaxHistory );
				
				mEntities.put( plottable.getID(), entity );
				
				for( PlottableUpdateListener listener: mListeners )
				{
					listener.addPlottableEntity( entity );
				}
			}
			else
			{
				entity.addPlottable( plottable );

				for( PlottableUpdateListener listener: mListeners )
				{
					listener.entitiesUpdated();;
				}
			}
		}
    }
	
	public void addListener( PlottableUpdateListener listener )
	{
		mListeners.add( listener );
	}
	
	public void removeListener( PlottableUpdateListener listener )
	{
		mListeners.remove( listener );
	}
	
	public class CullThread implements Runnable
	{
		@Override
        public void run()
        {
			long cutoffTime = System.currentTimeMillis() - 
						( mCullThresholdMinutes * 1000 );

			Set<String> ids = Collections.unmodifiableSet( mEntities.keySet() );

			for( String id: ids )
			{
				PlottableEntity entity = mEntities.get( id );
				
				if( entity.getCurrentPlottable().getTimestamp() < cutoffTime )
				{
					mEntities.remove( id );

					/**
					 * Let all listeners know we've removed (culled) an entity
					 */
					for( PlottableUpdateListener listener: mListeners )
					{
						listener.removePlottableEntity( entity );
					}
				}
			}
        }
	}
}
