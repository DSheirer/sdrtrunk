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


package io.github.dsheirer.gui.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entry in the tile server chooser: a server with tiles that follow the theme, or always light or always dark tiles.
 * Dark tiles come from the server's dark style, or are recolored light tiles when it has none.
 * @param server tile server
 * @param style of the tiles
 */
public record TileChoice(TileServer server, Style style)
{
    /**
     * Tile style.  The name is stored in the map preference.  Don't change it.
     */
    public enum Style
    {
        THEME(" - Auto"),
        LIGHT(" - Light"),
        DARK(" - Dark");

        private final String mSuffix;

        Style(String suffix)
        {
            mSuffix = suffix;
        }
    }

    /**
     * Choices offered to the user, in menu order: each server following the theme, then light, then dark
     */
    public static final List<TileChoice> CHOICES;

    static
    {
        List<TileChoice> choices = new ArrayList<>();

        for(TileServer server: TileServer.SERVERS)
        {
            for(Style style: Style.values())
            {
                choices.add(new TileChoice(server, style));
            }
        }

        CHOICES = Collections.unmodifiableList(choices);
    }

    /**
     * Choice with the server and style
     * @param serverId from the map preference, or null
     * @param style name from the map preference, or null
     * @return choice, following the theme when the style is unknown, see {@link TileServer#get(String)}
     */
    public static TileChoice get(String serverId, String style)
    {
        TileServer server = TileServer.get(serverId);

        for(Style candidate: Style.values())
        {
            if(candidate.name().equals(style))
            {
                return new TileChoice(server, candidate);
            }
        }

        return new TileChoice(server, Style.THEME);
    }

    /**
     * Indicates if the tiles are dark
     * @param darkTheme true when the application uses a dark theme
     */
    public boolean isDark(boolean darkTheme)
    {
        return switch(style)
        {
            case THEME -> darkTheme;
            case LIGHT -> false;
            case DARK -> true;
        };
    }

    /**
     * Tiles to load
     * @param darkTheme true when the application uses a dark theme
     * @param key from the user, for a server that needs one, or null
     */
    public TileSource getSource(boolean darkTheme, String key)
    {
        return server.getSource(isDark(darkTheme), key);
    }

    @Override
    public String toString()
    {
        return server.name() + style.mSuffix;
    }
}
