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
 * NXDN Type-D alias identifier composed of a home repeater and a radio or talkgroup identifier value.
 */
public interface INXDNTypeD
{
    /**
     * Home repeater for the identifier
     */
    int getHomeRepeater();

    /**
     * Sets the home repeater for the identifier
     */
    void setHomeRepeater(int homeRepeater);

    /**
     * Radio or talkgroup value, without the home repeater.
     */
    int getIdentifier();

    /**
     * Sets the radio or talkgroup value, without the home repeater.
     */
    void setIdentifier(int identifier);
}
