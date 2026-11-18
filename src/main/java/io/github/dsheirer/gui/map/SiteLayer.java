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

import io.github.dsheirer.service.radioreference.SiteRecord;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Radio reference trunked system sites: a radio tower at each site, with the base of the tower at the site location.
 * Use on the JavaFX thread.
 */
public class SiteLayer implements MapLayer
{
    private static final Color TOWER = Color.rgb(240, 128, 32);
    private static final Color OUTLINE = Color.rgb(40, 40, 40, 200 / 255.0);
    private static final double TOWER_WIDTH = 1.6;
    private static final double OUTLINE_WIDTH = 3.6;
    private static final double HEIGHT = 16;
    private static final double HALF_BASE = 5;
    //Hit area around the tower, in pixels from the site location
    private static final double HIT_LEFT = -10;
    private static final double HIT_TOP = -24;
    private static final double HIT_WIDTH = 20;
    private static final double HIT_HEIGHT = 28;

    private final Supplier<List<SiteRecord>> mSites;
    private boolean mShowSites = true;

    /**
     * Constructs an instance
     * @param sites to draw, read each time the map is drawn
     */
    public SiteLayer(Supplier<List<SiteRecord>> sites)
    {
        mSites = sites;
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
     * @param viewport of the map
     * @param x of the point in the view
     * @param y of the point in the view
     * @return sites, or an empty list when the sites are hidden or none is under the point
     */
    public List<SiteRecord> getSitesAt(MapViewport viewport, double x, double y)
    {
        if(!mShowSites)
        {
            return Collections.emptyList();
        }

        record Hit(SiteRecord site, double distance) {}

        List<Hit> hits = new ArrayList<>();

        for(SiteRecord site: mSites.get())
        {
            double dx = x - viewport.getScreenX(site.longitude());
            double dy = y - viewport.getScreenY(site.latitude());

            if(dx >= HIT_LEFT && dx < HIT_LEFT + HIT_WIDTH && dy >= HIT_TOP && dy < HIT_TOP + HIT_HEIGHT)
            {
                //Nearest to the middle of the tower first
                hits.add(new Hit(site, Math.hypot(dx, dy + (HEIGHT / 2.0))));
            }
        }

        //Stable, so sites at the same spot keep their order
        hits.sort(Comparator.comparingDouble(Hit::distance));
        return hits.stream().map(Hit::site).toList();
    }

    @Override
    public void draw(GraphicsContext gc, TileMapView map)
    {
        if(!mShowSites)
        {
            return;
        }

        MapViewport viewport = map.getViewport();
        double width = viewport.getWidth();
        double height = viewport.getHeight();
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        for(SiteRecord site: mSites.get())
        {
            double x = viewport.getScreenX(site.longitude());
            double y = viewport.getScreenY(site.latitude());

            if(x >= -20 && x <= width + 20 && y >= -4 && y <= height + 30)
            {
                drawTower(gc, x, y, OUTLINE, OUTLINE_WIDTH);
                drawTower(gc, x, y, TOWER, TOWER_WIDTH);
                gc.setFill(TOWER);
                gc.fillOval(x - 2, y - HEIGHT - 2, 4, 4);
            }
        }
    }

    /**
     * Strokes a tower with its base at the point: an A-frame mast with a cross brace and radio waves at the top
     */
    private static void drawTower(GraphicsContext gc, double x, double y, Color color, double lineWidth)
    {
        double top = y - HEIGHT;
        gc.setStroke(color);
        gc.setLineWidth(lineWidth);
        gc.strokePolyline(new double[]{x - HALF_BASE, x, x + HALF_BASE}, new double[]{y, top, y}, 3);
        gc.strokeLine(x - (HALF_BASE * 0.55), y - (HEIGHT * 0.45), x + (HALF_BASE * 0.55), y - (HEIGHT * 0.45));
        gc.strokeArc(x - 5, top - 4, 10, 8, 135, 90, ArcType.OPEN);
        gc.strokeArc(x - 5, top - 4, 10, 8, -45, 90, ArcType.OPEN);
        gc.strokeArc(x - 9, top - 7, 18, 14, 135, 90, ArcType.OPEN);
        gc.strokeArc(x - 9, top - 7, 18, 14, -45, 90, ArcType.OPEN);
    }
}
