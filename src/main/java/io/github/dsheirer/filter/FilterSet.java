/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.filter;

import io.github.dsheirer.log.LoggingSuppressor;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter set is a parent container for filters.
 *
 * @param <T> type of element tracked/filtered by this filter set.
 */
public class FilterSet<T> implements IFilter<T>
{
	private static final Logger LOGGER = LoggerFactory.getLogger(FilterSet.class);
	private static LoggingSuppressor sLoggingSuppressor = new LoggingSuppressor(LOGGER);
	private List<IFilter<T>> mFilters = new ArrayList<>();
	private String mName;
	private IFilterChangeListener mFilterChangeListener;

	/**
	 * Constructs an instance of a filter set.
	 *
	 * @param name of this filter set.
	 */
	public FilterSet(String name)
	{
		mName = name;
	}

	/**
	 * Constructs an instance with a single filter using the default 'Filters' name.
	 *
	 * @param filter for this filter set.
	 */
	public FilterSet(IFilter<T> filter)
	{
		this("Filters");
		addFilter(filter);
	}

	/**
	 * Registers the listener with each child filter element.
	 * @param listener to register
	 */
	public void register(IFilterChangeListener listener)
	{
		mFilterChangeListener = listener;

		for(IFilter child: mFilters)
		{
			child.register(listener);
		}
	}

	/**
	 * Name of this filter set.
	 *
	 * @return name
	 */
	public String getName()
	{
		return mName;
	}

	/**
	 * Sets the name of this filter set.
	 *
	 * @param name to set
	 */
	public void setName(String name)
	{
		mName = name;
	}

	/**
	 * Name for this filter set.
	 *
	 * @return name
	 */
	public String toString()
	{
		return getName();
	}

	/**
	 * Indicates if the element passes at least one of the child filters in this filter set.
	 *
	 * @param t to evaluate
	 * @return true if the argument passes a child filter.
	 */
	@Override
	public boolean passes(T t)
	{
		for(IFilter<T> filter : mFilters)
		{
			//Stop at the first filter that says that it can process the message type.
			if(filter.canProcess(t))
			{
				return filter.passes(t);
			}
		}

		return false;
	}

	/**
	 * Indicates if this filter set contains at least one filter that can evaluate the type of argument that is presented.
	 *
	 * @param t element to evaluate
	 * @return true if this filter set can evaluate objects of the argument's type.
	 */
	@Override
	public boolean canProcess(T t)
	{
		for(IFilter<T> filter : mFilters)
		{
			if(filter.canProcess(t))
			{
				return true;
			}
		}

		sLoggingSuppressor.info(t.getClass().getSimpleName(), 1, "FilterSet [" +
			this.getClass().getSimpleName() + "] has no filter element for items of type [" +
				t.getClass().getSimpleName() + "]");

		return false;
	}

	/**
	 * Child filters
	 *
	 * @return filters
	 */
	public List<IFilter<T>> getFilters()
	{
		return new ArrayList<>(mFilters);
	}

	/**
	 * Adds a list of child filters to this filter set.
	 * @param filters to add
	 */
	public void addFilters(List<IFilter<T>> filters)
	{
		for(IFilter<T> filter: filters)
		{
			addFilter(filter);
		}
	}

	/**
	 * Adds the child filter to this filter set.
	 * @param filter to add
	 */
	public void addFilter(IFilter<T> filter)
	{
		if(filter instanceof Filter<?,?> filterInstance && filterInstance.getKeyExtractor() == null)
		{
			LOGGER.warn("Filter [" + filter.getClass() + "] has a null key extractor");
		}

		mFilters.add(filter);
		filter.register(mFilterChangeListener);
	}

	/**
	 * Indicates if any of the child filters contain at least one filter element that is enabled.
	 * @return enabled state
	 */
	@Override
	public boolean isEnabled()
	{
		for(IFilter filter: mFilters)
		{
			if(filter.isEnabled())
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Recursive count of enabled child filter elements.
	 * @return count
	 */
	@Override
	public int getEnabledCount()
	{
		int count = 0;

		for(IFilter filter: mFilters)
		{
			count += filter.getEnabledCount();
		}

		return count;
	}

	/**
	 * Recursive count of child filter elements.
	 * @return count
	 */
	@Override
	public int getElementCount()
	{
		int count = 0;

		for(IFilter filter: mFilters)
		{
			count += filter.getElementCount();
		}

		return count;
	}
}
