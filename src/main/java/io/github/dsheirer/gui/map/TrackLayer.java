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

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.icon.Icon;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.map.PlottableEntityHistory;
import io.github.dsheirer.map.PlottableEntityModel;
import io.github.dsheirer.map.TimestampedGeoPosition;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Plotted tracks: for each entity, a two-tone trail through its most recent positions, its alias icon at the latest
 * position and its alias name (or identifier) beside the icon, in the alias color.  Use on the JavaFX thread.
 */
public class TrackLayer implements MapLayer
{
    private static final Color DEFAULT_COLOR = Color.BLUE;
    private static final Color TRAIL_BACKGROUND = Color.BLACK;
    private static final Color LABEL_HALO = Color.rgb(255, 255, 255, 0.85);
    private static final Font LABEL_FONT = Font.font(12);
    //Map edge allowance for icons and labels that are partly in view
    private static final double MARGIN = 200;

    private final PlottableEntityModel mEntityModel;
    private final IconModel mIconModel;
    private final Set<PlottableEntityHistory> mEntities = new LinkedHashSet<>();
    private int mTrackHistoryLength = 3;

    /**
     * Constructs an instance
     * @param entityModel for the entities' aliases
     * @param iconModel for the alias icons
     */
    public TrackLayer(PlottableEntityModel entityModel, IconModel iconModel)
    {
        mEntityModel = entityModel;
        mIconModel = iconModel;
    }

    /**
     * Number of positions in each trail, including the latest position
     */
    public int getTrackHistoryLength()
    {
        return mTrackHistoryLength;
    }

    public void setTrackHistoryLength(int length)
    {
        mTrackHistoryLength = length;
    }

    /**
     * Shows the entity
     * @return true if it wasn't shown
     */
    public boolean add(PlottableEntityHistory entity)
    {
        return entity != null && mEntities.add(entity);
    }

    /**
     * Shows the entities
     * @return true if any wasn't shown
     */
    public boolean addAll(Collection<PlottableEntityHistory> entities)
    {
        boolean added = false;

        for(PlottableEntityHistory entity: entities)
        {
            added |= add(entity);
        }

        return added;
    }

    /**
     * Stops showing the entities
     */
    public void removeAll(Collection<PlottableEntityHistory> entities)
    {
        mEntities.removeAll(entities);
    }

    /**
     * Stops showing all entities.  They're shown again when they report a new position.
     */
    public void clear()
    {
        mEntities.clear();
    }

    @Override
    public void draw(GraphicsContext gc, TileMapView map)
    {
        MapViewport viewport = map.getViewport();
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.setFont(LABEL_FONT);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.CENTER);

        for(PlottableEntityHistory entity: mEntities)
        {
            List<TimestampedGeoPosition> history = entity.getTrackHistoryModel().getTrackHistory();

            if(history.isEmpty() || !history.getFirst().isValid())
            {
                continue;
            }

            List<Alias> aliases = mEntityModel.getAliases(entity);
            Alias alias = aliases.isEmpty() ? null : aliases.getFirst();
            Color color = alias != null ? toColor(alias.getColor()) : DEFAULT_COLOR;

            drawTrail(gc, viewport, history, color);

            TimestampedGeoPosition latest = history.getFirst();
            double x = viewport.getScreenX(latest.getLongitude());
            double y = viewport.getScreenY(latest.getLatitude());

            if(x < -MARGIN || x > viewport.getWidth() + MARGIN || y < -MARGIN || y > viewport.getHeight() + MARGIN)
            {
                continue;
            }

            double iconWidth = drawIcon(gc, alias, x, y);
            String label = alias != null ? alias.getName() : String.valueOf(entity.getIdentifier());
            double labelX = x + iconWidth / 2.0 + 3;
            gc.setStroke(LABEL_HALO);
            gc.setLineWidth(3);
            gc.strokeText(label, labelX, y);
            gc.setFill(color);
            gc.fillText(label, labelX, y);
        }
    }

    /**
     * Trail through the most recent positions: a wide black line under a narrow line in the entity's color
     */
    private void drawTrail(GraphicsContext gc, MapViewport viewport, List<TimestampedGeoPosition> history,
                           Color color)
    {
        int length = Math.min(history.size(), mTrackHistoryLength);

        if(length < 2)
        {
            return;
        }

        double[] xs = new double[length];
        double[] ys = new double[length];
        double reference = viewport.getScreenX(history.getFirst().getLongitude());
        double worldSize = viewport.getWorldSize();

        for(int index = 0; index < length; index++)
        {
            TimestampedGeoPosition position = history.get(index);
            double x = viewport.getScreenX(position.getLongitude());
            //Keep the trail on the same copy of the world as the latest position
            xs[index] = x - Math.round((x - reference) / worldSize) * worldSize;
            ys[index] = viewport.getScreenY(position.getLatitude());
        }

        gc.setStroke(TRAIL_BACKGROUND);
        gc.setLineWidth(3);
        gc.strokePolyline(xs, ys, length);
        gc.setStroke(color);
        gc.setLineWidth(1);
        gc.strokePolyline(xs, ys, length);
    }

    /**
     * Draws the alias icon, or the default icon, centered on the point
     * @return drawn icon width
     */
    private double drawIcon(GraphicsContext gc, Alias alias, double x, double y)
    {
        Icon icon = mIconModel.getIcon(alias != null ? alias.getIconName() : null);
        Image image = icon != null ? icon.getFxImage() : null;

        if(image == null || image.getHeight() <= 0)
        {
            return 0;
        }

        double height = IconModel.DEFAULT_ICON_SIZE;
        double width = image.getWidth() * height / image.getHeight();
        gc.drawImage(image, x - width / 2.0, y - height / 2.0, width, height);
        return width;
    }

    private static Color toColor(int rgb)
    {
        return Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }
}
