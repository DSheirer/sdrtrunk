/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.alias.id;

/**
 * NXDN Type-D alias identifier range of radio or talkgroup values that share a common home repeater.
 */
public interface INXDNTypeDRange
{
    /**
     * Home repeater for the range
     */
    int getHomeRepeater();

    /**
     * Sets the home repeater for the range
     */
    void setHomeRepeater(int homeRepeater);

    /**
     * Minimum radio or talkgroup value, without the home repeater.
     */
    int getMinIdentifier();

    /**
     * Sets the minimum radio or talkgroup value, without the home repeater.
     */
    void setMinIdentifier(int identifier);

    /**
     * Maximum radio or talkgroup value, without the home repeater.
     */
    int getMaxIdentifier();

    /**
     * Sets the maximum radio or talkgroup value, without the home repeater.
     */
    void setMaxIdentifier(int identifier);
}
