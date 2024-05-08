
/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.icon.IconModel;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.painter.AbstractPainter;

/**
 * Paints plottable entities to the map.
 */
public class PlottableEntityPainter extends AbstractPainter<JXMapViewer>
{
    private PlottableEntityRenderer mRenderer;
    private Set<PlottableEntityHistory> mEntities = new HashSet<>();

    /**
     * Constructs an instance
     * @param aliasModel to lookup alias for entities
     * @param iconModel to lookup icon from alias.
     */
    public PlottableEntityPainter(AliasModel aliasModel, IconModel iconModel)
    {
        mRenderer = new PlottableEntityRenderer(aliasModel, iconModel);
        setAntialiasing(true);
        setCacheable(false);
    }

    /**
     * Sets the length of the plotted history trails.
     * @param length of history trails
     */
    public void setTrackHistoryLength(int length)
    {
        mRenderer.setTrackHistoryLength(length);
    }

    /**
     * Current size of the history trail length.
     */
    public int getTrackHistoryLength()
    {
        return mRenderer.getTrackHistoryLength();
    }

    /**
     * Adds an entity to the map
     * @param entity to add
     */
    public boolean addEntity(PlottableEntityHistory entity)
    {
        if(entity != null && !mEntities.contains(entity))
        {
            mEntities.add(entity);
            return true;
        }

        return false;
    }

    /**
     * Adds all entities to this painter
     * @param entities to add.
     */
    public boolean addAll(List<PlottableEntityHistory> entities)
    {
        boolean added = false;
        for(PlottableEntityHistory entity : entities)
        {
            added |= addEntity(entity);
        }

        return added;
    }

    /**
     * Removes an entity from the map
     * @param entity to remove
     */
    public void removeEntity(PlottableEntityHistory entity)
    {
        mEntities.remove(entity);
    }

    /**
     * Clears all entities from the map.
     */
    public void clearAllEntities()
    {
        mEntities.clear();
    }

    /**
     * Clears teh specified entities from the map
     * @param toDelete to delete
     */
    public void clearEntities(List<PlottableEntityHistory> toDelete)
    {
        mEntities.removeAll(toDelete);
    }

    @Override
    protected void doPaint(Graphics2D g, JXMapViewer map, int width, int height)
    {
        Rectangle viewportBounds = map.getViewportBounds();

        g.translate(-viewportBounds.getX(), -viewportBounds.getY());

        for(PlottableEntityHistory entity : mEntities)
        {
            mRenderer.paintPlottableEntity(g, map, entity, true);
        }

        g.translate(viewportBounds.getX(), viewportBounds.getY());
    }
}
