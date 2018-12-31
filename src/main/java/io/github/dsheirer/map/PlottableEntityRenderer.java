/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.map;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.icon.IconManager;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoPosition;

import javax.swing.ImageIcon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.List;

public class PlottableEntityRenderer
{
    private AliasModel mAliasModel;
    private IconManager mIconManager;

    public PlottableEntityRenderer(AliasModel aliasModel, IconManager iconManager)
    {
        mAliasModel = aliasModel;
        mIconManager = iconManager;
    }

    public void paintPlottableEntity(Graphics2D g, JXMapViewer viewer, PlottableEntityHistory entity, boolean antiAliasing)
    {
        List<GeoPosition> locationHistory = entity.getLocationHistory();

        if(!locationHistory.isEmpty() && locationHistory.get(locationHistory.size() - 1).isValid())
        {
            Graphics2D graphics = (Graphics2D)g.create();

            if(antiAliasing)
            {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            }

            List<Alias> aliases = getAliases(entity);

            Alias alias = aliases.isEmpty() ? null : aliases.get(0);

            /**
             * Use the entity's preferred color for lines and labels
             */
            Color color = (alias != null ? alias.getDisplayColor() : Color.YELLOW);
            graphics.setColor(color);

            /**
             * Paint the route first, so the icon and label overlay it
             */
            paintRoute(graphics, viewer, entity, color);


            /**
             * Convert the lat/long geoposition to an x/y point on the viewer
             */

            Point2D point = viewer.getTileFactory().geoToPixel(locationHistory.get(locationHistory.size() - 1), viewer.getZoom());

            /**
             * Paint the icon at the current location
             */
            ImageIcon icon = getIcon(alias);
            paintIcon(graphics, point, icon);

            /**
             * Paint the label offset to the right of the icon
             */
            String label = (alias != null ? alias.getName() : entity.getIdentifier().toString());
            paintLabel(graphics, point, label, (int)(icon.getIconWidth() / 2), 0);

            /**
             * Cleanup
             */
            graphics.dispose();
        }
    }

    /**
     * Optional alias that matches the identifier for the entity history
     */
    private List<Alias> getAliases(PlottableEntityHistory entityHistory)
    {
        AliasList aliasList = mAliasModel.getAliasList(entityHistory.getIdentifierCollection());

        if(aliasList != null)
        {
            return aliasList.getAliases(entityHistory.getIdentifier());
        }

        return Collections.EMPTY_LIST;
    }

    private ImageIcon getIcon(Alias alias)
    {
        String iconName = (alias != null ? alias.getIconName() : null);
        return mIconManager.getIcon(iconName, IconManager.DEFAULT_ICON_SIZE);
    }

    private void paintIcon(Graphics2D graphics, Point2D point, ImageIcon icon)
    {
        graphics.drawImage(icon.getImage(), (int)point.getX() - (icon.getIconWidth() / 2),
            (int)point.getY() - (icon.getIconHeight() / 2), null);
    }

    private void paintLabel(Graphics2D graphics, Point2D point, String label, int xOffset, int yOffset)
    {
        graphics.drawString(label, (int)point.getX() + xOffset, (int)point.getY() + yOffset);
    }

    /**
     * Paints a two-tone route from the entity's list of plottables (locations).
     * using black as a wider background route, and the entity's preferred color
     * as a narrower foreground route.
     */
    private void paintRoute(Graphics2D graphics, JXMapViewer viewer, PlottableEntityHistory entity, Color color)
    {
        List<GeoPosition> locations = entity.getLocationHistory();

        if(!locations.isEmpty())
        {
            // Draw the route with a black background line
            graphics.setColor(Color.BLACK);
            graphics.setStroke(new BasicStroke(3));

            drawRoute(locations, graphics, viewer);

            // Draw the route again, in the entity's preferred color
            graphics.setColor(color);
            graphics.setStroke(new BasicStroke(1));

            drawRoute(locations, graphics, viewer);
        }
    }

    /**
     * Draws a route from a list of plottables
     */
    private void drawRoute(List<GeoPosition> locations, Graphics2D g, JXMapViewer viewer)
    {
        Point2D lastPoint = null;

        for(GeoPosition location : locations)
        {
            // convert geo-coordinate to world bitmap pixel
            Point2D currentPoint = viewer.getTileFactory().geoToPixel(location, viewer.getZoom());

            if(lastPoint != null)
            {
                g.drawLine((int)lastPoint.getX(), (int)lastPoint.getY(), (int)currentPoint.getX(), (int)currentPoint.getY());
            }

            lastPoint = currentPoint;
        }
    }
}


