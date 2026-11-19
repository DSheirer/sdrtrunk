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
package io.github.dsheirer.alias;

/**
 * Posted on the global event bus when an alias's playback priority is changed outside the alias editor, for example
 * by the Now Playing channel table's Mute/Unmute menu item, so that an open alias editor can update its Listen toggle.
 */
public class AliasPriorityChangedEvent
{
    private final Alias mAlias;

    /**
     * Constructs an instance
     * @param alias whose playback priority changed
     */
    public AliasPriorityChangedEvent(Alias alias)
    {
        mAlias = alias;
    }

    /**
     * Alias whose playback priority changed
     */
    public Alias getAlias()
    {
        return mAlias;
    }
}
