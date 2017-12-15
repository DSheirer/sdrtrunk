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
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.jdesktop.swingx.mapviewer.GeoPosition;

import alias.Alias;

public class PlottableEntity implements Comparable<PlottableEntity>
{
	private TreeSet<Plottable> mPlottables = new TreeSet<Plottable>();

	private Alias mAlias;
	private Color mColor;
	private String mID;
	
	private int mMaxHistory = 2;

	/**
	 * Constructs the entity using the group and alias from the plottable and
	 * adds the plottable as the first plottable entry in the history
	 */
	public PlottableEntity( Plottable plottable, Color color )
	{
		addPlottable( plottable );
		mColor = color;
	}
	
	public int getMaxHistory()
	{
		return mMaxHistory;
	}
	
	public void setMaxHistory( int maxHistory )
	{
		if( maxHistory >= 1 )
		{
			mMaxHistory = maxHistory;
		}
	}
	
	public Plottable getCurrentPlottable()
	{
		return mPlottables.last();
	}
	
	public GeoPosition getCurrentGeoPosition()
	{
		return getCurrentPlottable().getGeoPosition();
	}

	/**
	 * Adds the plottable to this entity
	 */
	public void addPlottable( Plottable plottable )
	{
		/**
		 * Grab the entity settings from the latest plot
		 */
		mAlias = plottable.getAlias();
		mID = plottable.getID();
		
		/**
		 * Add the plottable to the list
		 */
		mPlottables.add( plottable );
		
		while( mPlottables.size() > mMaxHistory )
		{
			mPlottables.remove( mPlottables.first() );
		}
	}
	
	public String getLabel()
	{
		if( mAlias == null )
		{
			return mID;
		}
		else
		{
			return mAlias.getName();
		}
	}
	
	public String getMapIconName()
	{
		if( mAlias != null )
		{
			return mAlias.getIconName();
		}
		else
		{
			return null;
		}
	}

	/**
	 * Color for rendering this entity (label, route, etc.)
	 */
	public Color getColor()
	{
		return mColor;
	}
	
	public void setColor( Color color )
	{
		mColor = color;
	}
	
	/**
	 * Default id to use in the label
	 */
	public String getID()
	{
		return mID;
	}
	
	/**
	 * Gets the alias for this entity
	 */
	public Alias getAlias()
	{
		return mAlias;
	}

	/**
	 * Returns an unmodifiable sorted (time order) list of plottables
	 */
	public Set<Plottable> getPlottables()
	{
		return Collections.unmodifiableSet( mPlottables );
	}

	/**
	 * Clears all plottables from this entity
	 */
	public void clear()
	{
		mPlottables.clear();
	}

	/**
	 * Compares by alias name
	 */
	@Override
    public int compareTo( PlottableEntity o )
    {
	    return this.getAlias().getName().compareTo( o.getAlias().getName() );
    }
}
