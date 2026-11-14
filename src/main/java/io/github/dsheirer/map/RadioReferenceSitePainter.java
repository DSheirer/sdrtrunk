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

import io.github.dsheirer.service.radioreference.SiteRecord;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.painter.AbstractPainter;

/**
 * Paints a radio tower at each radio reference trunked system site, with the base of the tower at the site location.
 */
public class RadioReferenceSitePainter extends AbstractPainter<JXMapViewer>
{
    private static final Color TOWER = new Color(240, 128, 32);
    private static final Color OUTLINE = new Color(40, 40, 40, 200);
    private static final Stroke TOWER_STROKE = new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Stroke OUTLINE_STROKE = new BasicStroke(3.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final double HEIGHT = 16;
    private static final double HALF_BASE = 5;
    //Hit area around the tower, in pixels from the site location
    private static final Rectangle HIT_AREA = new Rectangle(-10, -24, 20, 28);

    private final Supplier<List<SiteRecord>> mSites;
    private volatile boolean mShowSites = true;

    /**
     * Constructs an instance
     * @param sites to paint, read each time the map is painted
     */
    public RadioReferenceSitePainter(Supplier<List<SiteRecord>> sites)
    {
        mSites = sites;
        setAntialiasing(true);
        setCacheable(false);
    }

    /**
     * Shows or hides the sites
     */
    public void setShowSites(boolean show)
    {
        mShowSites = show;
    }

    public boolean isShowSites()
    {
        return mShowSites;
    }

    /**
     * Sites whose tower is under the point, nearest first
     * @param map viewer
     * @param point in the viewer
     * @return sites, or an empty list when the sites are hidden or none is under the point
     */
    public List<SiteRecord> getSitesAt(JXMapViewer map, Point point)
    {
        if(!mShowSites)
        {
            return Collections.emptyList();
        }

        Rectangle viewport = map.getViewportBounds();
        List<SiteRecord> sites = new ArrayList<>();
        List<Double> distances = new ArrayList<>();

        for(SiteRecord site: mSites.get())
        {
            Point2D pixel = map.getTileFactory().geoToPixel(new GeoPosition(site.latitude(), site.longitude()),
                map.getZoom());
            double dx = point.x - (pixel.getX() - viewport.getX());
            double dy = point.y - (pixel.getY() - viewport.getY());

            if(HIT_AREA.contains(dx, dy))
            {
                //Nearest to the middle of the tower first
                double distance = Math.hypot(dx, dy + (HEIGHT / 2.0));
                int index = 0;

                while(index < distances.size() && distances.get(index) <= distance)
                {
                    index++;
                }

                sites.add(index, site);
                distances.add(index, distance);
            }
        }

        return sites;
    }

    @Override
    protected void doPaint(Graphics2D g, JXMapViewer map, int width, int height)
    {
        if(!mShowSites)
        {
            return;
        }

        Rectangle viewport = map.getViewportBounds();

        for(SiteRecord site: mSites.get())
        {
            Point2D pixel = map.getTileFactory().geoToPixel(new GeoPosition(site.latitude(), site.longitude()),
                map.getZoom());
            double x = pixel.getX() - viewport.getX();
            double y = pixel.getY() - viewport.getY();

            if(x >= -20 && x <= width + 20 && y >= -4 && y <= height + 30)
            {
                paintTower(g, x, y);
            }
        }
    }

    /**
     * Paints a tower with its base at the point: an A-frame mast with a cross brace and radio waves at the top
     */
    private static void paintTower(Graphics2D g, double x, double y)
    {
        double top = y - HEIGHT;
        Path2D mast = new Path2D.Double();
        mast.moveTo(x - HALF_BASE, y);
        mast.lineTo(x, top);
        mast.lineTo(x + HALF_BASE, y);
        mast.moveTo(x - (HALF_BASE * 0.55), y - (HEIGHT * 0.45));
        mast.lineTo(x + (HALF_BASE * 0.55), y - (HEIGHT * 0.45));

        Arc2D leftInner = new Arc2D.Double(x - 5, top - 4, 10, 8, 135, 90, Arc2D.OPEN);
        Arc2D rightInner = new Arc2D.Double(x - 5, top - 4, 10, 8, -45, 90, Arc2D.OPEN);
        Arc2D leftOuter = new Arc2D.Double(x - 9, top - 7, 18, 14, 135, 90, Arc2D.OPEN);
        Arc2D rightOuter = new Arc2D.Double(x - 9, top - 7, 18, 14, -45, 90, Arc2D.OPEN);

        g.setStroke(OUTLINE_STROKE);
        g.setColor(OUTLINE);
        g.draw(mast);
        g.draw(leftInner);
        g.draw(rightInner);
        g.draw(leftOuter);
        g.draw(rightOuter);

        g.setStroke(TOWER_STROKE);
        g.setColor(TOWER);
        g.draw(mast);
        g.draw(leftInner);
        g.draw(rightInner);
        g.draw(leftOuter);
        g.draw(rightOuter);
        g.fill(new Ellipse2D.Double(x - 2, top - 2, 4, 4));
    }
}
