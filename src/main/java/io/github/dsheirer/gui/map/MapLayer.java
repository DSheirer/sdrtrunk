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

import javafx.scene.canvas.GraphicsContext;

/**
 * Overlay drawn over the map tiles, in the order the layers were added to the {@link TileMapView}.
 */
public interface MapLayer
{
    /**
     * Draws the layer.  Called on the JavaFX thread.
     * @param gc of the map canvas, in screen pixels
     * @param map for converting positions to screen pixels
     */
    void draw(GraphicsContext gc, TileMapView map);
}
