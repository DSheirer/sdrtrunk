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

import io.github.dsheirer.map.GeoPosition;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

/**
 * Push-pin at the user's location, with the tip of the pin at the location.  Use on the JavaFX thread.
 */
public class UserLocationLayer implements MapLayer
{
    private static final Color NEEDLE = Color.rgb(90, 90, 90);
    private static final Color HEAD = Color.rgb(220, 40, 40);
    private static final Color HEAD_OUTLINE = Color.rgb(110, 0, 0);
    private static final Color HIGHLIGHT = Color.rgb(255, 255, 255, 170 / 255.0);
    private static final Color SHADOW = Color.rgb(0, 0, 0, 70 / 255.0);
    private static final double NEEDLE_LENGTH = 14.0;
    private static final double HEAD_RADIUS = 7.0;

    private GeoPosition mPosition;

    /**
     * Sets the location to mark
     * @param position of the user, or null to remove the pin
     */
    public void setPosition(GeoPosition position)
    {
        mPosition = position;
    }

    /**
     * Location that is marked, or null
     */
    public GeoPosition getPosition()
    {
        return mPosition;
    }

    @Override
    public void draw(GraphicsContext gc, TileMapView map)
    {
        if(mPosition == null)
        {
            return;
        }

        MapViewport viewport = map.getViewport();
        double x = viewport.getScreenX(mPosition.getLongitude());
        double y = viewport.getScreenY(mPosition.getLatitude());
        double headY = y - NEEDLE_LENGTH - HEAD_RADIUS;

        //Shadow where the needle enters the map, so the tip reads as the location
        gc.setFill(SHADOW);
        gc.fillOval(x - 4, y - 2, 8, 4);

        gc.setStroke(NEEDLE);
        gc.setLineWidth(2.0);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.strokeLine(x, y, x, headY);

        gc.setFill(HEAD);
        gc.fillOval(x - HEAD_RADIUS, headY - HEAD_RADIUS, HEAD_RADIUS * 2, HEAD_RADIUS * 2);
        gc.setStroke(HEAD_OUTLINE);
        gc.setLineWidth(1.0);
        gc.strokeOval(x - HEAD_RADIUS, headY - HEAD_RADIUS, HEAD_RADIUS * 2, HEAD_RADIUS * 2);

        gc.setFill(HIGHLIGHT);
        gc.fillOval(x - HEAD_RADIUS / 2 - 1, headY - HEAD_RADIUS / 2 - 1, 4, 4);
    }
}
