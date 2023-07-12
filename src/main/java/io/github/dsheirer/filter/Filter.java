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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base filter class that uses an extractor function to extract a filter key (K) from an object of type T and then
 * lookup the filter element (rule) indicating if objects of this type are enabled.
 *
 * @param <T> object type that this filter can process.
 * @param <K> key type used to lookup filter elements
 */
public abstract class Filter<T,K> implements IFilter<T>
{
    private static final Logger LOG = LoggerFactory.getLogger(Filter.class);
    private String mName;
    private Map<K, FilterElement<K>> mFilterElementMap = new HashMap<>();
    private IFilterChangeListener mFilterChangeListener;

    /**
     * Constructs an instance
     * @param name of this filter
     */
    public Filter(String name)
    {
        mName = name;
    }

    /**
     * Indicates if this filter contains a filter element matching the key value.
     * @param key to check
     * @return true if this filter has a matching filter element.
     */
    protected boolean hasKey(K key)
    {
        return mFilterElementMap.containsKey(key);
    }

    /**
     * Key extractor function provided by the subclass.
     * @return key extractor function
     */
    public abstract Function<T,K> getKeyExtractor();

    @Override
    public boolean passes(T t)
    {
        K key = getKeyExtractor().apply(t);

        if(mFilterElementMap.containsKey(key))
        {
            return mFilterElementMap.get(key).isEnabled();
        }

        return false;
    }

    /**
     * Indicates if this filter can process the item T, meaning that it has a FilterElement that matches the key
     * value for items of type T.
     * @param t to test
     * @return true if this filter can process item t
     */
    @Override
    public boolean canProcess(T t)
    {
        return hasKey(getKeyExtractor().apply(t));
    }

    @Override
    public boolean isEnabled()
    {
        for(FilterElement filterElement: mFilterElementMap.values())
        {
            if(filterElement.isEnabled())
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Adds the child filter element to this filter
     * @param filterElement to add
     */
    public void add(FilterElement<K> filterElement)
    {
        if(mFilterElementMap.containsKey(filterElement.getElement()))
        {
            LOG.warn("Filter element for key [" + filterElement.getElement() + "] named [" + filterElement.getName() +
                    "] already exists - overwriting existing value");
        }

        mFilterElementMap.put(filterElement.getElement(), filterElement);
        filterElement.register(mFilterChangeListener);
    }

    /**
     * List of filter elements managed by this filter
     */
    public final List<FilterElement<K>> getFilterElements()
    {
        return new ArrayList<>(mFilterElementMap.values());
    }

    /**
     * Name of this filter
     */
    public String getName()
    {
        return mName;
    }

    /**
     * Sets the name of this filter
     *
     * @param name to set
     */
    public void setName(String name)
    {
        mName = name;
    }

    /**
     * Pretty version or name of this filter.
     *
     * @return name
     */
    public String toString()
    {
        return mName;
    }

    /**
     * Count of enabled child filter elements.
     *
     * @return count
     */
    @Override
    public int getEnabledCount()
    {
        int count = 0;

        for(FilterElement filterElement : mFilterElementMap.values())
        {
            if(filterElement.isEnabled())
            {
                count++;
            }
        }

        return count;
    }

    /**
     * Count of child filter elements
     *
     * @return count
     */
    @Override
    public int getElementCount()
    {
        return mFilterElementMap.size();
    }

    /**
     * Registers a filter change listener on this filter and all filter elements.
     * @param listener to be notified of any filter changes.
     */
    @Override
    public void register(IFilterChangeListener listener)
    {
        mFilterChangeListener = listener;

        for(FilterElement child: mFilterElementMap.values())
        {
            child.register(listener);
        }
    }
}
