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
 * Filter interface
 *
 * @param <T> type of filter
 */
public interface IFilter<T>
{
    /**
     * Generic filter method.
     *
     * @param t - message to filter
     * @return - true if the message passes the filter
     */
    boolean passes(T t);

    /**
     * Indicates if the filter can process (filter) the object
     *
     * @param t - candidate message for filtering
     * @return - true if the filter is capable of filtering the message
     */
    boolean canProcess(T t);

    /**
     * Indicates if this filter is enabled to evaluate messages
     */
    boolean isEnabled();

    /**
     * Display name for the filter
     */
    String getName();

    /**
     * (Recursive) count of enabled child filter elements.
     *
     * @return total enable count.
     */
    int getEnabledCount();

    /**
     * (Recursive) count of total child filter elements.
     *
     * @return total count of child filter elements.
     */
    int getElementCount();

    /**
     * Registers the listener to be notified when the enabled state of any child filter elements is updated.
     *
     * @param listener to be notified
     */
    void register(IFilterChangeListener listener);
}
