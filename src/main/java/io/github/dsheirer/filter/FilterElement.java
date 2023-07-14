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

/**
 * Filter element provides filtering and enabled state tracking for an individual filterable element.
 *
 * @param <T> element to track and filter.
 */
public class FilterElement<T> implements Comparable<FilterElement<T>>
{
    private boolean mEnabled;
    private final T mElement;
    private IFilterChangeListener mFilterChangeListener;

    /**
     * Constructs an instance
     *
     * @param element to track
     * @param enabled enabled state of this filter element
     */
    public FilterElement(T element, boolean enabled)
    {
        mElement = element;
        mEnabled = enabled;
    }

    /**
     * Constructs an instance with a default state of enabled (true).
     *
     * @param t element to track
     */
    public FilterElement(T t)
    {
        this(t, true);
    }

    /**
     * Registers the listener with each child filter element.
     *
     * @param listener to register
     */
    public void register(IFilterChangeListener listener)
    {
        mFilterChangeListener = listener;
    }

    /**
     * Element being filtered.
     *
     * @return element
     */
    public T getElement()
    {
        return mElement;
    }

    /**
     * Name or display string for the element.
     *
     * @return string version of the element.
     */
    public String toString()
    {
        return getName();
    }

    /**
     * Indicates if this filter is enabled indicating that elements of this type will pass this filter.
     *
     * @return enabled state.
     */
    public boolean isEnabled()
    {
        return mEnabled;
    }

    /**
     * Sets the enabled state of this filter.
     *
     * @param enabled true to allow this filter's element type to pass the filter.
     */
    public void setEnabled(boolean enabled)
    {
        mEnabled = enabled;

        if(mFilterChangeListener != null)
        {
            mFilterChangeListener.filterChanged();
        }
    }

    /**
     * Name of the tracked element.
     *
     * @return name
     */
    public String getName()
    {
        return mElement.toString();
    }

    /**
     * Implements compareto interface for ordering of filter elements.
     *
     * @param other the object to be compared.
     * @return comparison value
     */
    @Override
    public int compareTo(FilterElement<T> other)
    {
        return toString().compareTo(other.toString());
    }

    /**
     * Implement equals interface for comparison of filter elements.
     *
     * @param o to compare
     * @return comparison
     */
    @Override
    public boolean equals(Object o)
    {
        if(o instanceof FilterElement fe)
        {
            return compareTo(fe) == 0;
        }

        return false;
    }
}
