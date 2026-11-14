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

package io.github.dsheirer.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.painter.AbstractPainter;

/**
 * Paints a push-pin at the user's location, with the tip of the pin at the location.
 */
public class UserLocationPainter extends AbstractPainter<JXMapViewer>
{
    private static final Color NEEDLE = new Color(90, 90, 90);
    private static final Color HEAD = new Color(220, 40, 40);
    private static final Color HEAD_OUTLINE = new Color(110, 0, 0);
    private static final Color HIGHLIGHT = new Color(255, 255, 255, 170);
    private static final Color SHADOW = new Color(0, 0, 0, 70);
    private static final double NEEDLE_LENGTH = 14.0;
    private static final double HEAD_RADIUS = 7.0;

    private volatile GeoPosition mPosition;

    /**
     * Constructs an instance
     */
    public UserLocationPainter()
    {
        setAntialiasing(true);
        setCacheable(false);
    }

    /**
     * Sets the location to mark
     * @param position of the user, or null to remove the pin
     */
    public void setPosition(GeoPosition position)
    {
        mPosition = position;
    }

    /**
     * Location that is marked
     * @return position, or null
     */
    public GeoPosition getPosition()
    {
        return mPosition;
    }

    @Override
    protected void doPaint(Graphics2D g, JXMapViewer map, int width, int height)
    {
        GeoPosition position = mPosition;

        if(position == null)
        {
            return;
        }

        Rectangle viewportBounds = map.getViewportBounds();
        Point2D point = map.getTileFactory().geoToPixel(position, map.getZoom());
        double x = point.getX() - viewportBounds.getX();
        double y = point.getY() - viewportBounds.getY();
        double headY = y - NEEDLE_LENGTH - HEAD_RADIUS;

        //Shadow where the needle enters the map, so the tip reads as the location
        g.setColor(SHADOW);
        g.fill(new Ellipse2D.Double(x - 4, y - 2, 8, 4));

        g.setColor(NEEDLE);
        g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x, y, x, headY));

        Ellipse2D head = new Ellipse2D.Double(x - HEAD_RADIUS, headY - HEAD_RADIUS, HEAD_RADIUS * 2, HEAD_RADIUS * 2);
        g.setColor(HEAD);
        g.fill(head);
        g.setColor(HEAD_OUTLINE);
        g.setStroke(new BasicStroke(1.0f));
        g.draw(head);

        g.setColor(HIGHLIGHT);
        g.fill(new Ellipse2D.Double(x - HEAD_RADIUS / 2 - 1, headY - HEAD_RADIUS / 2 - 1, 4, 4));
    }
}
