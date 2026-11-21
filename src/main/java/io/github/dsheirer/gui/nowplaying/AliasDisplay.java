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
package io.github.dsheirer.gui.nowplaying;

import com.google.common.base.Joiner;
import io.github.dsheirer.alias.Alias;
import java.util.List;

/**
 * Alias table cell value: the alias names to show, with the color and icon of the alias that styles the cell.
 * @param text of the alias names
 * @param color of the styling alias as a 32-bit RGB integer
 * @param iconName of the styling alias, or null
 */
public record AliasDisplay(String text, int color, String iconName) implements Comparable<AliasDisplay>
{
    /**
     * Creates the display for the aliases, styled by the first alias.
     * @param aliases to display
     * @return display or null when there are no aliases.
     */
    public static AliasDisplay of(List<Alias> aliases)
    {
        if(aliases == null || aliases.isEmpty())
        {
            return null;
        }

        return of(Joiner.on(", ").skipNulls().join(aliases), aliases.getFirst());
    }

    /**
     * Creates the display for the text, styled by the alias
     * @param text to display
     * @param alias to style the display
     * @return display
     */
    public static AliasDisplay of(String text, Alias alias)
    {
        return new AliasDisplay(text, alias.getColor(), alias.getIconName());
    }

    @Override
    public int compareTo(AliasDisplay other)
    {
        return String.CASE_INSENSITIVE_ORDER.compare(text, other.text);
    }
}
